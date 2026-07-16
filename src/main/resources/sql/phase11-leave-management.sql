-- ═══════════════════════════════════════════════════
-- Phase 11: 请假管理补全 — 增量脚本
-- 内容：
--  1. 新建 work_calendar 表（法定节假日/调班工作日）
--  2. 新建 leave_attachment 表（请假附件元数据）
--  3. 新建 comp_leave_grant 与 comp_leave_usage 表（调休明细）
--  4. leave_application 增列 start_period / end_period
--  5. RBAC 种子（工作日历权限码）
-- ═══════════════════════════════════════════════════

-- ============================================================
-- 1. 工作日历表（法定节假日/调班工作日）
-- ============================================================
DROP TABLE IF EXISTS `work_calendar`;
CREATE TABLE `work_calendar` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `calendar_date` DATE        NOT NULL,
    `day_type`      TINYINT     NOT NULL  COMMENT '1-法定节假日/休息 2-调班工作日',
    `name`          VARCHAR(64) DEFAULT NULL COMMENT '节日/调班说明',
    `year`          INT         NOT NULL,
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME    DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_calendar_date` (`calendar_date`),
    KEY `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作日历（法定节假日/调班）';


-- ============================================================
-- 2. 请假附件表
-- ============================================================
DROP TABLE IF EXISTS `leave_attachment`;
CREATE TABLE `leave_attachment` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT       DEFAULT NULL COMMENT '关联 leave_application.id（绑定前为 NULL）',
    `file_name`      VARCHAR(255) NOT NULL,
    `object_key`     VARCHAR(255) NOT NULL COMMENT 'OSS objectName',
    `file_url`       VARCHAR(512) NOT NULL,
    `file_size`      BIGINT       DEFAULT NULL,
    `content_type`   VARCHAR(64)  DEFAULT NULL,
    `upload_by`      BIGINT       NOT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假附件表';


-- ============================================================
-- 3. 调休入账明细表 + 调休占用明细表
-- ============================================================
DROP TABLE IF EXISTS `comp_leave_grant`;
CREATE TABLE `comp_leave_grant` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `employee_id`    BIGINT       NOT NULL,
    `overtime_month` CHAR(7)      NOT NULL COMMENT '加班所属月 yyyy-MM',
    `days`           DECIMAL(4,1) NOT NULL COMMENT '本次折算入账天数',
    `used_days`      DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    `expire_date`    DATE         NOT NULL COMMENT '过期日（加班次月月末）',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '1-有效 0-已过期清零',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_employee_status_expire` (`employee_id`,`status`,`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调休入账明细表';

DROP TABLE IF EXISTS `comp_leave_usage`;
CREATE TABLE `comp_leave_usage` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT       NOT NULL COMMENT '关联 leave_application.id',
    `grant_id`       BIGINT       NOT NULL COMMENT '关联 comp_leave_grant.id',
    `days`           DECIMAL(4,1) NOT NULL COMMENT '从该 grant 扣减的天数',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_grant_id` (`grant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调休占用明细表';


-- ============================================================
-- 4. leave_application 增列：start_period / end_period
-- ============================================================
ALTER TABLE `leave_application`
    ADD COLUMN `start_period` TINYINT NOT NULL DEFAULT 0 COMMENT '开始时段: 0-上午 1-下午' AFTER `end_date`,
    ADD COLUMN `end_period`   TINYINT NOT NULL DEFAULT 1 COMMENT '结束时段: 0-上午 1-下午' AFTER `start_period`;


-- ============================================================
-- 5. RBAC 种子 — 工作日历权限码
-- ============================================================

-- 5.1 工作日历菜单 + 管理权限（挂在考勤管理 att=29 下）
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(69, 29,'工作日历', 'att:calendar',        2, '/att/calendar', 6, NOW(), NOW()),
(70, 69,'管理',     'att:calendar:manage', 3, NULL, 1, NOW(), NOW());

-- ROLE_ADMIN (id=1)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 69), (1, 70);

-- ROLE_HR (id=2)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2, 69), (2, 70);
