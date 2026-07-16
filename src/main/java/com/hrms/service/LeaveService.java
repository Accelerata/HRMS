package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.LeaveApplyDTO;
import com.hrms.entity.*;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.LeaveType;
import com.hrms.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 假期余额与请假服务（含请假审批流）
 *
 * 核心业务规则：
 * 1. 年假计算（按入职年限）：不满1年0天 / 1-9年5天 / 10-19年10天 / 20年+15天
 * 2. 调休计算：累计加班小时数 / 8 = 调休天数，不足8小时的部分不计
 * 3. 余额占用：提交审批时扣减（占用），审批拒绝/取消时回补；年假/调休校验余额，事假/病假不限制
 * 4. 审批分级（6.3.4）：
 *    - 年假/调休 ≤3天：直接上级；>3天：直接上级 → 部门负责人
 *    - 事假/病假 ≤1天：直接上级；>1天：直接上级 → 部门负责人
 *    - 婚假/产假/丧假：直接上级一级审批，通过后 HR 备案（无需二审）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    /** 每8小时加班 = 1天调休 */
    private static final BigDecimal HOURS_PER_COMP_DAY = new BigDecimal("8");

    /** 申请状态：0-草稿 1-审批中 2-已通过 3-已拒绝 4-已取消 */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_CANCELED = 4;

    private final LeaveBalanceMapper leaveBalanceMapper;
    private final LeaveApplicationMapper leaveApplicationMapper;
    private final OvertimeRecordMapper overtimeRecordMapper;
    private final EmployeeMapper employeeMapper;
    private final SysUserMapper sysUserMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalStateMachineService stateMachine;
    private final NotificationService notificationService;

    // ═══════════════ 假期余额查询 ═══════════════

    /** 查询员工假期余额 */
    public List<LeaveBalance> getBalances(Long employeeId, int year) {
        return leaveBalanceMapper.selectByEmployeeAndYear(employeeId, year);
    }

    /** 初始化员工年假余额 */
    @Transactional
    public void initAnnualLeaveBalance(Long employeeId, LocalDate entryDate, int year) {
        BigDecimal annualDays = calculateAnnualLeaveDays(entryDate, year);
        LeaveBalance existing = leaveBalanceMapper.selectByEmployeeTypeAndYear(
                employeeId, LeaveType.ANNUAL.getCode(), year);

        if (existing == null) {
            LeaveBalance balance = new LeaveBalance();
            balance.setEmployeeId(employeeId);
            balance.setLeaveType(LeaveType.ANNUAL.getCode());
            balance.setTotalDays(annualDays);
            balance.setUsedDays(BigDecimal.ZERO);
            balance.setRemainingDays(annualDays);
            balance.setYear(year);
            leaveBalanceMapper.insert(balance);
        } else {
            existing.setTotalDays(annualDays);
            existing.setRemainingDays(annualDays.subtract(existing.getUsedDays()));
            leaveBalanceMapper.update(existing);
        }
        log.info("初始化年假余额: employeeId={}, year={}, days={}", employeeId, year, annualDays);
    }

    // ═══════════════ 请假申请 ═══════════════

    /**
     * 创建请假申请（草稿，仅校验余额不扣减，扣减在提交时执行）
     */
    @Transactional
    public LeaveApplication apply(LeaveApplyDTO dto) {
        Employee employee = resolveCurrentEmployee();
        if (dto.getEmployeeId() != null && !dto.getEmployeeId().equals(employee.getId())) {
            throw BaseException.forbidden("不能代他人提交请假申请");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw BaseException.badRequest("结束日期不能早于开始日期");
        }
        if (dto.getDays() == null || dto.getDays().compareTo(BigDecimal.ZERO) <= 0) {
            throw BaseException.badRequest("请假天数必须大于0");
        }

        LeaveType type = LeaveType.fromCode(dto.getLeaveType());

        // 对需要校验余额的假期类型，检查余额（不扣减）
        if (type == LeaveType.ANNUAL || type == LeaveType.COMPENSATORY) {
            LeaveBalance balance = requireBalance(employee.getId(), dto.getLeaveType(), dto.getStartDate().getYear());
            if (!hasEnoughBalance(balance, dto.getDays())) {
                throw BaseException.badRequest(
                        type.getLabel() + "余额不足，当前剩余" + balance.getRemainingDays() + "天");
            }
        }

        LeaveApplication application = new LeaveApplication();
        application.setEmployeeId(employee.getId());
        application.setLeaveType(dto.getLeaveType());
        application.setStartDate(dto.getStartDate());
        application.setEndDate(dto.getEndDate());
        application.setDays(dto.getDays());
        application.setReason(dto.getReason());
        application.setHandoverTo(dto.getHandoverTo());
        application.setStatus(STATUS_DRAFT);
        leaveApplicationMapper.insert(application);

        log.info("请假申请草稿创建成功: id={}, employeeId={}, type={}, days={}",
                application.getId(), employee.getId(), type.getLabel(), dto.getDays());
        return application;
    }

    /**
     * 提交请假申请进入审批：占用余额 + 按 6.3.4 规则生成审批待办
     */
    @Transactional
    public void submit(Long id) {
        LeaveApplication application = requireApplication(id);
        Employee employee = resolveCurrentEmployee();
        if (!application.getEmployeeId().equals(employee.getId())) {
            throw BaseException.forbidden("只能提交本人的请假申请");
        }
        if (application.getStatus() != STATUS_DRAFT) {
            throw BaseException.badRequest("仅草稿状态可提交");
        }

        LeaveType type = LeaveType.fromCode(application.getLeaveType());

        // 占用余额（年假/调休）
        if (type == LeaveType.ANNUAL || type == LeaveType.COMPENSATORY) {
            LeaveBalance balance = requireBalance(employee.getId(), application.getLeaveType(),
                    application.getStartDate().getYear());
            deductBalance(balance, application.getDays());
            leaveBalanceMapper.update(balance);
        }

        application.setStatus(STATUS_PENDING);
        leaveApplicationMapper.updateStatus(id, STATUS_PENDING);

        // 生成审批待办（直接上级 [→ 部门负责人]）
        boolean needDeptManager = needDeptManagerApproval(type, application.getDays());
        stateMachine.startApproval(BusinessTypeEnum.LEAVE.getCode(), id,
                ApprovalStateMachineService.ApprovalContext.ofLeave(
                        employee.getId(), employee.getDeptId(), employee.getUserId(), needDeptManager));

        log.info("请假申请已提交审批: id={}, type={}, days={}, needDeptManager={}",
                id, type.getLabel(), application.getDays(), needDeptManager);
    }

    /**
     * 审批请假申请
     */
    @Transactional
    public void approve(Long id, ApprovalActionDTO dto) {
        LeaveApplication application = requireApplication(id);
        if (application.getStatus() != STATUS_PENDING) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.LEAVE.getCode(), id);
        ApprovalRecord myRecord = records.stream()
                .filter(r -> r.getIsPending() == 1 && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        if (result.isTerminated()) {
            // 拒绝/退回 → 回补余额
            application.setStatus(STATUS_REJECTED);
            leaveApplicationMapper.updateStatus(id, STATUS_REJECTED);
            restoreBalance(application);
            log.info("请假申请被拒绝: id={}, 余额已回补", id);
            return;
        }

        if (result.isApproved()) {
            application.setStatus(STATUS_APPROVED);
            leaveApplicationMapper.updateStatus(id, STATUS_APPROVED);
            notifyHrFilingIfNeeded(application);
            log.info("请假申请审批通过: id={}", id);
        }
    }

    /**
     * 取消请假申请（仅审批中 + 本人 + 尚无审批人处理）
     */
    @Transactional
    public void cancel(Long id) {
        LeaveApplication application = requireApplication(id);
        Employee employee = resolveCurrentEmployee();
        if (!application.getEmployeeId().equals(employee.getId())) {
            throw BaseException.forbidden("只能取消本人的请假申请");
        }
        if (application.getStatus() != STATUS_PENDING) {
            throw BaseException.badRequest("仅审批中状态可取消");
        }

        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.LEAVE.getCode(), id);
        boolean anyProcessed = records.stream().anyMatch(r -> r.getIsPending() != 1);
        if (anyProcessed) {
            throw BaseException.badRequest("已有审批人处理，无法取消");
        }

        leaveApplicationMapper.updateStatus(id, STATUS_CANCELED);
        restoreBalance(application);
        // 关闭关联待办审批记录
        closePendingApprovalRecords(id);

        log.info("请假申请已取消: id={}, 余额已回补", id);
    }

    /** 查询员工请假记录 */
    public List<LeaveApplication> getApplications(Long employeeId, int page, int size) {
        int offset = (page - 1) * size;
        return leaveApplicationMapper.selectByEmployee(employeeId, offset, size);
    }

    // ═══════════════ 审批辅助 ═══════════════

    /**
     * 6.3.4 分级规则：年假/调休>3天、事假/病假>1天 需部门负责人二审
     */
    private boolean needDeptManagerApproval(LeaveType type, BigDecimal days) {
        if (type == LeaveType.ANNUAL || type == LeaveType.COMPENSATORY) {
            return days.compareTo(new BigDecimal("3")) > 0;
        }
        if (type == LeaveType.PERSONAL || type == LeaveType.SICK) {
            return days.compareTo(BigDecimal.ONE) > 0;
        }
        return false;
    }

    /**
     * 婚假/产假/丧假审批通过后向 HR 备案（不产生审批任务）
     */
    private void notifyHrFilingIfNeeded(LeaveApplication application) {
        LeaveType type = LeaveType.fromCode(application.getLeaveType());
        if (type != LeaveType.MARRIAGE && type != LeaveType.MATERNITY && type != LeaveType.BEREAVEMENT) {
            return;
        }
        try {
            Employee employee = employeeMapper.selectById(application.getEmployeeId());
            String empName = employee != null ? employee.getName() : "员工#" + application.getEmployeeId();
            SysUser hr = sysUserHr();
            if (hr != null) {
                notificationService.sendApprovalNotify(hr.getId(), "请假备案: " + type.getLabel(),
                        empName + " 的" + type.getLabel() + "申请已审批通过（"
                                + application.getStartDate() + " 至 " + application.getEndDate()
                                + "，共" + application.getDays() + "天），请备案。",
                        BusinessTypeEnum.LEAVE.getCode(), application.getId());
            }
        } catch (Exception e) {
            log.warn("HR备案通知发送失败: {}", e.getMessage());
        }
    }

    private SysUser sysUserHr() {
        try {
            return sysUserMapper.findFirstByRoleCode("ROLE_HR");
        } catch (Exception e) {
            log.warn("未找到HR专员: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 回补已占用的假期余额（拒绝/取消时调用）
     */
    private void restoreBalance(LeaveApplication application) {
        LeaveType type = LeaveType.fromCode(application.getLeaveType());
        if (type != LeaveType.ANNUAL && type != LeaveType.COMPENSATORY) {
            return;
        }
        LeaveBalance balance = leaveBalanceMapper.selectByEmployeeTypeAndYear(
                application.getEmployeeId(), application.getLeaveType(),
                application.getStartDate().getYear());
        if (balance == null) {
            log.warn("回补余额失败，余额记录不存在: employeeId={}, type={}",
                    application.getEmployeeId(), type.getLabel());
            return;
        }
        BigDecimal newUsed = balance.getUsedDays().subtract(application.getDays());
        balance.setUsedDays(newUsed.max(BigDecimal.ZERO));
        balance.setRemainingDays(balance.getTotalDays().subtract(balance.getUsedDays()));
        leaveBalanceMapper.update(balance);
        log.info("假期余额已回补: employeeId={}, type={}, days={}",
                application.getEmployeeId(), type.getLabel(), application.getDays());
    }

    private void closePendingApprovalRecords(Long leaveId) {
        try {
            approvalRecordMapper.closePendingByBusiness(BusinessTypeEnum.LEAVE.getCode(), leaveId);
        } catch (Exception e) {
            log.warn("关闭待办审批记录失败: {}", e.getMessage());
        }
    }

    private LeaveApplication requireApplication(Long id) {
        LeaveApplication application = leaveApplicationMapper.selectById(id);
        if (application == null) {
            throw BaseException.notFound("请假申请不存在");
        }
        return application;
    }

    private Employee resolveCurrentEmployee() {
        Long employeeId = BaseContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw BaseException.badRequest("当前用户无关联员工档案");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw BaseException.notFound("员工档案不存在");
        }
        return employee;
    }

    private LeaveBalance requireBalance(Long employeeId, Integer leaveType, int year) {
        LeaveType type = LeaveType.fromCode(leaveType);
        LeaveBalance balance = leaveBalanceMapper.selectByEmployeeTypeAndYear(employeeId, leaveType, year);
        if (balance == null) {
            throw BaseException.badRequest("未找到" + type.getLabel() + "余额记录，请先初始化");
        }
        return balance;
    }

    // ═══════════════ 核心计算逻辑 ═══════════════

    /**
     * 根据入职时长计算年假天数
     *
     * @param entryDate 入职日期
     * @param year      计算年份
     * @return 当年年假总天数
     */
    public BigDecimal calculateAnnualLeaveDays(LocalDate entryDate, int year) {
        int serviceYears = year - entryDate.getYear();

        if (serviceYears < 1) {
            return BigDecimal.ZERO;
        } else if (serviceYears < 10) {
            return new BigDecimal("5");
        } else if (serviceYears < 20) {
            return new BigDecimal("10");
        } else {
            return new BigDecimal("15");
        }
    }

    /**
     * 根据加班记录计算调休天数
     * 规则：累计加班时长 / 8 小时 = 调休天数，不满8小时的部分不计
     *
     * @param overtimeRecords 未转换的加班记录列表
     * @return 可转换的调休天数
     */
    public BigDecimal calculateCompensatoryDays(List<OvertimeRecord> overtimeRecords) {
        if (overtimeRecords == null || overtimeRecords.isEmpty()) {
            return BigDecimal.ZERO.setScale(1);
        }

        BigDecimal totalHours = overtimeRecords.stream()
                .map(OvertimeRecord::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalHours.divide(HOURS_PER_COMP_DAY, 0, RoundingMode.DOWN).setScale(1);
    }

    /**
     * 校验余额是否足够
     *
     * @param balance 余额记录
     * @param useDays 需要使用的天数
     * @return true-足够, false-不足
     */
    public boolean hasEnoughBalance(LeaveBalance balance, BigDecimal useDays) {
        LeaveType type = LeaveType.fromCode(balance.getLeaveType());

        if (type == LeaveType.ANNUAL || type == LeaveType.COMPENSATORY) {
            return balance.getRemainingDays().compareTo(useDays) >= 0;
        }

        // 事假、病假等不限制余额
        return true;
    }

    /**
     * 扣减假期余额
     *
     * @param balance 当前余额记录
     * @param useDays 请假天数
     * @return 扣减后的余额
     * @throws BaseException 余额不足时抛出
     */
    public LeaveBalance deductBalance(LeaveBalance balance, BigDecimal useDays) {
        if (!hasEnoughBalance(balance, useDays)) {
            throw BaseException.badRequest(
                    "余额不足，当前剩余" + balance.getRemainingDays() + "天，无法请假" + useDays + "天");
        }

        BigDecimal newUsed = balance.getUsedDays().add(useDays);
        BigDecimal newRemaining = balance.getTotalDays().subtract(newUsed);

        balance.setUsedDays(newUsed);
        balance.setRemainingDays(newRemaining);

        return balance;
    }
}
