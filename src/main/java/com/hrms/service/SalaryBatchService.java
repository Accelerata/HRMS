package com.hrms.service;

import com.hrms.common.constant.SalaryWarningConstants;
import com.hrms.dto.SalaryCalcDTO;
import com.hrms.entity.*;
import com.hrms.mapper.*;
import com.hrms.vo.SalaryCalcResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 薪资批量核算服务
 *
 * 职责：
 * 1. 汇总考勤数据 → 组装 SalaryCalcDTO
 * 2. 调用 SalaryCalculationService 逐人计算
 * 3. 持久化结果到 salary_record
 * 4. 触发预警通知（高亮标记）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryBatchService {

    private final SalaryCalculationService calculationService;
    private final SalaryAccountMapper salaryAccountMapper;
    private final SalaryRecordMapper salaryRecordMapper;
    private final EmployeeMapper employeeMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final LeaveApplicationMapper leaveApplicationMapper;
    private final OvertimeRecordMapper overtimeRecordMapper;
    private final SocialInsuranceConfigMapper siConfigMapper;

    /**
     * 单个员工薪资核算
     *
     * @param employeeId 员工ID
     * @param year       核算年份
     * @param month      核算月份
     * @return 薪资计算结果
     */
    @Transactional
    public SalaryCalcResultVO calculateForEmployee(Long employeeId, int year, int month) {
        // 1. 查询员工信息
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在: " + employeeId);
        }
        // 待入职/已离职员工不核算
        if (employee.getStatus() != null && (employee.getStatus() == 0 || employee.getStatus() == 4)) {
            throw new IllegalStateException("该员工" + (employee.getStatus() == 0 ? "待入职" : "已离职") + "，不参与薪资核算");
        }

        // 2. 查询薪资账套
        SalaryAccount account = salaryAccountMapper.selectByEmployeeId(employeeId);
        if (account == null) {
            throw new IllegalArgumentException("员工薪资账套未配置: " + employeeId);
        }

        // 3. 查询社保公积金配置
        SocialInsuranceConfig siConfig = siConfigMapper.selectLatest();

        // 4. 汇总考勤数据
        AttendanceStats stats = aggregateAttendance(employeeId, year, month);

        // 5. 查询该年度历史薪资记录（用于个税累计预扣）
        List<SalaryRecord> historyRecords = salaryRecordMapper.selectByEmployeeAndYear(employeeId, year);

        // 6. 组装累计数据
        CumulativeData cumulative = buildCumulativeData(historyRecords);
        SalaryRecord lastMonthRecord = getLastMonthRecord(historyRecords, month);

        // 7. 组装计算输入 DTO
        SalaryCalcDTO input = SalaryCalcDTO.builder()
                .employeeId(employeeId)
                .employeeStatus(employee.getStatus())
                .year(year).month(month)
                .basicSalary(account.getBasicSalary())
                .positionSalary(account.getPositionSalary())
                .probationRatio(account.getProbationRatio())
                .lateCount(stats.lateCount)
                .earlyCount(stats.earlyCount)
                .absentDays(stats.absentDays)
                .personalLeaveDays(stats.personalLeaveDays)
                .overtimeHours(stats.overtimeHours)
                .socialInsuranceBase(account.getSocialInsuranceBase())
                .housingFundBase(account.getHousingFundBase())
                .cumulativeGrossPay(cumulative.grossPay)
                .cumulativeSocialInsurance(cumulative.socialInsurance)
                .cumulativeHousingFund(cumulative.housingFund)
                .cumulativeSpecialDeduction(cumulative.specialDeduction)
                .cumulativeTaxPaid(cumulative.taxPaid)
                .lastMonthNetPay(lastMonthRecord != null ? lastMonthRecord.getNetPay() : null)
                .build();

        // 8. 调用核心计算
        SalaryCalcResultVO result = calculationService.calculate(input, siConfig);

        // 9. 持久化
        saveRecord(employeeId, year, month, result);

        return result;
    }

    /**
     * 批量核算（全公司在职员工）
     */
    @Transactional
    public int batchCalculate(int year, int month) {
        List<SalaryAccount> accounts = salaryAccountMapper.selectAllActive();
        int successCount = 0;

        for (SalaryAccount account : accounts) {
            try {
                calculateForEmployee(account.getEmployeeId(), year, month);
                successCount++;
            } catch (Exception e) {
                log.error("员工 {} 薪资核算失败: {}", account.getEmployeeId(), e.getMessage());
            }
        }

        log.info("批量薪资核算完成: {}/{} 成功", successCount, accounts.size());
        return successCount;
    }

    // ═══════════════════════════════════════════════
    // 私有辅助方法
    // ═══════════════════════════════════════════════

    /**
     * 汇总考勤统计数据
     */
    private AttendanceStats aggregateAttendance(Long employeeId, int year, int month) {
        AttendanceStats stats = new AttendanceStats();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 迟到/早退/旷工
        List<AttendanceRecord> records =
                attendanceRecordMapper.selectByEmployeeAndMonth(employeeId, year, month);
        if (records != null) {
            for (AttendanceRecord r : records) {
                if ("LATE".equals(r.getPunchInStatus())) stats.lateCount++;
                if ("EARLY".equals(r.getPunchOutStatus())) stats.earlyCount++;
                if ("ABSENT_HALF_DAY".equals(r.getPunchInStatus())
                        || "ABSENT_HALF_DAY".equals(r.getPunchOutStatus())) {
                    stats.absentDays = stats.absentDays.add(new BigDecimal("0.5"));
                }
            }
        }

        // 事假天数（只统计已通过的事假）
        BigDecimal leaveDays = leaveApplicationMapper.sumApprovedLeaveDays(
                employeeId, 3, startDate, endDate); // leaveType=3 事假
        stats.personalLeaveDays = leaveDays != null ? leaveDays : BigDecimal.ZERO;

        // 加班小时
        BigDecimal overtimeHours = overtimeRecordMapper.sumHoursByMonth(employeeId, year, month);
        stats.overtimeHours = overtimeHours != null ? overtimeHours : BigDecimal.ZERO;

        return stats;
    }

    /**
     * 组装个税累计预扣所需数据
     */
    private CumulativeData buildCumulativeData(List<SalaryRecord> historyRecords) {
        CumulativeData data = new CumulativeData();
        if (historyRecords != null) {
            for (SalaryRecord r : historyRecords) {
                data.grossPay = data.grossPay.add(r.getGrossPay());
                data.socialInsurance = data.socialInsurance.add(r.getSocialInsurancePersonal());
                data.housingFund = data.housingFund.add(r.getHousingFundPersonal());
                data.specialDeduction = data.specialDeduction.add(
                        r.getOtherDeduction() != null ? r.getOtherDeduction() : BigDecimal.ZERO);
                data.taxPaid = data.taxPaid.add(r.getTax());
            }
        }
        return data;
    }

    /**
     * 获取上月薪资记录（用于环比波动预警）
     */
    private SalaryRecord getLastMonthRecord(List<SalaryRecord> records, int currentMonth) {
        if (records == null) return null;
        int lastMonth = currentMonth - 1;
        for (SalaryRecord r : records) {
            if (r.getMonth() == lastMonth) return r;
        }
        return null;
    }

    /**
     * 持久化薪资记录
     */
    private void saveRecord(Long employeeId, int year, int month, SalaryCalcResultVO result) {
        SalaryRecord record = new SalaryRecord();
        record.setEmployeeId(employeeId);
        record.setYear(year);
        record.setMonth(month);
        record.setBasicSalary(result.getBasicSalary());
        record.setAttendanceDeduction(result.getAttendanceDeduction());
        record.setLeaveDeduction(result.getLeaveDeduction());
        record.setOvertimePay(BigDecimal.ZERO);
        record.setGrossPay(result.getGrossPay());
        record.setSocialInsurancePersonal(result.getSocialInsurancePersonal());
        record.setHousingFundPersonal(result.getHousingFundPersonal());
        record.setTaxableIncome(result.getTaxableIncome());
        record.setTax(result.getTax());
        record.setOtherDeduction(BigDecimal.ZERO);
        record.setNetPay(result.getNetPay());
        record.setProbationRatio(result.getProbationRatio());
        record.setLeaveDays(result.getLeaveDays());
        record.setOvertimeHours(result.getOvertimeHours());
        record.setLateCount(result.getLateCount());
        record.setEarlyCount(result.getEarlyCount());
        record.setAbsentCount(result.getAbsentCount());
        record.setWarnings(result.getWarnings() != null && !result.getWarnings().isEmpty()
                ? String.join(",", result.getWarnings()) : null);
        record.setStatus("DRAFT");

        salaryRecordMapper.insert(record);
    }

    // ═══════════════════════════════════════════════
    // 内部类
    // ═══════════════════════════════════════════════

    static class AttendanceStats {
        int lateCount = 0;
        int earlyCount = 0;
        BigDecimal absentDays = BigDecimal.ZERO;
        BigDecimal personalLeaveDays = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
    }

    static class CumulativeData {
        BigDecimal grossPay = BigDecimal.ZERO;
        BigDecimal socialInsurance = BigDecimal.ZERO;
        BigDecimal housingFund = BigDecimal.ZERO;
        BigDecimal specialDeduction = BigDecimal.ZERO;
        BigDecimal taxPaid = BigDecimal.ZERO;
    }
}
