-- ═══════════════════════════════════════════════════
-- Phase 12: 薪资管理后端补全
-- 全幂等：可重复执行，不会因部分已存在而报错
-- ═══════════════════════════════════════════════════

-- ============================================================
-- 1. 薪资账套模板（已存在则跳过）
-- ============================================================
CREATE TABLE IF NOT EXISTS `salary_plan` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `plan_name`   VARCHAR(64)  NOT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `status`      TINYINT      NOT NULL DEFAULT 1,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资账套模板';

CREATE TABLE IF NOT EXISTS `salary_plan_item` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `plan_id`     BIGINT       NOT NULL,
    `item_name`   VARCHAR(64)  NOT NULL,
    `item_code`   VARCHAR(32)  NOT NULL,
    `item_type`   VARCHAR(16)  NOT NULL COMMENT 'INCOME / DEDUCTION',
    `category`    VARCHAR(32)  NOT NULL COMMENT 'FIXED/VARIABLE/ATTENDANCE/SOCIAL/FUND/TAX',
    `calc_rule`   VARCHAR(255) DEFAULT NULL,
    `sort_order`  INT          NOT NULL DEFAULT 0,
    `is_enabled`  TINYINT      NOT NULL DEFAULT 1,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资账套工资项目';

CREATE TABLE IF NOT EXISTS `salary_plan_scope` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `plan_id`        BIGINT       NOT NULL,
    `scope_type`     VARCHAR(16)  NOT NULL COMMENT 'DEPT/POSITION/GRADE/DEFAULT',
    `target_id`      BIGINT       DEFAULT NULL,
    `target_value`   VARCHAR(64)  DEFAULT NULL,
    `priority`       TINYINT      NOT NULL DEFAULT 0,
    `effective_date` DATE         NOT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_plan_id` (`plan_id`),
    INDEX `idx_scope_type_target` (`scope_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资账套适用范围';


-- ============================================================
-- 2. 扩展 salary_account（幂等存储过程）
-- ============================================================
DROP PROCEDURE IF EXISTS `ensure_col`;
DELIMITER //
CREATE PROCEDURE `ensure_col`(IN tbl VARCHAR(64), IN col VARCHAR(64), IN col_def VARCHAR(512))
BEGIN
    SET @cnt = 0;
    SELECT COUNT(*) INTO @cnt FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col;
    IF @cnt = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', col_def);
        PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
END //
DELIMITER ;

CALL `ensure_col`('salary_account', 'plan_id',              'BIGINT       DEFAULT NULL COMMENT ''关联账套ID'' AFTER `employee_id`');
CALL `ensure_col`('salary_account', 'effective_start_date', 'DATE         DEFAULT NULL COMMENT ''生效开始'' AFTER `effective_date`');
CALL `ensure_col`('salary_account', 'effective_end_date',   'DATE         DEFAULT NULL COMMENT ''生效结束'' AFTER `effective_start_date`');
CALL `ensure_col`('salary_account', 'change_reason',        'VARCHAR(255) DEFAULT NULL COMMENT ''调薪原因'' AFTER `effective_end_date`');
CALL `ensure_col`('salary_account', 'operator_id',          'BIGINT       DEFAULT NULL COMMENT ''操作人ID'' AFTER `change_reason`');

-- 索引幂等
SET @idx = 0;
SELECT COUNT(*) INTO @idx FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'salary_account' AND INDEX_NAME = 'idx_employee_effective';
SET @sql = IF(@idx = 0,
    'ALTER TABLE `salary_account` ADD INDEX `idx_employee_effective` (`employee_id`, `effective_start_date`, `effective_end_date`)',
    'SELECT ''idx_employee_effective exists'' AS _');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

DROP PROCEDURE IF EXISTS `ensure_col`;


-- ============================================================
-- 3. 调薪历史表
-- ============================================================
CREATE TABLE IF NOT EXISTS `salary_change_history` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `employee_id`        BIGINT       NOT NULL,
    `account_id`         BIGINT       DEFAULT NULL,
    `change_type`        VARCHAR(16)  NOT NULL COMMENT 'CREATE/ADJUST/DEACTIVATE',
    `field_name`         VARCHAR(32)  DEFAULT NULL,
    `old_value`          VARCHAR(256) DEFAULT NULL COMMENT '密文',
    `new_value`          VARCHAR(256) DEFAULT NULL COMMENT '密文',
    `change_reason`      VARCHAR(255) DEFAULT NULL,
    `source_business`    VARCHAR(32)  DEFAULT NULL COMMENT 'ONBOARD/ADJUST/MANUAL',
    `operator_id`        BIGINT       NOT NULL,
    `encryption_version` TINYINT      NOT NULL DEFAULT 1,
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_employee_id` (`employee_id`),
    INDEX `idx_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调薪历史表';


-- ============================================================
-- 4. 扩展 salary_batch
-- ============================================================
DROP PROCEDURE IF EXISTS `ensure_col2`;
DELIMITER //
CREATE PROCEDURE `ensure_col2`(IN col VARCHAR(64), IN col_def VARCHAR(512))
BEGIN
    SET @cnt = 0;
    SELECT COUNT(*) INTO @cnt FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'salary_batch' AND COLUMN_NAME = col;
    IF @cnt = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `salary_batch` ADD COLUMN `', col, '` ', col_def);
        PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
END //
DELIMITER ;

CALL `ensure_col2`('attendance_locked', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''考勤锁定'' AFTER `total_net_pay`');
CALL `ensure_col2`('lock_time',         'DATETIME DEFAULT NULL           COMMENT ''锁定时间'' AFTER `attendance_locked`');
CALL `ensure_col2`('blocking_reason',   'VARCHAR(500) DEFAULT NULL       COMMENT ''阻断原因'' AFTER `lock_time`');

DROP PROCEDURE IF EXISTS `ensure_col2`;


-- ============================================================
-- 5. 薪资调整项表
-- ============================================================
CREATE TABLE IF NOT EXISTS `salary_adjustment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `batch_id`    BIGINT       NOT NULL,
    `employee_id` BIGINT       NOT NULL,
    `record_id`   BIGINT       DEFAULT NULL,
    `adjust_type` VARCHAR(16)  NOT NULL COMMENT 'INCOME/DEDUCTION',
    `amount`      VARCHAR(256) NOT NULL COMMENT '加密金额',
    `reason`      VARCHAR(255) DEFAULT NULL,
    `operator_id` BIGINT       NOT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_batch_id` (`batch_id`),
    INDEX `idx_employee_id` (`employee_id`),
    INDEX `idx_record_id` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资调整项表';


-- ============================================================
-- 6. 工资条查看审计表
-- ============================================================
CREATE TABLE IF NOT EXISTS `payslip_view_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `employee_id`   BIGINT       NOT NULL,
    `record_id`     BIGINT       NOT NULL,
    `verify_method` VARCHAR(16)  DEFAULT NULL COMMENT 'PASSWORD/SMS/NONE',
    `ip_address`    VARCHAR(45)  DEFAULT NULL,
    `user_agent`    VARCHAR(255) DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_employee_record` (`employee_id`, `record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资条查看审计表';


-- ============================================================
-- 7. RBAC 权限种子（INSERT IGNORE 幂等）
--    已有: 45-47=salary:account*, 48-51=salary:calc*, 67=salary:batch:view, 71-73=salary:plan*
-- ============================================================

INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(71, 44,'薪资方案', 'salary:plan',        2, '/salary/plan', 4, NOW(), NOW()),
(72, 71,'查看',     'salary:plan:view',   3, NULL, 1, NOW(), NOW()),
(73, 71,'管理',     'salary:plan:manage', 3, NULL, 2, NOW(), NOW()),

-- 批次扩展（挂在 salary:calc=48 下，salary:batch:view=67 已存在）
(74, 48,'核算',     'salary:batch:calc',    3, NULL, 5, NOW(), NOW()),
(75, 48,'调整',     'salary:batch:adjust',  3, NULL, 6, NOW(), NOW()),
(76, 48,'提交',     'salary:batch:submit',  3, NULL, 7, NOW(), NOW()),
(77, 48,'发放',     'salary:batch:pay',     3, NULL, 8, NOW(), NOW()),
(78, 48,'归档',     'salary:batch:archive', 3, NULL, 9, NOW(), NOW()),

-- 工资条
(79, 44,'工资条',     'salary:payslip',      2, '/salary/payslip', 6, NOW(), NOW()),
(80, 79,'查看全量',   'salary:payslip:view', 3, NULL, 1, NOW(), NOW()),
(81, 79,'查看本人',   'salary:payslip:self', 3, NULL, 2, NOW(), NOW()),

-- 报表
(82, 44,'薪资报表', 'salary:report',      2, '/salary/report', 7, NOW(), NOW()),
(83, 82,'查看',     'salary:report:view', 3, NULL, 1, NOW(), NOW());


-- ============================================================
-- 8. 角色-权限分配（INSERT IGNORE，已有关联不重复）
-- ============================================================

-- ROLE_ADMIN (1): 全量
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1,71),(1,72),(1,73),(1,74),(1,75),(1,76),(1,77),(1,78),(1,79),(1,80),(1,81),(1,82),(1,83);

-- ROLE_HR (2): 除归档外全部
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2,71),(2,72),(2,73),(2,74),(2,75),(2,76),(2,77),(2,79),(2,80),(2,81),(2,82),(2,83);

-- ROLE_FINANCE (4): 查看+审批+发放+报表
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(4,71),(4,72),(4,77),(4,78),(4,79),(4,80),(4,82),(4,83);

-- ROLE_MANAGER (3): 仅报表
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3,82),(3,83);

-- ROLE_EMPLOYEE (5): 仅本人工资条
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(5,79),(5,81);
