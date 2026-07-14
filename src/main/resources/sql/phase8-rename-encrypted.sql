-- ═══════════════════════════════════════════════════
-- Phase 8: 加密列重命名为原列名
-- ═══════════════════════════════════════════════════
-- 执行前提:
--   1. 已执行 phase7-cleanup-plaintext.sql（明文字段已删除，列名已释放）
--   2. 数据加密迁移已完成并验证通过
--   3. 应用代码已更新为新的列名映射（本脚本需与代码同步部署）
--
-- 目的: 将 *_encrypted 列重命名为原列名，让数据库列名与实体属性名一致
--       后续新增记录无需区分"明文列"和"密文列"，彻底完成加密切换
-- ═══════════════════════════════════════════════════


-- ============================================================
-- 1. employee 表 — 重命名 8 个加密列
-- ============================================================
ALTER TABLE `employee`
    CHANGE COLUMN `phone_encrypted`               `phone`               VARCHAR(512)  DEFAULT NULL COMMENT '手机号(AES-256-GCM加密)',
    CHANGE COLUMN `email_encrypted`               `email`               VARCHAR(512)  DEFAULT NULL COMMENT '邮箱(AES-256-GCM加密)',
    CHANGE COLUMN `id_card_encrypted`             `id_card`             VARCHAR(512)  DEFAULT NULL COMMENT '身份证号(AES-256-GCM加密)',
    CHANGE COLUMN `registered_address_encrypted`  `registered_address`  VARCHAR(512)  DEFAULT NULL COMMENT '户籍地址(AES-256-GCM加密)',
    CHANGE COLUMN `current_address_encrypted`     `current_address`     VARCHAR(512)  DEFAULT NULL COMMENT '现居住地址(AES-256-GCM加密)',
    CHANGE COLUMN `bank_account_encrypted`        `bank_account`        VARCHAR(512)  DEFAULT NULL COMMENT '银行账号(AES-256-GCM加密)',
    CHANGE COLUMN `bank_name_encrypted`           `bank_name`           VARCHAR(512)  DEFAULT NULL COMMENT '开户行(AES-256-GCM加密)',
    CHANGE COLUMN `base_salary_encrypted`         `base_salary`         VARCHAR(256)  DEFAULT NULL COMMENT '基本工资(AES-256-GCM加密)';


-- ============================================================
-- 2. onboarding_application 表 — 重命名 8 个加密列
-- ============================================================
ALTER TABLE `onboarding_application`
    CHANGE COLUMN `phone_encrypted`               `phone`               VARCHAR(512)  DEFAULT NULL COMMENT '手机号(AES-256-GCM加密)',
    CHANGE COLUMN `email_encrypted`               `email`               VARCHAR(512)  DEFAULT NULL COMMENT '邮箱(AES-256-GCM加密)',
    CHANGE COLUMN `id_card_encrypted`             `id_card`             VARCHAR(512)  DEFAULT NULL COMMENT '身份证号(AES-256-GCM加密)',
    CHANGE COLUMN `registered_address_encrypted`  `registered_address`  VARCHAR(512)  DEFAULT NULL COMMENT '户籍地址(AES-256-GCM加密)',
    CHANGE COLUMN `current_address_encrypted`     `current_address`     VARCHAR(512)  DEFAULT NULL COMMENT '现居住地址(AES-256-GCM加密)',
    CHANGE COLUMN `bank_account_encrypted`        `bank_account`        VARCHAR(512)  DEFAULT NULL COMMENT '银行账号(AES-256-GCM加密)',
    CHANGE COLUMN `bank_name_encrypted`           `bank_name`           VARCHAR(512)  DEFAULT NULL COMMENT '开户行(AES-256-GCM加密)',
    CHANGE COLUMN `offer_salary_encrypted`        `offer_salary`        VARCHAR(256)  DEFAULT NULL COMMENT '入职薪酬(AES-256-GCM加密)';


-- ============================================================
-- 3. regularization_application 表 — 重命名加密列
-- ============================================================
ALTER TABLE `regularization_application`
    CHANGE COLUMN `formal_salary_encrypted` `formal_salary` VARCHAR(256) DEFAULT NULL COMMENT '转正后薪资(AES-256-GCM加密)';


-- ============================================================
-- 4. salary_account 表 — 重命名 5 个加密金额列
-- ============================================================
ALTER TABLE `salary_account`
    CHANGE COLUMN `basic_salary_encrypted`               `basic_salary`               VARCHAR(256) DEFAULT NULL COMMENT '基本工资(AES-256-GCM加密)',
    CHANGE COLUMN `position_salary_encrypted`            `position_salary`            VARCHAR(256) DEFAULT NULL COMMENT '岗位工资(AES-256-GCM加密)',
    CHANGE COLUMN `performance_salary_encrypted`         `performance_salary`         VARCHAR(256) DEFAULT NULL COMMENT '绩效工资(AES-256-GCM加密)',
    CHANGE COLUMN `social_insurance_base_encrypted`      `social_insurance_base`      VARCHAR(256) DEFAULT NULL COMMENT '社保缴纳基数(AES-256-GCM加密)',
    CHANGE COLUMN `housing_fund_base_encrypted`          `housing_fund_base`          VARCHAR(256) DEFAULT NULL COMMENT '公积金缴纳基数(AES-256-GCM加密)';


-- ============================================================
-- 5. salary_record 表 — 重命名 11 个加密金额列
-- ============================================================
ALTER TABLE `salary_record`
    CHANGE COLUMN `basic_salary_encrypted`                    `basic_salary`                    VARCHAR(256) DEFAULT NULL COMMENT '基本工资(AES-256-GCM加密)',
    CHANGE COLUMN `attendance_deduction_encrypted`            `attendance_deduction`            VARCHAR(256) DEFAULT NULL COMMENT '考勤扣款(AES-256-GCM加密)',
    CHANGE COLUMN `leave_deduction_encrypted`                 `leave_deduction`                 VARCHAR(256) DEFAULT NULL COMMENT '请加扣款(AES-256-GCM加密)',
    CHANGE COLUMN `overtime_pay_encrypted`                    `overtime_pay`                    VARCHAR(256) DEFAULT NULL COMMENT '加班费(AES-256-GCM加密)',
    CHANGE COLUMN `gross_pay_encrypted`                       `gross_pay`                       VARCHAR(256) DEFAULT NULL COMMENT '应发工资(AES-256-GCM加密)',
    CHANGE COLUMN `social_insurance_personal_encrypted`       `social_insurance_personal`       VARCHAR(256) DEFAULT NULL COMMENT '社保个人(AES-256-GCM加密)',
    CHANGE COLUMN `housing_fund_personal_encrypted`           `housing_fund_personal`           VARCHAR(256) DEFAULT NULL COMMENT '公积金个人(AES-256-GCM加密)',
    CHANGE COLUMN `taxable_income_encrypted`                  `taxable_income`                  VARCHAR(256) DEFAULT NULL COMMENT '应纳税所得额(AES-256-GCM加密)',
    CHANGE COLUMN `tax_encrypted`                             `tax`                             VARCHAR(256) DEFAULT NULL COMMENT '个税(AES-256-GCM加密)',
    CHANGE COLUMN `other_deduction_encrypted`                 `other_deduction`                 VARCHAR(256) DEFAULT NULL COMMENT '其他扣款(AES-256-GCM加密)',
    CHANGE COLUMN `net_pay_encrypted`                         `net_pay`                         VARCHAR(256) DEFAULT NULL COMMENT '实发工资(AES-256-GCM加密)';


-- ═══════════════════════════════════════════════════
-- 重命名完成: 5 张表、33 个加密列已改为原名
-- 列类型: 统一 VARCHAR(256/512)，存储 Base64 密文
-- 列注释: 已标明 AES-256-GCM 加密，便于后续维护
-- ═══════════════════════════════════════════════════
