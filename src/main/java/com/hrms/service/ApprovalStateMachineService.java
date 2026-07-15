package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalActionEnum;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审批状态机引擎 — 核心 Service
 *
 * 职责：
 * 1. 根据审批模板生成审批记录（含逾期时间）
 * 2. 解析审批人（部门主管 / HR专员 / 原部门主管 / 新部门主管）
 * 3. 处理审批动作（通过/拒绝/退回），含顺序门控
 * 4. 判断审批是否全部完成
 * 5. 审批结果通知
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
    private final NotificationService notificationService;

    private static final int DUE_HOURS = 48; // 每级审批 48h 截止

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
            // 条件步骤过滤：needHr 为 false 时跳过 condition_expr='needHr' 的 HR 审批步骤
            if (!context.isNeedHr() && "needHr".equals(tpl.getConditionExpr())) {
                log.info("跳过条件审批步骤: templateId={}, stepName={}, conditionExpr={}, needHr={}",
                        tpl.getId(), tpl.getStepName(), tpl.getConditionExpr(), context.isNeedHr());
                continue;
            }

            // 条件步骤过滤：hasSalaryAdjust 为 false 时跳过 condition_expr='hasSalaryAdjust' 的薪资审批步骤
            if (!context.isHasSalaryAdjust() && "hasSalaryAdjust".equals(tpl.getConditionExpr())) {
                log.info("跳过条件审批步骤: templateId={}, stepName={}, conditionExpr={}, hasSalaryAdjust={}",
                        tpl.getId(), tpl.getStepName(), tpl.getConditionExpr(), context.isHasSalaryAdjust());
                continue;
            }

            Long approverId = resolveApprover(tpl.getApproverTarget(), context);
            String approverName = resolveApproverName(tpl.getApproverTarget(), context, approverId);

            if (approverId == null) {
                log.warn("无法解析审批人: target={}, businessType={}, businessId={}",
                        tpl.getApproverTarget(), businessType, businessId);
                continue;
            }

            ApprovalRecord record = new ApprovalRecord();
            record.setBusinessType(businessType);
            record.setBusinessId(businessId);
            record.setStepOrder(tpl.getStepOrder());
            record.setApproverId(approverId);
            record.setApproverName(approverName);
            record.setIsPending(1);
            // 计算每级 48h 截止时间
            record.setDueTime(LocalDateTime.now().plusHours(DUE_HOURS));
            records.add(record);
        }

        if (!records.isEmpty()) {
            recordMapper.insertBatch(records);
            log.info("审批流程已启动: businessType={}, businessId={}, 生成{}条待办",
                    businessType, businessId, records.size());

            // 发送待办通知
            for (ApprovalRecord r : records) {
                String title = "审批待办: " + BusinessTypeEnum.fromCode(businessType).getLabel();
                notificationService.sendApprovalNotify(r.getApproverId(), title,
                        "您有一项新的审批任务待处理，请登录审批工作台查看。",
                        businessType, businessId);
            }
        }
    }

    /**
     * 处理单个审批动作
     *
     * @param recordId      审批记录ID
     * @param action        动作 (1-通过 2-拒绝 3-退回)
     * @param comment       审批意见
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
        if (!record.getApproverId().equals(currentUserId)) {
            throw BaseException.badRequest("您不是该审批步骤的审批人");
        }

        // 2. 顺序门控：是否存在更低 step_order 的未处理记录
        int lowerCount = recordMapper.countLowerPending(
                record.getBusinessType(), record.getBusinessId(), record.getStepOrder());
        if (lowerCount > 0) {
            throw BaseException.badRequest("请先完成前置审批步骤");
        }

        // 3. 更新审批记录
        recordMapper.updateAction(recordId, action, comment, 0);

        log.info("审批完成: recordId={}, action={}, approverId={}", recordId, action, currentUserId);

        // 4. 判断后续流程
        ApprovalResult result = new ApprovalResult();
        result.setBusinessType(record.getBusinessType());
        result.setBusinessId(record.getBusinessId());
        result.setAction(action);
        result.setStepOrder(record.getStepOrder());

        // 5. 发送通知（通过时通知下一级审批人，拒绝/退回时通知提交人）
        if (action == ApprovalActionEnum.REJECT.getCode()
                || action == ApprovalActionEnum.RETURN.getCode()) {
            result.setTerminated(true);
            result.setApproved(false);
            notifySubmitter(record.getBusinessType(), record.getBusinessId(),
                    "审批被拒绝/退回", "您的申请已被审批人处理，结果："
                            + ApprovalActionEnum.fromCode(action).getLabel());
            return result;
        }

        // 通过 → 检查是否所有步骤都已通过
        List<ApprovalRecord> pendingRecords = recordMapper.selectPendingByBusiness(
                record.getBusinessType(), record.getBusinessId());

        if (pendingRecords.isEmpty()) {
            result.setTerminated(false);
            result.setApproved(true);
            // 通知提交人审批全部通过
            notifySubmitter(record.getBusinessType(), record.getBusinessId(),
                    "审批全部通过", "您的申请已全部审批通过。");
        } else {
            result.setTerminated(false);
            result.setApproved(false);
            // 通知下一级审批人
            for (ApprovalRecord r : pendingRecords) {
                notificationService.sendApprovalNotify(r.getApproverId(),
                        "审批待办: 请处理下一步审批",
                        "上级审批已通过，请您登录审批工作台处理下一步。",
                        record.getBusinessType(), record.getBusinessId());
            }
        }

        return result;
    }

    /**
     * 查询某业务的所有审批记录
     */
    public List<ApprovalRecord> getApprovalRecords(int businessType, long businessId) {
        return recordMapper.selectByBusiness(businessType, businessId);
    }

    /**
     * 通知提交人
     */
    private void notifySubmitter(int businessType, long businessId, String title, String content) {
        try {
            // 通过业务类型找到提交人并通知
            // 简化：通知同一业务的所有相关审批人
            List<ApprovalRecord> allRecords = recordMapper.selectByBusiness(businessType, businessId);
            if (!allRecords.isEmpty()) {
                Long firstApprover = allRecords.get(0).getApproverId();
                notificationService.sendApprovalNotify(firstApprover, title, content, businessType, businessId);
            }
        } catch (Exception e) {
            log.warn("通知发送失败: {}", e.getMessage());
        }
    }

    // ═══════════════ 审批人解析 ═══════════════

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
            case "finance_specialist":
                return resolveFinanceSpecialist();
            default:
                log.warn("未知的审批人指向: {}", approverTarget);
                return null;
        }
    }

    private String resolveApproverName(String approverTarget, ApprovalContext context, Long approverId) {
        if (approverId == null) return null;
        SysUser user = sysUserMapper.findById(approverId);
        return user != null ? user.getUsername() : ("用户" + approverId);
    }

    private Long resolveDeptManager(Long deptId) {
        if (deptId == null) return null;
        Department dept = departmentMapper.selectById(deptId);
        if (dept == null || dept.getManagerId() == null) {
            log.warn("部门无负责人: deptId={}", deptId);
            return null;
        }
        Employee manager = employeeMapper.selectById(dept.getManagerId());
        if (manager == null || manager.getUserId() == null) {
            log.warn("部门负责人无关联系统账号: deptId={}, managerId={}", deptId, dept.getManagerId());
            return null;
        }
        return manager.getUserId();
    }

    private Long resolveHrSpecialist() {
        SysUser hr = sysUserMapper.findFirstByRoleCode("ROLE_HR");
        if (hr == null) {
            log.warn("系统中未找到HR专员");
            return null;
        }
        return hr.getId();
    }

    private Long resolveFinanceSpecialist() {
        SysUser finance = sysUserMapper.findFirstByRoleCode("ROLE_FINANCE");
        if (finance == null) {
            log.warn("系统中未找到财务专员");
            return null;
        }
        return finance.getId();
    }

    // ═══════════════ 内部类 ═══════════════

    @lombok.Data
    public static class ApprovalContext {
        private Long deptId;
        private Long oldDeptId;
        private Long newDeptId;
        /** 是否需要 HR 审批（默认 true，兼容现有多级审批流程） */
        private boolean needHr = true;
        /** 是否有薪资调整（默认 false，调岗薪资调整时触发财务审批） */
        private boolean hasSalaryAdjust = false;

        public static ApprovalContext ofDept(Long deptId) {
            ApprovalContext ctx = new ApprovalContext();
            ctx.deptId = deptId;
            return ctx;
        }

        public static ApprovalContext ofDept(Long deptId, boolean needHr) {
            ApprovalContext ctx = new ApprovalContext();
            ctx.deptId = deptId;
            ctx.needHr = needHr;
            return ctx;
        }

        public static ApprovalContext ofTransfer(Long oldDeptId, Long newDeptId) {
            ApprovalContext ctx = new ApprovalContext();
            ctx.oldDeptId = oldDeptId;
            ctx.newDeptId = newDeptId;
            return ctx;
        }

        public static ApprovalContext ofTransfer(Long oldDeptId, Long newDeptId, boolean hasSalaryAdjust) {
            ApprovalContext ctx = new ApprovalContext();
            ctx.oldDeptId = oldDeptId;
            ctx.newDeptId = newDeptId;
            ctx.hasSalaryAdjust = hasSalaryAdjust;
            return ctx;
        }
    }

    @lombok.Data
    public static class ApprovalResult {
        private int businessType;
        private long businessId;
        private int action;
        private int stepOrder;
        private boolean terminated;
        private boolean approved;
    }
}
