package com.hrms.common.constant;

/**
 * 薪资预警常量
 */
public final class SalaryWarningConstants {

    private SalaryWarningConstants() {}

    /** 请假天数预警阈值：超过15天 */
    public static final double LEAVE_DAYS_THRESHOLD = 15.0;

    /** 加班小时预警阈值：超过50小时 */
    public static final double OVERTIME_HOURS_THRESHOLD = 50.0;

    /** 薪资环比波动预警阈值：超过30% */
    public static final double SALARY_CHANGE_RATIO_THRESHOLD = 0.30;

    // ────────────── 预警编码 ──────────────

    public static final String WARN_LEAVE_OVER_15 = "LEAVE_OVER_15_DAYS";
    public static final String WARN_OVERTIME_OVER_50H = "OVERTIME_OVER_50H";
    public static final String WARN_SALARY_CHANGE_OVER_30PCT = "SALARY_CHANGE_OVER_30PCT";

    // ────────────── 默认扣款标准 ──────────────

    /** 每次迟到扣款（元） */
    public static final double DEFAULT_LATE_FINE = 50.0;

    /** 每次早退扣款（元） */
    public static final double DEFAULT_EARLY_FINE = 50.0;

    /** 月计薪天数 */
    public static final double PAID_DAYS_PER_MONTH = 21.75;
}
