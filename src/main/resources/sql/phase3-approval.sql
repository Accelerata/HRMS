-- ═══════════════════════════════════════════════════
-- Phase 3: 审批状态机与入转调离 — 建表脚本
-- ═══════════════════════════════════════════════════

-- 1. 审批模板表：定义每种业务类型的审批步骤
DROP TABLE IF EXISTS `approval_template`;
CREATE TABLE `approval_template` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `business_type`   TINYINT      NOT NULL                COMMENT '业务类型: 1-入职 2-转正 3-调岗 4-离职 5-薪资',
    `step_order`      TINYINT      NOT NULL                COMMENT '审批步骤序号（同序号=并行审批）',
    `approver_target` VARCHAR(32)  NOT NULL                COMMENT '审批人指向: dept_manager / hr_specialist / old_dept_manager / new_dept_manager',
    `step_name`       VARCHAR(64)  DEFAULT NULL            COMMENT '步骤名称（如"部门主管一审"）',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批模板表';


-- 2. 审批记录表：实际审批过程
DROP TABLE IF EXISTS `approval_record`;
CREATE TABLE `approval_record` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `business_type`   TINYINT      NOT NULL                COMMENT '业务类型',
    `business_id`     BIGINT       NOT NULL                COMMENT '业务单据ID',
    `step_order`      TINYINT      NOT NULL                COMMENT '审批步骤序号',
    `approver_id`     BIGINT       DEFAULT NULL            COMMENT '审批人ID（关联 sys_user.id）',
    `approver_name`   VARCHAR(32)  DEFAULT NULL            COMMENT '审批人姓名',
    `action`          TINYINT      DEFAULT NULL            COMMENT '审批动作: 1-通过 2-拒绝 3-退回',
    `comment`         VARCHAR(500) DEFAULT NULL            COMMENT '审批意见',
    `is_pending`      TINYINT      NOT NULL DEFAULT 1      COMMENT '是否待审批: 1-待审 0-已处理',
    `operate_time`    DATETIME     DEFAULT NULL            COMMENT '审批操作时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_business` (`business_type`, `business_id`),
    INDEX `idx_approver_pending` (`approver_id`, `is_pending`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';


-- 3. 入职申请表
DROP TABLE IF EXISTS `onboarding_application`;
CREATE TABLE `onboarding_application` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `real_name`         VARCHAR(32)  NOT NULL                COMMENT '候选人姓名',
    `phone`             VARCHAR(20)  NOT NULL                COMMENT '手机号',
    `email`             VARCHAR(64)  DEFAULT NULL            COMMENT '邮箱',
    `id_card`           VARCHAR(20)  DEFAULT NULL            COMMENT '身份证号',
    `target_dept_id`    BIGINT       NOT NULL                COMMENT '目标部门ID',
    `target_position_id` BIGINT      NOT NULL                COMMENT '目标职位ID',
    `offer_salary`      DECIMAL(12,2) NOT NULL              COMMENT '入职薪酬',
    `probation_months`  TINYINT      DEFAULT 3              COMMENT '试用期月数',
    `entry_date`        DATE         NOT NULL                COMMENT '预计入职日期',
    `status`            TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-草稿 1-审批中 2-已通过 3-已拒绝 4-待入职 5-已入职',
    `employee_id`       BIGINT       DEFAULT NULL            COMMENT '入职后关联员工ID(审批通过后回填)',
    `submitter_id`      BIGINT       NOT NULL                COMMENT '提交人ID',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_submitter` (`submitter_id`),
    INDEX `idx_target_dept` (`target_dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入职申请表';


-- 4. 转正申请表
DROP TABLE IF EXISTS `regularization_application`;
CREATE TABLE `regularization_application` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `employee_id`         BIGINT       NOT NULL                COMMENT '员工ID',
    `formal_salary`       DECIMAL(12,2) DEFAULT NULL          COMMENT '转正后薪资',
    `probation_summary`   TEXT         DEFAULT NULL            COMMENT '试用期工作小结',
    `supervisor_comment`  VARCHAR(500) DEFAULT NULL            COMMENT '直属上级评语',
    `status`              TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-草稿 1-审批中 2-已通过 3-已拒绝',
    `submitter_id`        BIGINT       NOT NULL                COMMENT '提交人ID',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_employee` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转正申请表';


-- 5. 调岗申请表
DROP TABLE IF EXISTS `transfer_application`;
CREATE TABLE `transfer_application` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `employee_id`         BIGINT       NOT NULL                COMMENT '员工ID',
    `from_dept_id`        BIGINT       NOT NULL                COMMENT '原部门ID',
    `to_dept_id`          BIGINT       NOT NULL                COMMENT '新部门ID',
    `from_position_id`    BIGINT       NOT NULL                COMMENT '原职位ID',
    `to_position_id`      BIGINT       DEFAULT NULL            COMMENT '新职位ID',
    `transfer_reason`     VARCHAR(500) DEFAULT NULL            COMMENT '调岗原因',
    `effective_date`      DATE         NOT NULL                COMMENT '生效日期',
    `old_manager_approved` TINYINT      DEFAULT 0              COMMENT '原部门负责人审批: 0-未审 1-通过 2-拒绝',
    `new_manager_approved` TINYINT      DEFAULT 0              COMMENT '新部门负责人审批: 0-未审 1-通过 2-拒绝',
    `status`              TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-草稿 1-审批中 2-已通过 3-已拒绝',
    `submitter_id`        BIGINT       NOT NULL                COMMENT '提交人ID',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_employee` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调岗申请表';


-- 6. 离职申请表
DROP TABLE IF EXISTS `resignation_application`;
CREATE TABLE `resignation_application` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `employee_id`       BIGINT       NOT NULL                COMMENT '员工ID',
    `resignation_type`  TINYINT      NOT NULL                COMMENT '离职类型: 1-主动 2-协商 3-合同到期 4-裁员',
    `resignation_reason` VARCHAR(500) DEFAULT NULL           COMMENT '离职原因',
    `resignation_date`  DATE         NOT NULL                COMMENT '最后工作日',
    `handover_info`     VARCHAR(500) DEFAULT NULL            COMMENT '交接事项',
    `handover_to`       BIGINT       DEFAULT NULL            COMMENT '接手人ID',
    `status`            TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-草稿 1-审批中 2-已通过 3-已拒绝',
    `submitter_id`      BIGINT       NOT NULL                COMMENT '提交人ID',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_employee` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离职申请表';


-- 7. 员工异动日志表
DROP TABLE IF EXISTS `employee_transfer`;
CREATE TABLE `employee_transfer` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `employee_id`     BIGINT       NOT NULL                COMMENT '员工ID',
    `transfer_type`   TINYINT      NOT NULL                COMMENT '异动类型: 1-入职 2-转正 3-调岗 4-离职',
    `business_id`     BIGINT       NOT NULL                COMMENT '关联业务单据ID',
    `before_data`     JSON         DEFAULT NULL            COMMENT '异动前数据(JSON)',
    `after_data`      JSON         DEFAULT NULL            COMMENT '异动后数据(JSON)',
    `effective_date`  DATE         DEFAULT NULL            COMMENT '生效日期',
    `remark`          VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_employee` (`employee_id`),
    INDEX `idx_type` (`transfer_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工异动日志表';


-- ═══════════════════════════════════════
-- 初始化审批模板数据
-- ═══════════════════════════════════════
INSERT INTO `approval_template` (`business_type`, `step_order`, `approver_target`, `step_name`) VALUES
-- 入职: 一审=目标部门主管, 二审=HR专员
(1, 1, 'dept_manager',     '目标部门主管审批'),
(1, 2, 'hr_specialist',     'HR专员审批'),

-- 转正: 一审=部门主管, 二审=HR专员
(2, 1, 'dept_manager',     '直属部门主管审批'),
(2, 2, 'hr_specialist',     'HR专员审批'),

-- 调岗: 并行审批=原部门主管 + 新部门主管
(3, 1, 'old_dept_manager', '原部门负责人审批'),
(3, 1, 'new_dept_manager', '新部门负责人审批'),

-- 离职: 一审=部门主管, 二审=HR专员
(4, 1, 'dept_manager',     '直属部门主管审批'),
(4, 2, 'hr_specialist',     'HR专员审批');
