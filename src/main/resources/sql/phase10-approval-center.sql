-- ═══════════════════════════════════════════════════
-- Phase 10: 审批中心补全 — 增量脚本
-- 内容：
--  1. approval_record 增列（代审留痕 + 提交人）
--  2. leave_application 增列（工作交接人）
--  3. salary_record 增列（批次挂接）
--  4. 新建补卡申请表 / 薪资批次表 / 审批委托表
--  5. 审批模板种子（请假/补卡/薪资批次）
--  6. RBAC 种子（补卡权限码 + ROLE_FINANCE 审批权限树）
-- ═══════════════════════════════════════════════════

-- ============================================================
-- 1. approval_record 表：代审留痕与提交人列
-- ============================================================
ALTER TABLE `approval_record`
    ADD COLUMN `original_approver_id`   BIGINT      DEFAULT NULL COMMENT '原审批人ID（转交/委托改派前）' AFTER `approver_name`,
    ADD COLUMN `original_approver_name` VARCHAR(32) DEFAULT NULL COMMENT '原审批人姓名' AFTER `original_approver_id`,
    ADD COLUMN `assign_type`            TINYINT     NOT NULL DEFAULT 0 COMMENT '分配方式: 0-正常 1-转交 2-委托' AFTER `original_approver_name`,
    ADD COLUMN `submitter_id`           BIGINT      DEFAULT NULL COMMENT '提交人ID（sys_user.id，审批结果通知对象）' AFTER `assign_type`;


-- ============================================================
-- 2. leave_application 表：工作交接人
-- ============================================================
ALTER TABLE `leave_application`
    ADD COLUMN `handover_to` BIGINT DEFAULT NULL COMMENT '工作交接人ID（关联 employee.id）' AFTER `reason`;


-- ============================================================
-- 3. salary_record 表：批次挂接
-- ============================================================
ALTER TABLE `salary_record`
    ADD COLUMN `batch_id` BIGINT DEFAULT NULL COMMENT '所属薪资批次ID' AFTER `month`,
    ADD INDEX `idx_batch` (`batch_id`);


-- ============================================================
-- 4.1 补卡申请表
-- ============================================================
DROP TABLE IF EXISTS `supplementary_card_application`;
CREATE TABLE `supplementary_card_application` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `employee_id`     BIGINT       NOT NULL                 COMMENT '员工ID',
    `attendance_date` DATE         NOT NULL                 COMMENT '补卡日期',
    `card_type`       TINYINT      NOT NULL                 COMMENT '卡型: 1-上班卡 2-下班卡',
    `supplement_time` TIME         NOT NULL                 COMMENT '补卡时间',
    `reason`          VARCHAR(500) NOT NULL                 COMMENT '补卡事由',
    `status`          TINYINT      NOT NULL DEFAULT 1       COMMENT '状态: 0-草稿 1-审批中 2-已通过 3-已拒绝',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_employee` (`employee_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_emp_date_type` (`employee_id`, `attendance_date`, `card_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';


-- ============================================================
-- 4.2 薪资批次表
-- ============================================================
DROP TABLE IF EXISTS `salary_batch`;
CREATE TABLE `salary_batch` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `year`           INT            NOT NULL                COMMENT '核算年份',
    `month`          INT            NOT NULL                COMMENT '核算月份(1-12)',
    `status`         VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING/APPROVED/REJECTED',
    `employee_count` INT            NOT NULL DEFAULT 0      COMMENT '核算人数',
    `total_net_pay`  DECIMAL(14,2)  NOT NULL DEFAULT 0.00   COMMENT '实发合计',
    `submitter_id`   BIGINT         DEFAULT NULL            COMMENT '提交人ID（HR，sys_user.id）',
    `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_year_month` (`year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资批次表';


-- ============================================================
-- 4.3 审批委托表
-- ============================================================
DROP TABLE IF EXISTS `approval_delegation`;
CREATE TABLE `approval_delegation` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT   COMMENT '主键',
    `delegator_id`   BIGINT       NOT NULL                  COMMENT '委托人ID（原审批人）',
    `delegator_name` VARCHAR(32)  DEFAULT NULL              COMMENT '委托人姓名',
    `delegate_id`    BIGINT       NOT NULL                  COMMENT '被委托人ID',
    `delegate_name`  VARCHAR(32)  DEFAULT NULL              COMMENT '被委托人姓名',
    `start_time`     DATETIME     NOT NULL                  COMMENT '委托开始时间',
    `end_time`       DATETIME     NOT NULL                  COMMENT '委托结束时间',
    `status`         TINYINT      NOT NULL DEFAULT 1        COMMENT '状态: 0-已取消 1-生效中',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_delegator_status` (`delegator_id`, `status`),
    INDEX `idx_delegate` (`delegate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批委托表';


-- ============================================================
-- 5. 审批模板种子：请假(6) / 补卡(7) / 薪资批次(5)
-- ============================================================
INSERT INTO `approval_template` (`business_type`, `step_order`, `approver_target`, `step_name`, `condition_expr`) VALUES
-- 请假: 一级=直接上级；二级=部门负责人（年假/调休>3天、事假/病假>1天时触发）
(6, 1, 'direct_supervisor', '直接上级审批', NULL),
(6, 2, 'dept_manager',      '部门负责人审批', 'leaveNeedDeptManager'),

-- 补卡: 直接上级单级审批
(7, 1, 'direct_supervisor', '直接上级审批', NULL),

-- 薪资批次: 财务专员审批（需求 8.1「财务专员 → [老板]」，老板角色暂缺，预留第二级）
(5, 1, 'finance_specialist', '财务专员审批', NULL);


-- ============================================================
-- 6. RBAC 种子
--    6.1 补卡权限码：att:card:apply / att:card:approve
--    6.2 ROLE_FINANCE 补 approval 权限树（55-58），支撑薪资批次审批
-- ============================================================

-- 6.1 补卡权限码（挂在考勤管理 att=29 下）
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(64, 29,'补卡申请', 'att:card',        2, '/att/card',   5, NOW(), NOW()),
(65, 64,'申请',     'att:card:apply',  3, NULL, 1, NOW(), NOW()),
(66, 64,'审批',     'att:card:approve',3, NULL, 2, NOW(), NOW());

-- ROLE_ADMIN (id=1)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 64), (1, 65), (1, 66);

-- ROLE_HR (id=2)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2, 64), (2, 65), (2, 66);

-- ROLE_MANAGER (id=3): 主管既可发起也可审批补卡
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3, 64), (3, 65), (3, 66);

-- ROLE_EMPLOYEE (id=5): 员工发起补卡
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(5, 64), (5, 65);

-- 6.2 ROLE_FINANCE (id=4): 补 approval 权限树（审批管理菜单 + 工作台 + 查看 + 审批）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(4, 55), (4, 56), (4, 57), (4, 58);

-- 6.3 ROLE_EMPLOYEE (id=5): 补 att:leave:view(37)
--     员工需查看本人请假记录与余额（需求 9.3），phase2 遗漏未分配
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(5, 37);

-- 6.4 薪资批次查看权限码：salary:batch:view
--     批次含全公司实发合计，不可用 salary:calc:view（员工持有会越权）
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(67, 48,'批次查看', 'salary:batch:view', 3, NULL, 4, NOW(), NOW());

-- ROLE_ADMIN (id=1) / ROLE_HR (id=2) / ROLE_FINANCE (id=4)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 67), (2, 67), (4, 67);

-- 6.5 打卡操作权限码：att:record:punch（挂在打卡记录 att:record=30 下）
--     全角色均需打卡：管理员/HR/主管/员工
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(68, 30,'打卡', 'att:record:punch', 3, NULL, 3, NOW(), NOW());

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 68), (2, 68), (3, 68), (5, 68);
