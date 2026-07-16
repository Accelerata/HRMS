package com.hrms.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 薪资计算结果 VO
 */
@Data
@Builder
public class SalaryCalcResultVO {

    /** 员工ID */
    private Long employeeId;

    /** 核算年份 */
    private Integer year;

    /** 核算月份 */
    private Integer month;

    // ────────────── 应发明细 ──────────────

    /** 应发基本工资（含试用期折算） */
    private BigDecimal basicSalary;

    /** 考勤扣款合计（迟到+早退+旷工） */
    private BigDecimal attendanceDeduction;

    /** 事假扣款 */
    private BigDecimal leaveDeduction;

    /** 应发合计 */
    private BigDecimal grossPay;

    // ────────────── 扣除明细 ──────────────

    /** 社保个人缴纳 */
    private BigDecimal socialInsurancePersonal;

    /** 公积金个人缴纳 */
    private BigDecimal housingFundPersonal;

    /** 应纳税所得额 */
    private BigDecimal taxableIncome;

    /** 个人所得税（累计预扣法计算结果） */
    private BigDecimal tax;

    // ────────────── 实发 ──────────────

    /** 实发工资 */
    private BigDecimal netPay;

    // ────────────── 统计信息 ──────────────

    /** 试用期比例 */
    private BigDecimal probationRatio;

    /** 当月请假天数 */
    private BigDecimal leaveDays;

    /** 当月加班小时 */
    private BigDecimal overtimeHours;

    /** 迟到次数 */
    private Integer lateCount;

    /** 早退次数 */
    private Integer earlyCount;

    /** 旷工次数 */
    private BigDecimal absentCount;

    // ────────────── 预警 ──────────────

    /** 预警标记列表（黄色/橙色，不会阻断审批） */
    private List<String> warnings;

    /** 阻断标记列表（红色，必须修复后才能提交审批） */
    private List<String> blockings;

    // ────────────── 个税明细 ──────────────

    /** 累计应纳税所得额（含本月） */
    private BigDecimal cumulativeTaxableIncome;

    /** 适用税率 */
    private BigDecimal taxRate;

    /** 速算扣除数 */
    private BigDecimal quickDeduction;
}
