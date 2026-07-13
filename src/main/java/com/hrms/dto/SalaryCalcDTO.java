package com.hrms.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 薪资计算输入参数
 */
@Data
@Builder
public class SalaryCalcDTO {

    /** 员工ID */
    private Long employeeId;

    /** 员工状态：1-试用期 2-正式 */
    private Integer employeeStatus;

    /** 核算年份 */
    private Integer year;

    /** 核算月份 */
    private Integer month;

    /** 基本工资 */
    private BigDecimal basicSalary;

    /** 岗位工资 */
    private BigDecimal positionSalary;

    /** 试用期比例（试用期员工才需要） */
    private BigDecimal probationRatio;

    // ────────────── 考勤统计 ──────────────

    /** 迟到次数 */
    private Integer lateCount;

    /** 早退次数 */
    private Integer earlyCount;

    /** 旷工天数（半天=0.5） */
    private BigDecimal absentDays;

    /** 事假天数 */
    private BigDecimal personalLeaveDays;

    // ────────────── 加班 ──────────────

    /** 当月加班小时数 */
    private BigDecimal overtimeHours;

    // ────────────── 社保公积金基数 ──────────────

    /** 社保缴纳基数（取实际工资与上下限的中间值） */
    private BigDecimal socialInsuranceBase;

    /** 公积金缴纳基数 */
    private BigDecimal housingFundBase;

    // ────────────── 累计数据（用于个税累计预扣法） ──────────────

    /** 年初至本月累计应发工资（不含本月） */
    private BigDecimal cumulativeGrossPay;

    /** 年初至本月累计社保个人缴纳（不含本月） */
    private BigDecimal cumulativeSocialInsurance;

    /** 年初至本月累计公积金个人缴纳（不含本月） */
    private BigDecimal cumulativeHousingFund;

    /** 年初至本月累计专项附加扣除（不含本月） */
    private BigDecimal cumulativeSpecialDeduction;

    /** 年初至本月累计已预缴个税（不含本月） */
    private BigDecimal cumulativeTaxPaid;

    // ────────────── 上月薪资（用于环比波动预警） ──────────────

    /** 上月实发工资（用于环比波动检测，无则为null） */
    private BigDecimal lastMonthNetPay;

    // ────────────── 扣款标准（可由公司政策配置） ──────────────

    /** 每次迟到罚款（默认50元） */
    @Builder.Default
    private BigDecimal lateFinePerTime = BigDecimal.valueOf(50);

    /** 每次早退罚款（默认50元） */
    @Builder.Default
    private BigDecimal earlyFinePerTime = BigDecimal.valueOf(50);

    /** 旷工半天扣款比例（相对于日工资的比例，默认1.0=扣1天） */
    @Builder.Default
    private BigDecimal absentDeductionRatio = BigDecimal.valueOf(1.0);

    /** 月计薪天数 */
    @Builder.Default
    private BigDecimal paidDaysPerMonth = new BigDecimal("21.75");
}
