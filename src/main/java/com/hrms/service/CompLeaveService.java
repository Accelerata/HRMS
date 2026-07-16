package com.hrms.service;

import com.hrms.common.exception.BaseException;
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
import java.time.YearMonth;
import java.util.List;

/**
 * 调休服务
 *
 * 核心职责：
 * 1. 加班时长折算入账（8小时=1天，余数滚动）
 * 2. 调休过期清零（过期日=加班次月月末）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompLeaveService {

    private static final BigDecimal HOURS_PER_DAY = new BigDecimal("8");

    private final OvertimeRecordMapper overtimeRecordMapper;
    private final CompLeaveGrantMapper compLeaveGrantMapper;
    private final LeaveBalanceMapper leaveBalanceMapper;

    /**
     * 将员工未转换加班时长折算为调休入账
     *
     * @param employeeId 员工ID
     * @return 本次入账天数（可能为0）
     */
    @Transactional
    public BigDecimal convertOvertime(Long employeeId) {
        List<OvertimeRecord> records = overtimeRecordMapper.selectUnconvertedByEmployee(employeeId);
        if (records == null || records.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalHours = records.stream()
                .map(OvertimeRecord::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // floor(hours/8) = days
        BigDecimal days = totalHours.divide(HOURS_PER_DAY, 0, RoundingMode.DOWN);
        if (days.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 确定加班月份（取最晚加班日期所在月）和过期日（次月月末）
        LocalDate latestDate = records.stream()
                .map(OvertimeRecord::getOvertimeDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
        String overtimeMonth = String.format("%d-%02d", latestDate.getYear(), latestDate.getMonthValue());
        YearMonth nextMonth = YearMonth.of(latestDate.getYear(), latestDate.getMonthValue()).plusMonths(1);
        LocalDate expireDate = nextMonth.atEndOfMonth();

        // 插入入账明细
        CompLeaveGrant grant = new CompLeaveGrant();
        grant.setEmployeeId(employeeId);
        grant.setOvertimeMonth(overtimeMonth);
        grant.setDays(days);
        grant.setUsedDays(BigDecimal.ZERO);
        grant.setExpireDate(expireDate);
        grant.setStatus(1);
        compLeaveGrantMapper.insert(grant);

        // 累加 leave_balance 调休行
        int year = latestDate.getYear();
        LeaveBalance balance = leaveBalanceMapper.selectByEmployeeTypeAndYear(
                employeeId, LeaveType.COMPENSATORY.getCode(), year);
        if (balance == null) {
            balance = new LeaveBalance();
            balance.setEmployeeId(employeeId);
            balance.setLeaveType(LeaveType.COMPENSATORY.getCode());
            balance.setTotalDays(days);
            balance.setUsedDays(BigDecimal.ZERO);
            balance.setRemainingDays(days);
            balance.setYear(year);
            leaveBalanceMapper.insert(balance);
        } else {
            balance.setTotalDays(balance.getTotalDays().add(days));
            balance.setRemainingDays(balance.getRemainingDays().add(days));
            leaveBalanceMapper.update(balance);
        }

        // 标记加班记录为已转换（幂等屏障）
        for (OvertimeRecord record : records) {
            overtimeRecordMapper.markAsConverted(record.getId());
        }

        log.info("加班折算调休入账: employeeId={}, hours={}, days={}, expireDate={}",
                employeeId, totalHours, days, expireDate);
        return days;
    }

    /**
     * 批量折算全体员工
     */
    @Transactional
    public int convertAllEmployees(List<Employee> employees) {
        int count = 0;
        for (Employee emp : employees) {
            try {
                BigDecimal days = convertOvertime(emp.getId());
                if (days.compareTo(BigDecimal.ZERO) > 0) {
                    count++;
                }
            } catch (Exception e) {
                log.error("调休折算失败: employeeId={}, error={}", emp.getId(), e.getMessage());
            }
        }
        return count;
    }
}
