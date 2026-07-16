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
 * 1. 根据审批模板生成审批记录（含逾期时间、提交人、委托自动改派）
 * 2. 解析审批人（部门主管 / HR专员 / 原部门主管 / 新部门主管 / 财务专员 / 直接上级）
 * 3. 处理审批动作（通过/拒绝/退回），含顺序门控与代审留痕
 * 4. 判断审批是否全部完成
 * 5. 审批结果通知（提交人以 context.submitterUserId 为准）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalStateMachineService {

    private final ApprovalTemplateMapper templateMapper;
    private final ApprovalRecordMapper recordMapper;
    private final ApprovalDelegationMapper delegationMapper;
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    private static final int DUE_HOURS = 48; // 每级审批 48h 截止

    /**
     * 启动审批流程——根据业务类型自动生成审批待办记录
     *
     * @param businessType 业务类型 (1-入职 2-转正 3-调岗 4-离职 5-薪资批次 6-请假 7-补卡)
     * @param businessId   业务单据ID
     * @param context      审批上下文（用于解析审批人、条件步骤、提交人）
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
            if (skipByCondition(tpl, context)) {
                continue;
            }

            Long approverId = resolveApprover(tpl.getApproverTarget(), context);
            if (approverId == null) {
                // 兜底：解析失败时改派 HR 专员，避免审批步骤静默丢失
                log.warn("审批人解析失败，回退HR专员: target={}, businessType={}, businessId={}",
                        tpl.getApproverTarget(), businessType, businessId);
                approverId = resolveHrSpecialist();
                if (approverId == null) {
                    log.error("HR专员亦无法解析，跳过该步骤: businessType={}, businessId={}", businessType, businessId);
                    continue;
                }
            }
            String approverName = resolveApproverName(approverId);

            ApprovalRecord record = new ApprovalRecord();
            record.setBusinessType(businessType);
            record.setBusinessId(businessId);
            record.setStepOrder(tpl.getStepOrder());
            record.setApproverId(approverId);
            record.setApproverName(approverName);
            record.setAssignType(0);
            record.setSubmitterId(context.getSubmitterUserId());

            // 委托自动改派：审批人存在生效中委托时改派被委托人
            applyDelegationIfActive(record);

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
                String content = r.getAssignType() != null && r.getAssignType() == 2
                        ? "您有一项受 " + r.getOriginalApproverName() + " 委托的审批任务待处理，请登录审批工作台查看。"
                        : "您有一项新的审批任务待处理，请登录审批工作台查看。";
                notificationService.sendApprovalNotify(r.getApproverId(), title, content,
                        businessType, businessId);
            }
        }
    }

    /**
     * 条件步骤过滤：命中条件且上下文不满足时跳过
     */
    private boolean skipByCondition(ApprovalTemplate tpl, ApprovalContext context) {
        String condition = tpl.getConditionExpr();
        if (condition == null) {
            return false;
        }
        boolean skip = switch (condition) {
            case "needHr" -> !context.isNeedHr();
            case "hasSalaryAdjust" -> !context.isHasSalaryAdjust();
            case "leaveNeedDeptManager" -> !context.isLeaveNeedDeptManager();
            default -> false;
        };
        if (skip) {
            log.info("跳过条件审批步骤: templateId={}, stepName={}, conditionExpr={}",
                    tpl.getId(), tpl.getStepName(), condition);
        }
        return skip;
    }

    /**
     * 委托自动改派：审批人存在生效中的委托时，改派被委托人并留存原审批人（assignType=2）
     */
    private void applyDelegationIfActive(ApprovalRecord record) {
        try {
            ApprovalDelegation delegation = delegationMapper.selectActiveByDelegator(
                    record.getApproverId(), LocalDateTime.now());
            if (delegation == null) {
                return;
            }
            record.setOriginalApproverId(record.getApproverId());
            record.setOriginalApproverName(record.getApproverName());
            record.setApproverId(delegation.getDelegateId());
            record.setApproverName(delegation.getDelegateName());
            record.setAssignType(2);
            log.info("审批任务委托改派: 原审批人={}({}), 被委托人={}({})",
                    record.getOriginalApproverName(), record.getOriginalApproverId(),
                    delegation.getDelegateName(), delegation.getDelegateId());
        } catch (Exception e) {
            log.warn("委托改派检查失败，按原审批人分配: {}", e.getMessage());
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

        // 3. 代审留痕：转交/委托任务审批时附加「XXX 代 YYY 审批」前缀
        String finalComment = buildAgentComment(record, comment);

        // 4. 更新审批记录
        recordMapper.updateAction(recordId, action, finalComment, 0);

        log.info("审批完成: recordId={}, action={}, approverId={}", recordId, action, currentUserId);

        // 5. 判断后续流程
        ApprovalResult result = new ApprovalResult();
        result.setBusinessType(record.getBusinessType());
        result.setBusinessId(record.getBusinessId());
        result.setAction(action);
        result.setStepOrder(record.getStepOrder());

        // 6. 发送通知（通过时通知下一级审批人，拒绝/退回时通知提交人）
        if (action == ApprovalActionEnum.REJECT.getCode()
                || action == ApprovalActionEnum.RETURN.getCode()) {
            result.setTerminated(true);
            result.setApproved(false);
            notifySubmitter(record, "审批被拒绝/退回", "您的申请已被审批人处理，结果："
                    + ApprovalActionEnum.fromCode(action).getLabel()
                    + (finalComment != null && !finalComment.isBlank() ? "，意见：" + finalComment : ""));
            return result;
        }

        // 通过 → 检查是否所有步骤都已通过
        List<ApprovalRecord> pendingRecords = recordMapper.selectPendingByBusiness(
                record.getBusinessType(), record.getBusinessId());

        if (pendingRecords.isEmpty()) {
            result.setTerminated(false);
            result.setApproved(true);
            // 通知提交人审批全部通过
            notifySubmitter(record, "审批全部通过", "您的申请已全部审批通过。");
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
     * 代审留痕：assignType != 0（1-转交 2-委托）时，附加「XXX 代 YYY 审批」前缀
     */
    private String buildAgentComment(ApprovalRecord record, String comment) {
        if (record.getAssignType() == null || record.getAssignType() == 0) {
            return comment;
        }
        String operator = record.getApproverName() != null ? record.getApproverName() : "用户" + record.getApproverId();
        String original = record.getOriginalApproverName() != null
                ? record.getOriginalApproverName() : "用户" + record.getOriginalApproverId();
        String prefix = operator + " 代 " + original + " 审批";
        return (comment == null || comment.isBlank()) ? prefix : prefix + "：" + comment;
    }

    /**
     * 查询某业务的所有审批记录
     */
    public List<ApprovalRecord> getApprovalRecords(int businessType, long businessId) {
        return recordMapper.selectByBusiness(businessType, businessId);
    }

    /**
     * 通知提交人（以审批记录上的 submitterId 为准，缺失时回退旧逻辑）
     */
    private void notifySubmitter(ApprovalRecord record, String title, String content) {
        try {
            Long submitterId = record.getSubmitterId();
            if (submitterId == null) {
                // 兼容历史数据：回退到同业务第一条审批记录
                List<ApprovalRecord> allRecords = recordMapper.selectByBusiness(
                        record.getBusinessType(), record.getBusinessId());
                submitterId = allRecords.stream()
                        .map(ApprovalRecord::getSubmitterId)
                        .filter(java.util.Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            }
            if (submitterId == null) {
                log.warn("提交人未知，通知跳过: businessType={}, businessId={}",
                        record.getBusinessType(), record.getBusinessId());
                return;
            }
            notificationService.sendApprovalNotify(submitterId, title, content,
                    record.getBusinessType(), record.getBusinessId());
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
            case "direct_supervisor":
                return resolveDirectSupervisor(context);
            default:
                log.warn("未知的审批人指向: {}", approverTarget);
                return null;
        }
    }

    private String resolveApproverName(Long approverId) {
        if (approverId == null) return null;
        SysUser user = sysUserMapper.findById(approverId);
        return user != null ? user.getUsername() : ("用户" + approverId);
    }

    /**
     * 直接上级解析链：employee.report_to → 部门负责人 → 返回 null（由调用方兜底HR）
     */
    private Long resolveDirectSupervisor(ApprovalContext context) {
        Long employeeId = context.getEmployeeId();
        if (employeeId != null) {
            Employee employee = employeeMapper.selectById(employeeId);
            if (employee != null && employee.getReportTo() != null) {
                Employee supervisor = employeeMapper.selectById(employee.getReportTo());
                if (supervisor != null && supervisor.getUserId() != null) {
                    return supervisor.getUserId();
                }
                log.warn("直接上级无关联系统账号: employeeId={}, reportTo={}", employeeId, employee.getReportTo());
            }
        }
        // 回退部门负责人
        Long deptId = context.getDeptId();
        if (deptId == null && employeeId != null) {
            Employee employee = employeeMapper.selectById(employeeId);
            deptId = employee != null ? employee.getDeptId() : null;
        }
        Long deptManager = resolveDeptManager(deptId);
        if (deptManager != null) {
            log.info("直接上级缺失，回退部门负责人: employeeId={}, deptId={}", employeeId, deptId);
        }
        return deptManager;
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
        /** 业务员工ID（请假/补卡申请人，用于直接上级解析） */
        private Long employeeId;
        /** 提交人用户ID（sys_user.id，审批结果通知对象） */
        private Long submitterUserId;
        /** 是否需要 HR 审批（默认 true，兼容现有多级审批流程） */
        private boolean needHr = true;
        /** 是否有薪资调整（默认 false，调岗薪资调整时触发财务审批） */
        private boolean hasSalaryAdjust = false;
        /** 请假是否需要部门负责人二审（年假/调休>3天、事假/病假>1天） */
        private boolean leaveNeedDeptManager = false;

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

        /** 请假/补卡：按员工构建（直接上级解析 + 提交人通知） */
        public static ApprovalContext ofEmployee(Long employeeId, Long deptId, Long submitterUserId) {
            ApprovalContext ctx = new ApprovalContext();
            ctx.employeeId = employeeId;
            ctx.deptId = deptId;
            ctx.submitterUserId = submitterUserId;
            return ctx;
        }

        /** 请假：附加部门负责人二审条件 */
        public static ApprovalContext ofLeave(Long employeeId, Long deptId, Long submitterUserId,
                                              boolean leaveNeedDeptManager) {
            ApprovalContext ctx = ofEmployee(employeeId, deptId, submitterUserId);
            ctx.leaveNeedDeptManager = leaveNeedDeptManager;
            return ctx;
        }

        public ApprovalContext withSubmitter(Long submitterUserId) {
            this.submitterUserId = submitterUserId;
            return this;
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
