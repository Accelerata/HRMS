-- ============================================================
-- Phase 13: 剩余问题修复与功能补全
-- 包含: 字段补全、部门合并、审计日志、考勤组扩展、排班制、
--       职级种子补全、新权限码
-- 全幂等：可重复执行，不会因部分已存在而报错
-- ============================================================

-- ── 0. 幂等辅助存储过程 ──
DROP PROCEDURE IF EXISTS `ensure_col_phase13`;
DELIMITER //
CREATE PROCEDURE `ensure_col_phase13`(IN tbl VARCHAR(64), IN col VARCHAR(64), IN col_def VARCHAR(512))
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


-- ── 1. 部门表：新增 description ──
CALL `ensure_col_phase13`('department', 'description', 'VARCHAR(500) NULL COMMENT ''部门描述/职能说明'' AFTER `manager_id`');


-- ── 2. 职位表：新增 dept_id ──
CALL `ensure_col_phase13`('position', 'dept_id', 'BIGINT NULL COMMENT ''所属部门ID（为空=全公司通用）'' AFTER `sequence`');


-- ── 3. 考勤组表：新增适用人员 + 午休 + 阈值字段 ──
CALL `ensure_col_phase13`('attendance_group', 'dept_id',                'BIGINT NULL COMMENT ''适用部门ID'' AFTER `absent_half_day_threshold`');
CALL `ensure_col_phase13`('attendance_group', 'position_id',            'BIGINT NULL COMMENT ''适用职位ID'' AFTER `dept_id`');
CALL `ensure_col_phase13`('attendance_group', 'employee_ids',           'VARCHAR(2000) NULL COMMENT ''适用员工ID列表（JSON数组）'' AFTER `position_id`');
CALL `ensure_col_phase13`('attendance_group', 'lunch_break_start',      'TIME NULL COMMENT ''午休开始时间（默认12:00）'' AFTER `employee_ids`');
CALL `ensure_col_phase13`('attendance_group', 'lunch_break_end',        'TIME NULL COMMENT ''午休结束时间（默认13:00）'' AFTER `lunch_break_start`');
CALL `ensure_col_phase13`('attendance_group', 'late_threshold_minutes', 'INT NULL DEFAULT 15 COMMENT ''迟到阈值（分钟）'' AFTER `lunch_break_end`');
CALL `ensure_col_phase13`('attendance_group', 'early_threshold_minutes','INT NULL DEFAULT 15 COMMENT ''早退阈值（分钟）'' AFTER `late_threshold_minutes`');


-- ── 4. 排班制班次表 ──
DROP TABLE IF EXISTS `shift_schedule`;
CREATE TABLE `shift_schedule` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `group_id` BIGINT NOT NULL COMMENT '考勤组ID',
    `day_of_week` INT NOT NULL COMMENT '星期几（1=周一, 7=周日）',
    `start_time` TIME NOT NULL COMMENT '上班时间',
    `end_time` TIME NOT NULL COMMENT '下班时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_group_id` (`group_id`)
) COMMENT '排班制班次配置';


-- ── 5. 审计日志表 ──
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `operator_id` BIGINT NULL COMMENT '操作人ID（sys_user.id）',
    `operator_name` VARCHAR(64) NULL COMMENT '操作人用户名',
    `operation` VARCHAR(64) NOT NULL COMMENT '操作类型（SALARY_VIEW, CREATE, UPDATE, DELETE, EXPORT等）',
    `resource_type` VARCHAR(64) NULL COMMENT '目标资源类型（EMPLOYEE, PAYSLIP, DEPARTMENT等）',
    `resource_id` VARCHAR(128) NULL COMMENT '目标资源ID',
    `request_summary` VARCHAR(500) NULL COMMENT '请求参数摘要',
    `result` VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果：SUCCESS/FAILURE',
    `error_message` VARCHAR(500) NULL COMMENT '失败时的错误信息',
    `client_ip` VARCHAR(64) NULL COMMENT '客户端IP地址',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX `idx_operator` (`operator_id`),
    INDEX `idx_operation` (`operation`),
    INDEX `idx_resource` (`resource_type`, `resource_id`),
    INDEX `idx_create_time` (`create_time`)
) COMMENT '操作审计日志';


-- ── 6. 薪资账套表：新增 salary_end_date ──
--    使用存储过程安全添加，不依赖 effective_end_date 列位置
CALL `ensure_col_phase13`('salary_account', 'salary_end_date', 'DATE NULL COMMENT ''薪资核算截止日期（离职时标记）'' AFTER `change_reason`');


-- ── 7. 职级对招表（grade 表不存在，先创建） ──
CREATE TABLE IF NOT EXISTS `grade` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sequence`    VARCHAR(8)  NOT NULL COMMENT '序列: M/P/S',
    `code`        VARCHAR(16) NOT NULL COMMENT '职级编码（如 P8）',
    `name`        VARCHAR(64) NOT NULL COMMENT '职级名称（如 P8-资深专家）',
    `level_order` INT         NOT NULL DEFAULT 0 COMMENT '级别排序（数字越大级别越高）',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_grade_code` (`code`),
    INDEX `idx_sequence` (`sequence`),
    INDEX `idx_level_order` (`level_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职级对招表';

-- 种子数据：P8-P10
INSERT IGNORE INTO `grade` (`sequence`, `code`, `name`, `level_order`, `status`) VALUES
('P', 'P8',  'P8-资深专家', 8,  1),
('P', 'P9',  'P9-高级专家', 9,  1),
('P', 'P10', 'P10-首席专家', 10, 1);

-- 种子数据：S4-S5
INSERT IGNORE INTO `grade` (`sequence`, `code`, `name`, `level_order`, `status`) VALUES
('S', 'S4', 'S4-高级职能', 4, 1),
('S', 'S5', 'S5-资深职能', 5, 1);


-- ── 8. 权限码种子数据 ──
--    修正列名：code→permission_code, name→menu_name
--    type: 1=目录 2=菜单 3=按钮/字段控制
--    审计日志查看/导出 → 挂到系统管理(id=1)下
--    部门合并 → 挂到部门管理(id=16)下
--    考勤统计 → 挂到考勤管理(id=29)下

-- 审计日志菜单项（如果不存在则插入）
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`)
VALUES (64, 1, '审计日志', 'sys:audit', 2, '/sys/audit-log', 4, NOW(), NOW());

-- 审计日志操作按钮
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(65, 64, '查看审计日志', 'audit:log:view',   3, NULL, 1, NOW(), NOW()),
(66, 64, '导出审计日志', 'audit:log:export', 3, NULL, 2, NOW(), NOW());

-- 部门合并按钮（挂到部门管理菜单下）
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`)
VALUES (67, 16, '部门合并', 'org:dept:merge', 3, NULL, 3, NOW(), NOW());

-- 考勤统计菜单项
INSERT IGNORE INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`)
VALUES (68, 29, '考勤统计', 'attendance:stats:view', 2, '/att/stats', 5, NOW(), NOW());


-- ── 9. 分配权限给角色 ──
-- 系统管理员：获得全部新权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 64), (1, 65), (1, 66), (1, 67), (1, 68);

-- HR专员：审计日志查看、部门合并、考勤统计
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2, 64), (2, 65), (2, 67), (2, 68);

-- 部门主管：考勤统计（查看本部门）
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3, 68);


-- ── 10. 清理存储过程 ──
DROP PROCEDURE IF EXISTS `ensure_col_phase13`;


-- ── 11. 验证 ──
SELECT 'OK: department.description exists' AS result
WHERE EXISTS (SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'department' AND COLUMN_NAME = 'description');

SELECT 'OK: position.dept_id exists' AS result
WHERE EXISTS (SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'position' AND COLUMN_NAME = 'dept_id');

SELECT 'OK: attendance_group new columns exist' AS result
WHERE EXISTS (SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_group' AND COLUMN_NAME = 'lunch_break_start');

SELECT 'OK: salary_account.salary_end_date exists' AS result
WHERE EXISTS (SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'salary_account' AND COLUMN_NAME = 'salary_end_date');

SELECT 'OK: grade table exists' AS result
WHERE EXISTS (SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grade');

SELECT 'OK: shift_schedule table exists' AS result
WHERE EXISTS (SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shift_schedule');

SELECT 'OK: audit_log table exists' AS result
WHERE EXISTS (SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_log');

SELECT 'OK: new permissions exist' AS result
WHERE EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'audit:log:view');

SELECT 'OK: admin has new permissions' AS result
WHERE EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 68);
