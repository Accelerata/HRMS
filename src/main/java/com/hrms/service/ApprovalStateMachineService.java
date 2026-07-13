package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalActionEnum;
import com.hrms.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批状态机引擎 — 核心 Service
 *
 * 职责：
 * 1. 根据审批模板生成审批记录
 * 2. 解析审批人（部门主管 / HR专员 / 原部门主管 / 新部门主管）
 * 3. 处理审批动作（通过/拒绝/退回）
 * 4. 判断审批是否全部完成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalStateMachineService {

    private final ApprovalTemplateMapper templateMapper;
    private final ApprovalRecordMapper recordMapper;
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 启动审批流程——根据业务类型自动生成审批待办记录
     *
     * @param businessType 业务类型 (1-入职 2-转正 3-调岗 4-离职)
     * @param businessId   业务单据ID
     * @param context      审批上下文（用于解析审批人）
     */
    @Transactional
    public void startApproval(int businessType, long businessId, ApprovalContext context) {
        List<ApprovalTemplate> templates = templateMapper.selectByBusinessType(businessType);
        if (templates.isEmpty()) {
            log.warn("未找到审批模板: businessType={}", businessType);
            return;
        }

        List<ApprovalRecord> records = new ArrayList<>();
        for (ApprovalTemplate tpl : templates) {
            Long approverId = resolveApprover(tpl.getApproverTarget(), context);
            String approverName = resolveApproverName(tpl.getApproverTarget(), context, approverId);

            if (approverId == null) {
                log.warn("无法解析审批人: target={}, businessType={}, businessId={}",
                        tpl.getApproverTarget(), businessType, businessId);
                // 跳过无法解析的步骤——避免阻塞审批流程
                continue;
            }

            ApprovalRecord record = new ApprovalRecord();
            record.setBusinessType(businessType);
            record.setBusinessId(businessId);
            record.setStepOrder(tpl.getStepOrder());
            record.setApproverId(approverId);
            record.setApproverName(approverName);
            record.setIsPending(1); // 待审批
            records.add(record);
        }

        if (!records.isEmpty()) {
            recordMapper.insertBatch(records);
            log.info("审批流程已启动: businessType={}, businessId={}, 生成{}条待办",
                    businessType, businessId, records.size());
        }
    }

    /**
     * 处理单个审批动作
     *
     * @param recordId 审批记录ID
     * @param action   动作 (1-通过 2-拒绝 3-退回)
     * @param comment  审批意见
     * @param currentUserId 当前操作人ID
     * @return 审批动作后的影响：是否是拒绝/退回操作
     */
    @Transactional
    public ApprovalResult processApproval(long recordId, int action, String comment, Long currentUserId) {
        // 1. 校验审批记录
        ApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw BaseException.notFound("审批记录不存在");
        }
        if (record.getIsPending() != 1) {
            throw BaseException.badRequest("该审批步骤已处理");
        }
        // 校验审批人身份
        if (!record.getApproverId().equals(currentUserId)) {
            throw BaseException.badRequest("您不是该审批步骤的审批人");
        }

        // 2. 更新审批记录
        recordMapper.updateAction(recordId, action, comment, 0); // isPending=0 已处理

        log.info("审批完成: recordId={}, action={}, approverId={}", recordId, action, currentUserId);

        // 3. 判断后续流程
        ApprovalResult result = new ApprovalResult();
        result.setBusinessType(record.getBusinessType());
        result.setBusinessId(record.getBusinessId());
        result.setAction(action);
        result.setStepOrder(record.getStepOrder());

        if (action == ApprovalActionEnum.REJECT.getCode()
                || action == ApprovalActionEnum.RETURN.getCode()) {
            // 拒绝/退回 → 整个审批流终止
            result.setTerminated(true);
            result.setApproved(false);
            return result;
        }

        // 通过 → 检查是否所有步骤都已通过
        List<ApprovalRecord> pendingRecords = recordMapper.selectPendingByBusiness(
                record.getBusinessType(), record.getBusinessId());

        if (pendingRecords.isEmpty()) {
            // 所有步骤都已完成（无待审记录）
            result.setTerminated(false);
            result.setApproved(true);
        } else {
            // 还有未完成的步骤
            result.setTerminated(false);
            result.setApproved(false); // 还要继续等待
        }

        return result;
    }

    /**
     * 查询某业务的所有审批记录
     */
    public List<ApprovalRecord> getApprovalRecords(int businessType, long businessId) {
        return recordMapper.selectByBusiness(businessType, businessId);
    }

    // ═══════════════ 审批人解析 ═══════════════

    /**
     * 根据审批人指向解析实际审批人ID
     */
    private Long resolveApprover(String approverTarget, ApprovalContext context) {
        switch (approverTarget) {
            case "dept_manager":
                return resolveDeptManager(context.getDeptId());
            case "hr_specialist":
                return resolveHrSpecialist();
            case "old_dept_manager":
                return resolveDeptManager(context.getOldDeptId());
            case "new_dept_manager":
                return resolveDeptManager(context.getNewDeptId());
            default:
                log.warn("未知的审批人指向: {}", approverTarget);
                return null;
        }
    }

    private String resolveApproverName(String approverTarget, ApprovalContext context, Long approverId) {
        // 通用做法：通过用户表查，这里简单用目标+ID拼接
        if (approverId == null) return null;
        SysUser user = sysUserMapper.findById(approverId);
        return user != null ? user.getUsername() : ("用户" + approverId);
    }

    /** 解析部门主管 → sys_user.id */
    private Long resolveDeptManager(Long deptId) {
        if (deptId == null) return null;
        Department dept = departmentMapper.selectById(deptId);
        if (dept == null || dept.getManagerId() == null) {
            log.warn("部门无负责人: deptId={}", deptId);
            return null;
        }
        // managerId 是 employee.id，需要转为 sys_user.id
        Employee manager = employeeMapper.selectById(dept.getManagerId());
        if (manager == null || manager.getUserId() == null) {
            log.warn("部门负责人无关联系统账号: deptId={}, managerId={}", deptId, dept.getManagerId());
            return null;
        }
        return manager.getUserId();
    }

    /** 解析HR专员 → 第一个拥有 ROLE_HR 角色的用户 */
    private Long resolveHrSpecialist() {
        SysUser hr = sysUserMapper.findFirstByRoleCode("ROLE_HR");
        if (hr == null) {
            log.warn("系统中未找到HR专员");
            return null;
        }
        return hr.getId();
    }

    // ═══════════════ 内部类 ═══════════════

    /**
     * 审批上下文——传递审批人解析所需信息
     */
    @lombok.Data
    public static class ApprovalContext {
        /** 目标部门ID（入职=目标部门, 转正/离职=员工所在部门） */
        private Long deptId;
        /** 原部门ID（仅调岗使用） */
        private Long oldDeptId;
        /** 新部门ID（仅调岗使用） */
        private Long newDeptId;

        public static ApprovalContext ofDept(Long deptId) {
            ApprovalContext ctx = new ApprovalContext();
            ctx.deptId = deptId;
            return ctx;
        }

        public static ApprovalContext ofTransfer(Long oldDeptId, Long newDeptId) {
            ApprovalContext ctx = new ApprovalContext();
            ctx.oldDeptId = oldDeptId;
            ctx.newDeptId = newDeptId;
            return ctx;
        }
    }

    /**
     * 审批结果
     */
    @lombok.Data
    public static class ApprovalResult {
        private int businessType;
        private long businessId;
        private int action;
        private int stepOrder;
        /** 是否终止（拒绝或退回时=true） */
        private boolean terminated;
        /** 是否全部通过 */
        private boolean approved;
    }
}
