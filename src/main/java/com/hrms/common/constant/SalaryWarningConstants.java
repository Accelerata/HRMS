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

    // ────────────── 预警编码（黄色/橙色，不阻断审批） ──────────────

    public static final String WARN_LEAVE_OVER_15 = "LEAVE_OVER_15_DAYS";
    public static final String WARN_OVERTIME_OVER_50H = "OVERTIME_OVER_50H";
    public static final String WARN_SALARY_CHANGE_OVER_30PCT = "SALARY_CHANGE_OVER_30PCT";

    // ────────────── 阻断编码（红色，必须修复后才能提交审批） ──────────────

    /** 实发工资为0或负数 */
    public static final String BLOCK_NET_PAY_ZERO_OR_NEGATIVE = "BLOCK_NET_PAY_ZERO_OR_NEGATIVE";

    /** 社保配置不存在 */
    public static final String BLOCK_SI_CONFIG_MISSING = "BLOCK_SI_CONFIG_MISSING";

    /** 薪资账套未配置 */
    public static final String BLOCK_SALARY_ACCOUNT_MISSING = "BLOCK_SALARY_ACCOUNT_MISSING";

    /** 员工状态异常（已离职/待入职） */
    public static final String BLOCK_EMPLOYEE_STATUS_INVALID = "BLOCK_EMPLOYEE_STATUS_INVALID";

    /** 应发工资异常（为0） */
    public static final String BLOCK_GROSS_PAY_ZERO = "BLOCK_GROSS_PAY_ZERO";

    // ────────────── 默认扣款标准 ──────────────

    /** 每次迟到扣款（元） */
    public static final double DEFAULT_LATE_FINE = 50.0;

    /** 每次早退扣款（元） */
    public static final double DEFAULT_EARLY_FINE = 50.0;

    /** 月计薪天数 */
    public static final double PAID_DAYS_PER_MONTH = 21.75;
}
