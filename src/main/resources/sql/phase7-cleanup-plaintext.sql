-- ═══════════════════════════════════════════════════
-- Phase 7: 删除明文列 — 清理脚本
-- ═══════════════════════════════════════════════════
-- ⚠️ 执行前提（必须全部满足，否则数据丢失）:
--   1. 已执行 phase5-encryption.sql（加密列已创建）
--   2. 已执行数据加密迁移（DataEncryptionMigrator，所有明文字段已加密写入 *_encrypted 列）
--   3. 已执行数据完整性校验（DataMigrationValidator，随机 5% 抽样全部通过）
--   4. 业务功能已回归验证通过
--   5. 已做好数据库备份！
--   6. 选择低峰期执行（涉及全表 DDL，可能锁表）
--
-- ⚠️ 警告: 本脚本 DROP 明文列后不可逆，请谨慎执行！
-- ═══════════════════════════════════════════════════


-- ============================================================
-- 1. employee 表 — 删除 8 个明文字段
-- ============================================================
ALTER TABLE `employee`
    DROP COLUMN `phone`,
    DROP COLUMN `email`,
    DROP COLUMN `id_card`,
    DROP COLUMN `registered_address`,
    DROP COLUMN `current_address`,
    DROP COLUMN `bank_account`,
    DROP COLUMN `bank_name`,
    DROP COLUMN `base_salary`;

-- 删除原 phone 明文的索引（密文已有 idx_phone_hash）
ALTER TABLE `employee`
    DROP INDEX `idx_phone`;


-- ============================================================
-- 2. onboarding_application 表 — 删除 8 个明文字段
-- ============================================================
ALTER TABLE `onboarding_application`
    DROP COLUMN `phone`,
    DROP COLUMN `email`,
    DROP COLUMN `id_card`,
    DROP COLUMN `registered_address`,
    DROP COLUMN `current_address`,
    DROP COLUMN `bank_account`,
    DROP COLUMN `bank_name`,
    DROP COLUMN `offer_salary`;


-- ============================================================
-- 3. regularization_application 表 — 删除明文字段
-- ============================================================
ALTER TABLE `regularization_application`
    DROP COLUMN `formal_salary`;


-- ============================================================
-- 4. salary_account 表 — 删除 5 个明文金额字段
-- ============================================================
ALTER TABLE `salary_account`
    DROP COLUMN `basic_salary`,
    DROP COLUMN `position_salary`,
    DROP COLUMN `performance_salary`,
    DROP COLUMN `social_insurance_base`,
    DROP COLUMN `housing_fund_base`;


-- ============================================================
-- 5. salary_record 表 — 删除 11 个明文金额字段
-- ============================================================
ALTER TABLE `salary_record`
    DROP COLUMN `basic_salary`,
    DROP COLUMN `attendance_deduction`,
    DROP COLUMN `leave_deduction`,
    DROP COLUMN `overtime_pay`,
    DROP COLUMN `gross_pay`,
    DROP COLUMN `social_insurance_personal`,
    DROP COLUMN `housing_fund_personal`,
    DROP COLUMN `taxable_income`,
    DROP COLUMN `tax`,
    DROP COLUMN `other_deduction`,
    DROP COLUMN `net_pay`;


-- ═══════════════════════════════════════════════════
-- 清理完成: 共删除 5 张表、33 个明文字段
-- ═══════════════════════════════════════════════════
