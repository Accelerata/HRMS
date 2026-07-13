-- ═══════════════════════════════════════════════════
-- Phase 4: 员工表扩展 + 合同表 — 迁移/建表脚本
-- ═══════════════════════════════════════════════════
-- ⚠️ 重要: 执行前请先备份数据！status=3 的含义从"离职"变为"待离职"


-- ============================================================
-- 1. 扩展 employee 表（新增 14 个字段）
-- ============================================================

-- 个人信息
ALTER TABLE `employee`
    ADD COLUMN `gender`              TINYINT       DEFAULT NULL COMMENT '性别: 0-未知 1-男 2-女' AFTER `name`,
    ADD COLUMN `phone`               VARCHAR(20)   DEFAULT NULL COMMENT '手机号' AFTER `gender`,
    ADD COLUMN `email`               VARCHAR(128)  DEFAULT NULL COMMENT '邮箱' AFTER `phone`,
    ADD COLUMN `id_card`             VARCHAR(18)   DEFAULT NULL COMMENT '身份证号' AFTER `email`,
    ADD COLUMN `birthday`            DATE          DEFAULT NULL COMMENT '生日（可从身份证号自动提取）' AFTER `id_card`,
    ADD COLUMN `registered_address`  VARCHAR(255)  DEFAULT NULL COMMENT '户籍地址' AFTER `birthday`,
    ADD COLUMN `current_address`     VARCHAR(255)  DEFAULT NULL COMMENT '现居住地址' AFTER `registered_address`,

-- 工作信息
    ADD COLUMN `grade`               VARCHAR(16)   DEFAULT NULL COMMENT '职级（如 P3、M5）' AFTER `position_id`,
    ADD COLUMN `report_to`           BIGINT        DEFAULT NULL COMMENT '直接汇报人ID（关联 employee.id）' AFTER `grade`,
    ADD COLUMN `work_location`       VARCHAR(128)  DEFAULT NULL COMMENT '工作地点' AFTER `report_to`,
    ADD COLUMN `entry_type`          TINYINT       DEFAULT NULL COMMENT '入职类型: 1-社招 2-校招 3-内推 4-调动' AFTER `work_location`,

-- 薪资信息
    ADD COLUMN `salary_account_id`   BIGINT        DEFAULT NULL COMMENT '薪资账套ID' AFTER `entry_type`,
    ADD COLUMN `base_salary`         DECIMAL(12,2) DEFAULT NULL COMMENT '基本工资（精确到分）' AFTER `salary_account_id`,
    ADD COLUMN `bank_account`        VARCHAR(32)   DEFAULT NULL COMMENT '银行账号' AFTER `base_salary`,
    ADD COLUMN `bank_name`           VARCHAR(64)   DEFAULT NULL COMMENT '开户行' AFTER `bank_account`;

-- 更新 status 字段注释
ALTER TABLE `employee`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '员工状态: 0-待入职 1-试用期 2-正式 3-待离职 4-已离职';

-- 新增索引
ALTER TABLE `employee`
    ADD INDEX `idx_phone` (`phone`),
    ADD INDEX `idx_grade` (`grade`),
    ADD INDEX `idx_report_to` (`report_to`),
    ADD INDEX `idx_entry_type` (`entry_type`),
    ADD INDEX `idx_salary_account_id` (`salary_account_id`);


-- ============================================================
-- 2. 数据迁移: 将旧 status=3(离职) 的数据改为 status=4(已离职)
--    (因为新枚举中 3=待离职, 4=已离职)
-- ⚠️ 执行前确认: 如果数据库中 status=3 的历史数据确实是"已离职"，
--    请取消下面注释后执行
-- ============================================================
-- UPDATE `employee` SET `status` = 4 WHERE `status` = 3 AND `resign_date` IS NOT NULL;


-- ============================================================
-- 3. 创建员工合同表
-- ============================================================
CREATE TABLE IF NOT EXISTS `employee_contract` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `employee_id`              BIGINT       NOT NULL                 COMMENT '员工ID',
    `contract_type`            TINYINT      NOT NULL                 COMMENT '合同类型: 1-固定期限 2-无固定期限 3-劳务合同',
    `contract_start_date`      DATE         NOT NULL                 COMMENT '合同开始日期',
    `contract_end_date`        DATE         DEFAULT NULL             COMMENT '合同到期日（无固定期限为空）',
    `probation_salary_ratio`   TINYINT      NOT NULL DEFAULT 80      COMMENT '试用期待遇比例（80-100）',
    `status`                   TINYINT      NOT NULL DEFAULT 1       COMMENT '状态: 0-已终止 1-生效中',
    `create_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`              DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_employee_id` (`employee_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工合同表';


-- ============================================================
-- 4. 扩展 onboarding_application 表（入职申请中补充员工扩展信息）
-- ============================================================
ALTER TABLE `onboarding_application`
    ADD COLUMN `gender`               TINYINT       DEFAULT NULL COMMENT '性别: 0-未知 1-男 2-女' AFTER `id_card`,
    ADD COLUMN `registered_address`   VARCHAR(255)  DEFAULT NULL COMMENT '户籍地址' AFTER `gender`,
    ADD COLUMN `current_address`      VARCHAR(255)  DEFAULT NULL COMMENT '现居住地址' AFTER `registered_address`,
    ADD COLUMN `grade`                VARCHAR(16)   DEFAULT NULL COMMENT '职级' AFTER `probation_months`,
    ADD COLUMN `report_to`            BIGINT        DEFAULT NULL COMMENT '直接汇报人ID' AFTER `grade`,
    ADD COLUMN `work_location`        VARCHAR(128)  DEFAULT NULL COMMENT '工作地点' AFTER `report_to`,
    ADD COLUMN `entry_type`           TINYINT       DEFAULT 1    COMMENT '入职类型: 1-社招 2-校招 3-内推 4-调动' AFTER `work_location`,
    ADD COLUMN `salary_account_id`    BIGINT        DEFAULT NULL COMMENT '薪资账套ID' AFTER `entry_type`,
    ADD COLUMN `bank_account`         VARCHAR(32)   DEFAULT NULL COMMENT '银行账号' AFTER `salary_account_id`,
    ADD COLUMN `bank_name`            VARCHAR(64)   DEFAULT NULL COMMENT '开户行' AFTER `bank_account`;
