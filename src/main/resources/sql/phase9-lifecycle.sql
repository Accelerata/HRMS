-- ═══════════════════════════════════════════════════
-- Phase 9: 入转调离生命周期补全 — 增量SQL脚本
-- ═══════════════════════════════════════════════════
-- 说明：本脚本为增量脚本，依赖 phase1 到 phase8 已执行
-- 执行顺序：应在现有所有 SQL 之后执行
-- 变更内容：
--   1. employee 表增加 employee_no 唯一约束
--   2. sys_user 表增加 force_change_pwd 列
--   3. notification 表
--   4. approval_record 表增加 due_time 列
--   5. 调岗审批模板调整为三级（old_dept→new_dept→hr）
--   6. 入职/转正/离职申请表增加字段
--   7. RBAC 新增四大流程权限码及角色分配
--   8. transfer_application 增加可调项字段
--   9. position 表增加 is_standard 列（入职二审条件判断）
--  10. grade_salary_range 表（职级薪资带宽）
--  11. approval_template 表增加 condition_expr 列（动态审批步骤）
--  12. 入职审批模板 HR 步骤标记为条件步骤 (condition_expr='needHr')
--  13. 调岗审批模板增加财务薪资审核步骤 (condition_expr='hasSalaryAdjust')


-- ============================================================
-- 1. employee 表：工号唯一约束
-- ============================================================
-- 若 employee_no 列已存在但无唯一约束，添加
-- 注意：若列不存在需先通过其它脚本添加，本脚本不做 ALTER ADD COLUMN
ALTER TABLE `employee`
    ADD UNIQUE INDEX `uk_employee_no` (`employee_no`);


-- ============================================================
-- 2. sys_user 表：增加 force_change_pwd 列
-- ============================================================
ALTER TABLE `sys_user`
    ADD COLUMN `force_change_pwd` TINYINT NOT NULL DEFAULT 0
    COMMENT '是否强制修改密码: 0-否 1-是' AFTER `login_fail_count`;


-- ============================================================
-- 3. notification 表（站内信/通知）
-- ============================================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `recipient_id`    BIGINT       NOT NULL                 COMMENT '接收人ID（sys_user.id）',
    `title`           VARCHAR(128) NOT NULL                 COMMENT '通知标题',
    `content`         TEXT         DEFAULT NULL             COMMENT '通知内容',
    `type`            TINYINT      NOT NULL DEFAULT 1       COMMENT '通知类型: 1-系统通知 2-审批通知 3-提醒 4-警告',
    `business_type`   TINYINT      DEFAULT NULL             COMMENT '关联业务类型',
    `business_id`     BIGINT       DEFAULT NULL             COMMENT '关联业务单据ID',
    `is_read`         TINYINT      NOT NULL DEFAULT 0       COMMENT '是否已读: 0-未读 1-已读',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_recipient_read` (`recipient_id`, `is_read`),
    INDEX `idx_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';


-- ============================================================
-- 4. approval_record 表：增加 due_time 列（审批超时截止时间）
-- ============================================================
ALTER TABLE `approval_record`
    ADD COLUMN `due_time` DATETIME DEFAULT NULL
    COMMENT '审批截止时间（startApproval 时计算，每级 48h）' AFTER `operate_time`;


-- ============================================================
-- 5. 调岗审批模板：改为三级顺序审批
--    old_dept_manager(step1) → new_dept_manager(step2) → hr_specialist(step3)
--    先删除旧的并行调岗模板，再插入新的三级模板
-- ============================================================
DELETE FROM `approval_template` WHERE `business_type` = 3;

INSERT INTO `approval_template` (`business_type`, `step_order`, `approver_target`, `step_name`) VALUES
(3, 1, 'old_dept_manager', '原部门负责人审批'),
(3, 2, 'new_dept_manager', '新部门负责人审批'),
(3, 3, 'hr_specialist',     'HR负责人备案');


-- ============================================================
-- 6. 入职申请表：增加录用类型、试用期薪资比例
-- ============================================================
ALTER TABLE `onboarding_application`
    ADD COLUMN `employment_type`   TINYINT DEFAULT 1       COMMENT '录用类型: 1-全职 2-兼职 3-实习' AFTER `entry_date`,
    ADD COLUMN `probation_salary_ratio` DECIMAL(3,2) DEFAULT 0.80 COMMENT '试用期薪资比例（如0.80表示80%）' AFTER `employment_type`;


-- ============================================================
-- 7. 转正申请表：增加审批结果类型字段
-- ============================================================
ALTER TABLE `regularization_application`
    ADD COLUMN `result_type` TINYINT DEFAULT NULL COMMENT '审批结果: 1-通过转正 2-延长试用 3-不通过辞退' AFTER `probation_summary`,
    ADD COLUMN `extended_months` TINYINT DEFAULT NULL COMMENT '延长试用月数' AFTER `result_type`;


-- ============================================================
-- 8. 调岗申请表：增加可调项字段
-- ============================================================
ALTER TABLE `transfer_application`
    ADD COLUMN `to_grade`       VARCHAR(16)  DEFAULT NULL COMMENT '目标职级' AFTER `to_position_id`,
    ADD COLUMN `to_report_to`   BIGINT       DEFAULT NULL COMMENT '新汇报人ID' AFTER `to_grade`,
    ADD COLUMN `salary_adjust`  DECIMAL(12,2) DEFAULT NULL COMMENT '调岗薪资调整金额' AFTER `to_report_to`;


-- ============================================================
-- 9. RBAC 种子数据：四大流程权限码
--    onboarding:manage / regularization:manage / transfer:manage / resignation:manage
--    先插入权限码，再分配给 ROLE_ADMIN 和 ROLE_HR
-- ============================================================

-- 一级目录: 入转调离
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(59, 0, '入转调离', 'hrlifecycle',       1, NULL,                7, NOW(), NOW()),
(60, 59,'入职管理', 'onboarding:manage',  2, '/lifecycle/onboarding',     1, NOW(), NOW()),
(61, 59,'转正管理', 'regularization:manage', 2, '/lifecycle/regularization', 2, NOW(), NOW()),
(62, 59,'调岗管理', 'transfer:manage',    2, '/lifecycle/transfer',        3, NOW(), NOW()),
(63, 59,'离职管理', 'resignation:manage', 2, '/lifecycle/resignation',     4, NOW(), NOW());

-- ROLE_ADMIN (id=1): 获得全部四个流程权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 59), (1, 60), (1, 61), (1, 62), (1, 63);

-- ROLE_HR (id=2): 获得全部四个流程权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2, 59), (2, 60), (2, 61), (2, 62), (2, 63);

-- ROLE_MANAGER (id=3): 获得入转调离目录 + 四流程查看（部门主管需要审批下属的入转调离）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3, 59), (3, 60), (3, 61), (3, 62), (3, 63);


-- ============================================================
-- 10. position 表：增加 is_standard 列（入职二审条件判断）
--    1=标准职位，0=非标准职位（如高管、特殊岗位）
--    非标准职位入职时强制触发 HR 二审
-- ============================================================
ALTER TABLE `position`
    ADD COLUMN `is_standard` TINYINT NOT NULL DEFAULT 1
    COMMENT '是否标准职位: 1-标准职位 0-非标准职位' AFTER `description`;


-- ============================================================
-- 11. grade_salary_range 表：职级薪资带宽
--    用于入职二审条件判断——薪资是否在对应职级的合理范围内
-- ============================================================
DROP TABLE IF EXISTS `grade_salary_range`;
CREATE TABLE `grade_salary_range` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `grade_code`  VARCHAR(16)    NOT NULL                 COMMENT '职级编码（如 P1、P3、M5）',
    `min_salary`  DECIMAL(12,2)  NOT NULL                 COMMENT '薪资下限（含，单位：元）',
    `max_salary`  DECIMAL(12,2)  NOT NULL                 COMMENT '薪资上限（含，单位：元）',
    `status`      TINYINT        NOT NULL DEFAULT 1       COMMENT '状态: 0-禁用 1-正常',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_grade_code` (`grade_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职级薪资范围表';

-- 种子数据：各职级默认薪资带宽（示例数据，按实际情况调整）
INSERT INTO `grade_salary_range` (`grade_code`, `min_salary`, `max_salary`) VALUES
('P1',   4000.00,  8000.00),
('P2',   6000.00,  12000.00),
('P3',   9000.00,  18000.00),
('P4',   13000.00, 25000.00),
('P5',   18000.00, 35000.00),
('P6',   25000.00, 45000.00),
('P7',   35000.00, 60000.00),
('M1',   8000.00,  15000.00),
('M2',   12000.00, 22000.00),
('M3',   18000.00, 32000.00),
('M4',   25000.00, 45000.00),
('M5',   35000.00, 60000.00),
('S1',   3000.00,  6000.00),
('S2',   4000.00,  8000.00),
('S3',   5000.00,  10000.00);


-- ============================================================
-- 12. approval_template 表：增加 condition_expr 列
--    用于动态审批步骤控制（NULL=无条件步骤）
--    'needHr' 表示仅当 needHr=true 时才生成该审批步骤
-- ============================================================
ALTER TABLE `approval_template`
    ADD COLUMN `condition_expr` VARCHAR(64) DEFAULT NULL
    COMMENT '条件表达式: NULL=无条件, needHr=需要HR审批时才生成此步骤' AFTER `step_name`;


-- ============================================================
-- 13. 入职审批模板：HR 二审步骤标记为条件步骤 (condition_expr='needHr')
--    标准职位 + 薪资在职级范围内 → 不生成 HR 审批步骤
--    非标准职位或薪资超范围 → 生成 HR 审批步骤
-- ============================================================
UPDATE `approval_template`
SET `condition_expr` = 'needHr'
WHERE `business_type` = 1 AND `approver_target` = 'hr_specialist';


-- ============================================================
-- 14. 调岗审批模板：增加财务薪资审核步骤 (condition_expr='hasSalaryAdjust')
--    仅当调岗涉及薪资调整时才触发第 4 级财务审批
-- ============================================================
INSERT INTO `approval_template` (`business_type`, `step_order`, `approver_target`, `step_name`, `condition_expr`)
VALUES (3, 4, 'finance_specialist', '财务薪资调整审核', 'hasSalaryAdjust');
