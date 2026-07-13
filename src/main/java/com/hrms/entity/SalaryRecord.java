package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪资记录（工资条）
 * 每月核算后生成的员工薪资明细
 */
@Data
public class SalaryRecord {

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 核算年份 */
    private Integer year;

    /** 核算月份 (1-12) */
    private Integer month;

    // ────────────── 应发部分 ──────────────

    /** 应发基本工资（已含试用期折算） */
    private BigDecimal basicSalary;

    /** 考勤扣款（迟到/早退/旷工罚款） */
    private BigDecimal attendanceDeduction;

    /** 事假扣款 */
    private BigDecimal leaveDeduction;

    /** 加班补贴 */
    private BigDecimal overtimePay;

    /** 应发合计 = basicSalary - attendanceDeduction - leaveDeduction + overtimePay */
    private BigDecimal grossPay;

    // ────────────── 扣除部分 ──────────────

    /** 社保个人缴纳 */
    private BigDecimal socialInsurancePersonal;

    /** 公积金个人缴纳 */
    private BigDecimal housingFundPersonal;

    /** 应纳税所得额 */
    private BigDecimal taxableIncome;

    /** 个人所得税（累计预扣法） */
    private BigDecimal tax;

    /** 其他扣款 */
    private BigDecimal otherDeduction;

    // ────────────── 实发 ──────────────

    /** 实发工资 = grossPay - socialInsurance - housingFund - tax - otherDeduction */
    private BigDecimal netPay;

    // ────────────── 统计信息（用于预警） ──────────────

    /** 试用期薪资比例 */
    private BigDecimal probationRatio;

    /** 当月请假天数合计 */
    private BigDecimal leaveDays;

    /** 当月加班小时合计 */
    private BigDecimal overtimeHours;

    /** 迟到次数 */
    private Integer lateCount;

    /** 早退次数 */
    private Integer earlyCount;

    /** 旷工次数（半天=0.5） */
    private BigDecimal absentCount;

    /**
     * 预警标记（JSON数组字符串）
     * 例如: ["LEAVE_OVER_15_DAYS","OVERTIME_OVER_50H","SALARY_CHANGE_OVER_30PCT"]
     */
    private String warnings;

    /**
     * 状态：
     * DRAFT - 草稿
     * CONFIRMED - 已确认
     * PAID - 已发放
     */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
