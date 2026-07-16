-- ═══════════════════════════════════════════════════
-- Phase 1: 组织架构与考勤休假 — 建表脚本
-- ═══════════════════════════════════════════════════
-- 依赖关系: 无外部依赖（employee.user_id 仅逻辑关联 sys_user）
-- 执行顺序: 本脚本应在 phase2-rbac.sql 之前执行


-- ============================================================
-- 1. 部门表
-- ============================================================
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `parent_id`       BIGINT       NOT NULL DEFAULT 0       COMMENT '上级部门ID（0表示根部门）',
    `dept_name`       VARCHAR(64)  NOT NULL                 COMMENT '部门名称',
    `dept_code`       VARCHAR(32)  NOT NULL                 COMMENT '部门编码',
    `sort_order`      INT          NOT NULL DEFAULT 0       COMMENT '排序号',
    `level`           TINYINT      NOT NULL DEFAULT 1       COMMENT '层级深度（1-5）',
    `status`          TINYINT      NOT NULL DEFAULT 1       COMMENT '状态: 0-禁用 1-正常',
    `manager_id`      BIGINT       DEFAULT NULL             COMMENT '负责人ID（关联 employee.id）',
    `description`     VARCHAR(500) DEFAULT NULL             COMMENT '部门描述/职能说明',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_dept_code` (`dept_code`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_level` (`level`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';


-- ============================================================
-- 2. 职位表（position 是 MySQL 保留字，使用反引号）
-- ============================================================
DROP TABLE IF EXISTS `position`;
CREATE TABLE `position` (
    `id`                         BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `position_name`              VARCHAR(64)  NOT NULL                 COMMENT '职位名称',
    `position_code`              VARCHAR(32)  NOT NULL                 COMMENT '职位编码',
    `sequence`                   VARCHAR(8)   NOT NULL                 COMMENT '职位序列: M-管理 P-专业 S-支持',
    `dept_id`                    BIGINT       DEFAULT NULL             COMMENT '所属部门ID（为空=全公司通用）',
    `grade_range`                VARCHAR(16)  DEFAULT NULL             COMMENT '职级范围（如 P1-P5）',
    `default_probation_months`   TINYINT      NOT NULL DEFAULT 3       COMMENT '默认试用期（月）',
    `description`                VARCHAR(500) DEFAULT NULL             COMMENT '职位描述',
    `is_standard`                TINYINT      NOT NULL DEFAULT 1       COMMENT '是否标准职位: 1-标准 0-非标准',
    `status`                     TINYINT      NOT NULL DEFAULT 1       COMMENT '状态: 0-禁用 1-正常',
    `create_time`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_position_code` (`position_code`),
    INDEX `idx_sequence` (`sequence`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位表';


-- ============================================================
-- 3. 考勤组表
-- ============================================================
DROP TABLE IF EXISTS `attendance_group`;
CREATE TABLE `attendance_group` (
    `id`                          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `group_name`                  VARCHAR(64)  NOT NULL                 COMMENT '考勤组名称',
    `group_type`                  TINYINT      NOT NULL DEFAULT 1       COMMENT '类型: 1-固定班 2-弹性班',
    `start_time`                  TIME         NOT NULL                 COMMENT '规定上班时间（如 09:00:00）',
    `end_time`                    TIME         NOT NULL                 COMMENT '规定下班时间（如 18:00:00）',
    `flex_threshold`              INT          NOT NULL DEFAULT 0       COMMENT '弹性阈值（分钟），固定班为迟到宽限，弹性班为弹性窗口',
    `absent_half_day_threshold`   INT          NOT NULL DEFAULT 120     COMMENT '半天旷工阈值（分钟），迟到/早退超过此值视为旷工半天',
    `create_time`                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                 DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_group_type` (`group_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组表';


-- ============================================================
-- 4. 员工表
-- ============================================================
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
    `id`                  BIGINT         NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `employee_no`         VARCHAR(32)    NOT NULL                 COMMENT '员工工号（系统自动生成，如 HR20240001）',
    `name`                VARCHAR(32)    NOT NULL                 COMMENT '姓名',
    -- 个人信息
    `gender`              TINYINT        DEFAULT NULL             COMMENT '性别: 0-未知 1-男 2-女',
    `phone`               VARCHAR(20)    DEFAULT NULL             COMMENT '手机号',
    `email`               VARCHAR(128)   DEFAULT NULL             COMMENT '邮箱',
    `id_card`             VARCHAR(18)    DEFAULT NULL             COMMENT '身份证号',
    `birthday`            DATE           DEFAULT NULL             COMMENT '生日（可从身份证号自动提取）',
    `registered_address`  VARCHAR(255)   DEFAULT NULL             COMMENT '户籍地址',
    `current_address`     VARCHAR(255)   DEFAULT NULL             COMMENT '现居住地址',
    -- 部门与职位
    `dept_id`             BIGINT         NOT NULL                 COMMENT '所属部门ID',
    `position_id`         BIGINT         NOT NULL                 COMMENT '职位ID',
    `grade`               VARCHAR(16)    DEFAULT NULL             COMMENT '职级（如 P3、M5）',
    -- 工作信息
    `report_to`           BIGINT         DEFAULT NULL             COMMENT '直接汇报人ID（关联 employee.id）',
    `work_location`       VARCHAR(128)   DEFAULT NULL             COMMENT '工作地点',
    `entry_type`          TINYINT        DEFAULT NULL             COMMENT '入职类型: 1-社招 2-校招 3-内推 4-调动',
    -- 薪资信息
    `salary_account_id`   BIGINT         DEFAULT NULL             COMMENT '薪资账套ID',
    `base_salary`         DECIMAL(12,2)  DEFAULT NULL             COMMENT '基本工资（精确到分）',
    `bank_account`        VARCHAR(32)    DEFAULT NULL             COMMENT '银行账号',
    `bank_name`           VARCHAR(64)    DEFAULT NULL             COMMENT '开户行',
    -- 状态与时间
    `status`              TINYINT        NOT NULL DEFAULT 0       COMMENT '员工状态: 0-待入职 1-试用期 2-正式 3-待离职 4-已离职',
    `entry_date`          DATE           DEFAULT NULL             COMMENT '入职日期',
    `regular_date`        DATE           DEFAULT NULL             COMMENT '转正日期',
    `resign_date`         DATE           DEFAULT NULL             COMMENT '离职日期',
    `user_id`             BIGINT         DEFAULT NULL             COMMENT '关联系统用户ID（sys_user.id，入职审批通过后回填）',
    `create_time`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_employee_no` (`employee_no`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_position_id` (`position_id`),
    INDEX `idx_phone` (`phone`),
    INDEX `idx_grade` (`grade`),
    INDEX `idx_report_to` (`report_to`),
    INDEX `idx_entry_type` (`entry_type`),
    INDEX `idx_salary_account_id` (`salary_account_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_entry_date` (`entry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';


-- ============================================================
-- 5. 员工合同表
-- ============================================================
DROP TABLE IF EXISTS `employee_contract`;
CREATE TABLE `employee_contract` (
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
-- 6. 打卡记录表
-- ============================================================
DROP TABLE IF EXISTS `attendance_record`;
CREATE TABLE `attendance_record` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `employee_id`       BIGINT       NOT NULL                 COMMENT '员工ID',
    `group_id`          BIGINT       NOT NULL                 COMMENT '考勤组ID',
    `attendance_date`   DATE         NOT NULL                 COMMENT '打卡日期',
    `punch_in_time`     TIME         DEFAULT NULL             COMMENT '上班打卡时间',
    `punch_out_time`    TIME         DEFAULT NULL             COMMENT '下班打卡时间',
    `punch_in_status`   VARCHAR(32)  NOT NULL DEFAULT 'NORMAL' COMMENT '上班打卡状态: NORMAL/LATE/MISSING_PUNCH/ABSENT_HALF_DAY',
    `punch_out_status`  VARCHAR(32)  DEFAULT NULL             COMMENT '下班打卡状态: NORMAL/EARLY/MISSING_PUNCH/ABSENT_HALF_DAY（上班打卡时为空）',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_employee_date` (`employee_id`, `attendance_date`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_attendance_date` (`attendance_date`),
    INDEX `idx_punch_in_status` (`punch_in_status`),
    INDEX `idx_punch_out_status` (`punch_out_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录表';


-- ============================================================
-- 7. 请假申请表
-- ============================================================
DROP TABLE IF EXISTS `leave_application`;
CREATE TABLE `leave_application` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `employee_id`     BIGINT         NOT NULL                 COMMENT '员工ID',
    `leave_type`      TINYINT        NOT NULL                 COMMENT '假期类型: 1-年假 2-调休 3-事假 4-病假 5-婚假 6-产假 7-丧假',
    `start_date`      DATE           NOT NULL                 COMMENT '请假开始日期',
    `end_date`        DATE           NOT NULL                 COMMENT '请假结束日期',
    `days`            DECIMAL(4,1)   NOT NULL                 COMMENT '请假天数（支持0.5天）',
    `reason`          VARCHAR(500)   DEFAULT NULL             COMMENT '请假原因',
    `status`          TINYINT        NOT NULL DEFAULT 0       COMMENT '审批状态: 0-草稿 1-审批中 2-已通过 3-已拒绝',
    `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_employee_id` (`employee_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_date_range` (`start_date`, `end_date`),
    INDEX `idx_leave_type` (`leave_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';


-- ============================================================
-- 8. 假期余额表
-- ============================================================
DROP TABLE IF EXISTS `leave_balance`;
CREATE TABLE `leave_balance` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `employee_id`      BIGINT         NOT NULL                 COMMENT '员工ID',
    `leave_type`       TINYINT        NOT NULL                 COMMENT '假期类型: 1-年假 2-调休 3-事假 4-病假 5-婚假 6-产假 7-丧假',
    `total_days`       DECIMAL(4,1)   NOT NULL DEFAULT 0.0     COMMENT '总余额（天）',
    `used_days`        DECIMAL(4,1)   NOT NULL DEFAULT 0.0     COMMENT '已使用天数',
    `remaining_days`   DECIMAL(4,1)   NOT NULL DEFAULT 0.0     COMMENT '剩余天数',
    `year`             INT            NOT NULL                 COMMENT '年份（年假按年计算）',
    `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_employee_leave_year` (`employee_id`, `leave_type`, `year`),
    INDEX `idx_employee_id` (`employee_id`),
    INDEX `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='假期余额表';


-- ============================================================
-- 9. 加班记录表
-- ============================================================
DROP TABLE IF EXISTS `overtime_record`;
CREATE TABLE `overtime_record` (
    `id`                  BIGINT         NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `employee_id`         BIGINT         NOT NULL                 COMMENT '员工ID',
    `overtime_date`       DATE           NOT NULL                 COMMENT '加班日期',
    `start_time`          DATETIME       NOT NULL                 COMMENT '加班开始时间',
    `end_time`            DATETIME       NOT NULL                 COMMENT '加班结束时间',
    `hours`               DECIMAL(6,2)   NOT NULL                 COMMENT '加班时长（小时）',
    `converted_to_comp`   TINYINT        NOT NULL DEFAULT 0       COMMENT '是否已转调休: 0-未转换 1-已转换',
    `create_time`         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_employee_id` (`employee_id`),
    INDEX `idx_overtime_date` (`overtime_date`),
    INDEX `idx_converted` (`converted_to_comp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班记录表';


-- ═══════════════════════════════════════════════════
-- 种子数据
-- ═══════════════════════════════════════════════════

-- 默认根部门
INSERT INTO `department` (`parent_id`, `dept_name`, `dept_code`, `sort_order`, `level`, `status`, `manager_id`, `description`, `create_time`, `update_time`)
VALUES (0, '总公司', 'ROOT', 1, 1, 1, NULL, '公司总部，统筹管理全公司业务', NOW(), NOW());

-- 默认职位（覆盖 M/P/S 序列）
INSERT INTO `position` (`position_name`, `position_code`, `sequence`, `grade_range`, `default_probation_months`, `dept_id`, `description`, `is_standard`, `status`, `create_time`, `update_time`) VALUES
('总经理',       'M5', 'M', 'M4-M5', 6, NULL, '公司最高管理者',       1, 1, NOW(), NOW()),
('部门经理',     'M3', 'M', 'M2-M3', 3, NULL, '部门负责人',           1, 1, NOW(), NOW()),
('HR主管',       'P4', 'P', 'P3-P5', 3, NULL, '人力资源主管',         1, 1, NOW(), NOW()),
('高级工程师',   'P3', 'P', 'P1-P5', 3, NULL, '软件开发高级岗',       1, 1, NOW(), NOW()),
('行政专员',     'S2', 'S', 'S1-S3', 3, NULL, '行政支持岗',           1, 1, NOW(), NOW());

-- 默认考勤组（标准固定班 9:00-18:00，迟到宽限30分钟，半天旷工阈值120分钟）
INSERT INTO `attendance_group` (`group_name`, `group_type`, `start_time`, `end_time`, `flex_threshold`, `absent_half_day_threshold`, `create_time`, `update_time`)
VALUES ('标准考勤组', 1, '09:00:00', '18:00:00', 30, 120, NOW(), NOW());
