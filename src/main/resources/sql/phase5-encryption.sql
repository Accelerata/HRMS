-- ═══════════════════════════════════════════════════
-- Phase 5: 敏感数据加密 — 添加加密列和哈希索引列
-- ═══════════════════════════════════════════════════
-- 执行前提: 有可用的加密密钥 (HRMS_ENCRYPTION_KEY 环境变量)
-- 说明: 本脚本为已有表添加加密列，不删除原明文字段（迁移完成并验证后再删除）
-- 注意: salary_account 和 salary_record 表已在 phase6-salary.sql 中直接建表（含加密列），无需在此处 ALTER


-- ============================================================
-- 1. employee 表 — 新增加密列
-- ============================================================
ALTER TABLE `employee`
    -- 手机号（需要精确查询 → 哈希索引）
    ADD COLUMN `phone_encrypted`        VARCHAR(512) DEFAULT NULL COMMENT '手机号(加密)' AFTER `phone`,
    ADD COLUMN `phone_hash`             CHAR(64)     DEFAULT NULL COMMENT '手机号哈希(用于精确查询)' AFTER `phone_encrypted`,
    -- 邮箱
    ADD COLUMN `email_encrypted`        VARCHAR(512) DEFAULT NULL COMMENT '邮箱(加密)' AFTER `email`,
    -- 身份证号（需要精确查询 → 哈希索引）
    ADD COLUMN `id_card_encrypted`      VARCHAR(512) DEFAULT NULL COMMENT '身份证号(加密)' AFTER `id_card`,
    ADD COLUMN `id_card_hash`           CHAR(64)     DEFAULT NULL COMMENT '身份证号哈希(用于查重)' AFTER `id_card_encrypted`,
    -- 户籍地址
    ADD COLUMN `registered_address_encrypted` VARCHAR(512) DEFAULT NULL COMMENT '户籍地址(加密)' AFTER `registered_address`,
    -- 现居住地址
    ADD COLUMN `current_address_encrypted`    VARCHAR(512) DEFAULT NULL COMMENT '现居住地址(加密)' AFTER `current_address`,
    -- 银行账号（需要唯一性校验 → 哈希索引）
    ADD COLUMN `bank_account_encrypted` VARCHAR(512) DEFAULT NULL COMMENT '银行账号(加密)' AFTER `bank_account`,
    ADD COLUMN `bank_account_hash`      CHAR(64)     DEFAULT NULL COMMENT '银行账号哈希(用于唯一性校验)' AFTER `bank_account_encrypted`,
    -- 开户行
    ADD COLUMN `bank_name_encrypted`    VARCHAR(512) DEFAULT NULL COMMENT '开户行(加密)' AFTER `bank_name`,
    -- 基本工资
    ADD COLUMN `base_salary_encrypted`  VARCHAR(256) DEFAULT NULL COMMENT '基本工资(加密)' AFTER `base_salary`,
    -- 加密版本号（支持密钥轮换）
    ADD COLUMN `encryption_version`     TINYINT      NOT NULL DEFAULT 1 COMMENT '加密版本(1=AES-256-GCM)' AFTER `base_salary_encrypted`;

-- 为哈希索引列添加索引（加速精确查询）
ALTER TABLE `employee`
    ADD INDEX `idx_phone_hash` (`phone_hash`),
    ADD INDEX `idx_id_card_hash` (`id_card_hash`),
    ADD INDEX `idx_bank_account_hash` (`bank_account_hash`);


-- ============================================================
-- 2. onboarding_application 表 — 新增加密列
-- ============================================================
ALTER TABLE `onboarding_application`
    ADD COLUMN `phone_encrypted`             VARCHAR(512) DEFAULT NULL COMMENT '手机号(加密)' AFTER `phone`,
    ADD COLUMN `phone_hash`                  CHAR(64)     DEFAULT NULL COMMENT '手机号哈希' AFTER `phone_encrypted`,
    ADD COLUMN `email_encrypted`             VARCHAR(512) DEFAULT NULL COMMENT '邮箱(加密)' AFTER `email`,
    ADD COLUMN `id_card_encrypted`           VARCHAR(512) DEFAULT NULL COMMENT '身份证号(加密)' AFTER `id_card`,
    ADD COLUMN `id_card_hash`                CHAR(64)     DEFAULT NULL COMMENT '身份证号哈希' AFTER `id_card_encrypted`,
    ADD COLUMN `registered_address_encrypted` VARCHAR(512) DEFAULT NULL COMMENT '户籍地址(加密)' AFTER `registered_address`,
    ADD COLUMN `current_address_encrypted`   VARCHAR(512) DEFAULT NULL COMMENT '现居住地址(加密)' AFTER `current_address`,
    ADD COLUMN `bank_account_encrypted`      VARCHAR(512) DEFAULT NULL COMMENT '银行账号(加密)' AFTER `bank_account`,
    ADD COLUMN `bank_account_hash`           CHAR(64)     DEFAULT NULL COMMENT '银行账号哈希' AFTER `bank_account_encrypted`,
    ADD COLUMN `bank_name_encrypted`         VARCHAR(512) DEFAULT NULL COMMENT '开户行(加密)' AFTER `bank_name`,
    ADD COLUMN `offer_salary_encrypted`      VARCHAR(256) DEFAULT NULL COMMENT '入职薪酬(加密)' AFTER `offer_salary`,
    ADD COLUMN `encryption_version`          TINYINT      NOT NULL DEFAULT 1 COMMENT '加密版本';

ALTER TABLE `onboarding_application`
    ADD INDEX `idx_oa_phone_hash` (`phone_hash`),
    ADD INDEX `idx_oa_id_card_hash` (`id_card_hash`);


-- ============================================================
-- 3. regularization_application 表 — 新增加密列
-- ============================================================
ALTER TABLE `regularization_application`
    ADD COLUMN `formal_salary_encrypted` VARCHAR(256) DEFAULT NULL COMMENT '转正薪资(加密)' AFTER `formal_salary`,
    ADD COLUMN `encryption_version`      TINYINT      NOT NULL DEFAULT 1 COMMENT '加密版本';
