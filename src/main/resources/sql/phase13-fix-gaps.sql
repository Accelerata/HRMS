-- ═══════════════════════════════════════════════════
-- Phase 13: 补漏 — 入转调离权限 + 职级薪资带宽表
-- 说明：phase9 部分未执行，补回缺失内容
-- ═══════════════════════════════════════════════════

-- ============================================================
-- 1. 职级薪资带宽表（phase9 漏建）
-- ============================================================
CREATE TABLE IF NOT EXISTS `grade_salary_range` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT,
    `grade_code`  VARCHAR(16)    NOT NULL,
    `min_salary`  DECIMAL(12,2)  NOT NULL,
    `max_salary`  DECIMAL(12,2)  NOT NULL,
    `status`      TINYINT        NOT NULL DEFAULT 1,
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_grade_code` (`grade_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职级薪资范围表';

INSERT IGNORE INTO `grade_salary_range` (`grade_code`, `min_salary`, `max_salary`) VALUES
('P1', 4000.00,  8000.00),
('P2', 6000.00,  12000.00),
('P3', 9000.00,  18000.00),
('P4', 13000.00, 25000.00),
('P5', 18000.00, 35000.00),
('P6', 25000.00, 45000.00),
('P7', 35000.00, 60000.00),
('M1', 8000.00,  15000.00),
('M2', 12000.00, 22000.00),
('M3', 18000.00, 32000.00),
('M4', 25000.00, 45000.00),
('M5', 35000.00, 60000.00),
('S1', 3000.00,  6000.00),
('S2', 4000.00,  8000.00),
('S3', 5000.00,  10000.00);


-- ============================================================
-- 2. position 表 is_standard 列（phase9 漏加）
-- ============================================================
DROP PROCEDURE IF EXISTS `ensure_col3`;
DELIMITER //
CREATE PROCEDURE `ensure_col3`(IN col VARCHAR(64), IN col_def VARCHAR(512))
BEGIN
    SET @cnt = 0;
    SELECT COUNT(*) INTO @cnt FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'position' AND COLUMN_NAME = col;
    IF @cnt = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `position` ADD COLUMN `', col, '` ', col_def);
        PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
END //
DELIMITER ;

CALL `ensure_col3`('is_standard', 'TINYINT NOT NULL DEFAULT 1 COMMENT ''是否标准职位: 1-标准 0-非标准'' AFTER `description`');

DROP PROCEDURE IF EXISTS `ensure_col3`;


-- ============================================================
-- 3. 入转调离权限码（phase9 漏插，ID 59-63）
--    控制器 OnboardingController → onboarding:manage
--           RegularizationController → regularization:manage
--           TransferController → transfer:manage
--           ResignationController → resignation:manage
-- ============================================================
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(59, 0, '入转调离', 'hrlifecycle',            1, NULL,                         7, NOW(), NOW()),
(60, 59,'入职管理', 'onboarding:manage',       2, '/lifecycle/onboarding',      1, NOW(), NOW()),
(61, 59,'转正管理', 'regularization:manage',   2, '/lifecycle/regularization',  2, NOW(), NOW()),
(62, 59,'调岗管理', 'transfer:manage',         2, '/lifecycle/transfer',        3, NOW(), NOW()),
(63, 59,'离职管理', 'resignation:manage',      2, '/lifecycle/resignation',     4, NOW(), NOW());


-- ============================================================
-- 4. 角色分配入转调离权限
--    ROLE_ADMIN(1) + ROLE_HR(2) + ROLE_MANAGER(3) 均需此权限
--    （主管需要审批下属入转调离）
-- ============================================================
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 59), (1, 60), (1, 61), (1, 62), (1, 63),
(2, 59), (2, 60), (2, 61), (2, 62), (2, 63),
(3, 59), (3, 60), (3, 61), (3, 62), (3, 63);


-- ============================================================
-- 5. 验证：确认所有控制器权限码都有对应记录
-- ============================================================
-- 以下 SELECT 应全部返回一行 'OK'
SELECT 'OK: hrlifecycle perms exist' AS result
WHERE EXISTS (SELECT 1 FROM sys_permission WHERE id = 59);
SELECT 'OK: onboarding:manage assigned' AS result
WHERE EXISTS (SELECT 1 FROM sys_role_permission WHERE permission_id = 60 AND role_id = 1);
