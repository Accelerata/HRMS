package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.dto.LeaveApplyDTO;
import com.hrms.entity.*;
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
 * 假期余额与请假服务
 *
 * 核心业务规则：
 * 1. 年假计算（按入职年限）：
 *    - 不满1年: 0天
 *    - 1-9年: 5天
 *    - 10-19年: 10天
 *    - 20年及以上: 15天
 * 2. 调休计算：累计加班小时数 / 8 = 调休天数，不足8小时的部分不计
 * 3. 余额扣减：支持天数扣减（含半天），余额不足时抛出异常
 * 4. 年假/调休 需要校验余额，事假/病假不限制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    /** 每8小时加班 = 1天调休 */
    private static final BigDecimal HOURS_PER_COMP_DAY = new BigDecimal("8");

    private final LeaveBalanceMapper leaveBalanceMapper;
    private final LeaveApplicationMapper leaveApplicationMapper;
    private final OvertimeRecordMapper overtimeRecordMapper;
    private final EmployeeMapper employeeMapper;

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
     * 提交请假申请
     */
    @Transactional
    public LeaveApplication apply(LeaveApplyDTO dto, Long employeeId) {
        LeaveType type = LeaveType.fromCode(dto.getLeaveType());

        // 1. 对需要校验余额的假期类型，检查余额
        if (type == LeaveType.ANNUAL || type == LeaveType.COMPENSATORY) {
            int year = dto.getStartDate().getYear();
            LeaveBalance balance = leaveBalanceMapper.selectByEmployeeTypeAndYear(
                    employeeId, dto.getLeaveType(), year);

            if (balance == null) {
                throw BaseException.badRequest("未找到" + type.getLabel() + "余额记录，请先初始化");
            }

            if (!hasEnoughBalance(balance, dto.getDays())) {
                throw BaseException.badRequest(
                        type.getLabel() + "余额不足，当前剩余" + balance.getRemainingDays() + "天");
            }

            // 扣减余额
            deductBalance(balance, dto.getDays());
            leaveBalanceMapper.update(balance);
        }

        // 2. 创建请假申请
        LeaveApplication application = new LeaveApplication();
        application.setEmployeeId(employeeId);
        application.setLeaveType(dto.getLeaveType());
        application.setStartDate(dto.getStartDate());
        application.setEndDate(dto.getEndDate());
        application.setDays(dto.getDays());
        application.setReason(dto.getReason());
        application.setStatus(0); // 草稿
        leaveApplicationMapper.insert(application);

        log.info("请假申请创建成功: employeeId={}, type={}, days={}",
                employeeId, type.getLabel(), dto.getDays());
        return application;
    }

    /** 查询员工请假记录 */
    public List<LeaveApplication> getApplications(Long employeeId, int page, int size) {
        int offset = (page - 1) * size;
        return leaveApplicationMapper.selectByEmployee(employeeId, offset, size);
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
