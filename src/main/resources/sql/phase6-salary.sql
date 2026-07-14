-- ═══════════════════════════════════════════════════
-- Phase 6: 薪资模块 — 建表脚本
-- ═══════════════════════════════════════════════════
-- 依赖关系: employee 表已创建（phase1-core.sql）
-- 说明: 本脚本包含加密列，密文明文列共存，迁移完成后再删除明文列


-- ============================================================
-- 1. 薪资账套表
-- ============================================================
DROP TABLE IF EXISTS `salary_account`;
CREATE TABLE `salary_account` (
    `id`                           BIGINT         NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `employee_id`                  BIGINT         NOT NULL                 COMMENT '员工ID',
    -- 金额字段（明文，迁移后删除）
    `basic_salary`                 DECIMAL(12,2)  DEFAULT NULL             COMMENT '基本工资（精确到分）',
    `position_salary`              DECIMAL(12,2)  DEFAULT NULL             COMMENT '岗位工资',
    `performance_salary`           DECIMAL(12,2)  DEFAULT NULL             COMMENT '绩效工资',
    `probation_ratio`              DECIMAL(4,3)   DEFAULT NULL             COMMENT '试用期薪资比例（如0.80）',
    `social_insurance_base`        DECIMAL(12,2)  DEFAULT NULL             COMMENT '社保缴纳基数',
    `housing_fund_base`            DECIMAL(12,2)  DEFAULT NULL             COMMENT '公积金缴纳基数',
    -- 金额字段（密文）
    `basic_salary_encrypted`       VARCHAR(256)   DEFAULT NULL             COMMENT '基本工资(加密)',
    `position_salary_encrypted`    VARCHAR(256)   DEFAULT NULL             COMMENT '岗位工资(加密)',
    `performance_salary_encrypted` VARCHAR(256)   DEFAULT NULL             COMMENT '绩效工资(加密)',
    `social_insurance_base_encrypted` VARCHAR(256) DEFAULT NULL            COMMENT '社保基数(加密)',
    `housing_fund_base_encrypted`  VARCHAR(256)   DEFAULT NULL             COMMENT '公积金基数(加密)',
    `encryption_version`           TINYINT        NOT NULL DEFAULT 1       COMMENT '加密版本(1=AES-256-GCM)',
    -- 状态与时间
    `status`                       TINYINT        NOT NULL DEFAULT 1       COMMENT '状态: 0-已失效 1-生效中',
    `effective_date`               DATETIME       DEFAULT NULL             COMMENT '生效日期',
    `create_time`                  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                  DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_employee_id` (`employee_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_effective_date` (`effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资账套表';


-- ============================================================
-- 2. 薪资记录表（工资条）
-- ============================================================
DROP TABLE IF EXISTS `salary_record`;
CREATE TABLE `salary_record` (
    `id`                              BIGINT         NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `employee_id`                     BIGINT         NOT NULL                 COMMENT '员工ID',
    `year`                            INT            NOT NULL                 COMMENT '核算年份',
    `month`                           INT            NOT NULL                 COMMENT '核算月份(1-12)',
    -- ────────────── 应发部分（明文，迁移后删除）──────────────
    `basic_salary`                    DECIMAL(12,2)  DEFAULT NULL             COMMENT '应发基本工资（已含试用期折算）',
    `attendance_deduction`            DECIMAL(12,2)  DEFAULT NULL             COMMENT '考勤扣款（迟到/早退/旷工罚款）',
    `leave_deduction`                 DECIMAL(12,2)  DEFAULT NULL             COMMENT '事假扣款',
    `overtime_pay`                    DECIMAL(12,2)  DEFAULT NULL             COMMENT '加班补贴',
    `gross_pay`                       DECIMAL(12,2)  DEFAULT NULL             COMMENT '应发合计',
    -- ────────────── 扣除部分（明文，迁移后删除）──────────────
    `social_insurance_personal`       DECIMAL(12,2)  DEFAULT NULL             COMMENT '社保个人缴纳',
    `housing_fund_personal`           DECIMAL(12,2)  DEFAULT NULL             COMMENT '公积金个人缴纳',
    `taxable_income`                  DECIMAL(12,2)  DEFAULT NULL             COMMENT '应纳税所得额',
    `tax`                             DECIMAL(12,2)  DEFAULT NULL             COMMENT '个人所得税（累计预扣法）',
    `other_deduction`                 DECIMAL(12,2)  DEFAULT NULL             COMMENT '其他扣款',
    -- ────────────── 实发（明文，迁移后删除）──────────────
    `net_pay`                         DECIMAL(12,2)  DEFAULT NULL             COMMENT '实发工资',
    -- ────────────── 应发部分（密文）──────────────
    `basic_salary_encrypted`          VARCHAR(256)   DEFAULT NULL             COMMENT '基本工资(加密)',
    `attendance_deduction_encrypted`  VARCHAR(256)   DEFAULT NULL             COMMENT '考勤扣款(加密)',
    `leave_deduction_encrypted`       VARCHAR(256)   DEFAULT NULL             COMMENT '请假扣款(加密)',
    `overtime_pay_encrypted`          VARCHAR(256)   DEFAULT NULL             COMMENT '加班费(加密)',
    `gross_pay_encrypted`             VARCHAR(256)   DEFAULT NULL             COMMENT '应发工资(加密)',
    -- ────────────── 扣除部分（密文）──────────────
    `social_insurance_personal_encrypted` VARCHAR(256) DEFAULT NULL           COMMENT '社保个人(加密)',
    `housing_fund_personal_encrypted` VARCHAR(256)   DEFAULT NULL             COMMENT '公积金个人(加密)',
    `taxable_income_encrypted`        VARCHAR(256)   DEFAULT NULL             COMMENT '应纳税所得额(加密)',
    `tax_encrypted`                   VARCHAR(256)   DEFAULT NULL             COMMENT '个税(加密)',
    `other_deduction_encrypted`       VARCHAR(256)   DEFAULT NULL             COMMENT '其他扣款(加密)',
    -- ────────────── 实发（密文）──────────────
    `net_pay_encrypted`               VARCHAR(256)   DEFAULT NULL             COMMENT '实发工资(加密)',
    `encryption_version`              TINYINT        NOT NULL DEFAULT 1       COMMENT '加密版本(1=AES-256-GCM)',
    -- ────────────── 统计信息（用于预警）──────────────
    `probation_ratio`                 DECIMAL(4,3)   DEFAULT NULL             COMMENT '试用期薪资比例',
    `leave_days`                      DECIMAL(4,1)   DEFAULT 0.0              COMMENT '当月请假天数合计',
    `overtime_hours`                  DECIMAL(6,2)   DEFAULT 0.00             COMMENT '当月加班小时合计',
    `late_count`                      INT            NOT NULL DEFAULT 0       COMMENT '迟到次数',
    `early_count`                     INT            NOT NULL DEFAULT 0       COMMENT '早退次数',
    `absent_count`                    DECIMAL(3,1)   DEFAULT 0.0              COMMENT '旷工次数（半天=0.5）',
    -- ────────────── 预警与状态 ──────────────
    `warnings`                        VARCHAR(500)   DEFAULT NULL             COMMENT '预警标记（JSON数组）',
    `status`                          VARCHAR(32)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT-草稿 CONFIRMED-已确认 PAID-已发放',
    `create_time`                     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                     DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_employee_year_month` (`employee_id`, `year`, `month`),
    INDEX `idx_year_month` (`year`, `month`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资记录表（工资条）';
