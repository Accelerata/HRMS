-- ═══════════════════════════════════════════════════
-- Phase 2: RBAC 权限系统 — 建表脚本
-- ═══════════════════════════════════════════════════
-- 依赖关系: sys_user_role.role_id 引用 sys_role.id（逻辑关联，无外键约束）
-- 执行顺序: 本脚本应在 phase1-core.sql 之后、phase3-approval.sql 之前执行
-- 初始管理员: admin / Admin@123


-- ============================================================
-- 1. 系统用户表
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `username`          VARCHAR(32)   NOT NULL                 COMMENT '登录账号（默认手机号）',
    `password`          VARCHAR(128)  NOT NULL                 COMMENT '加密后的密码（BCrypt）',
    `status`            TINYINT       NOT NULL DEFAULT 1       COMMENT '账号状态: 0-禁用 1-正常',
    `login_fail_count`  TINYINT       NOT NULL DEFAULT 0       COMMENT '连续登录失败次数（>=5 锁定）',
    `pwd_update_time`   DATETIME      DEFAULT NULL             COMMENT '密码最后修改时间（用于90天强制更换校验）',
    `last_login_time`   DATETIME      DEFAULT NULL             COMMENT '最后登录时间',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_username` (`username`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';


-- ============================================================
-- 2. 角色表
-- ============================================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `role_name`       VARCHAR(64)   NOT NULL                 COMMENT '角色名称（如：HR专员）',
    `role_code`       VARCHAR(32)   NOT NULL                 COMMENT '角色编码（如：ROLE_HR）',
    `data_scope`      TINYINT       NOT NULL DEFAULT 5       COMMENT '数据范围: 1-全平台 2-全部员工 3-本部门及下属 4-薪资相关 5-仅本人',
    `description`     VARCHAR(500)  DEFAULT NULL             COMMENT '角色描述',
    `status`          TINYINT       NOT NULL DEFAULT 1       COMMENT '状态: 0-禁用 1-正常',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_role_code` (`role_code`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';


-- ============================================================
-- 3. 权限菜单表
-- ============================================================
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `parent_id`         BIGINT        NOT NULL DEFAULT 0       COMMENT '父级权限ID（0为顶级目录）',
    `menu_name`         VARCHAR(64)   NOT NULL                 COMMENT '菜单或按钮名称',
    `permission_code`   VARCHAR(64)   NOT NULL                 COMMENT '权限标识（如 emp:view，唯一）',
    `type`              TINYINT       NOT NULL                 COMMENT '类型: 1-目录 2-菜单 3-按钮/字段控制',
    `path`              VARCHAR(128)  DEFAULT NULL             COMMENT '前端路由地址',
    `sort_order`        INT           NOT NULL DEFAULT 0       COMMENT '排序序号',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_permission_code` (`permission_code`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限菜单表';


-- ============================================================
-- 4. 用户-角色关联表（联合主键）
-- ============================================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `user_id`   BIGINT  NOT NULL COMMENT '用户ID',
    `role_id`   BIGINT  NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`),
    INDEX `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';


-- ============================================================
-- 5. 角色-权限关联表（联合主键）
-- ============================================================
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `role_id`         BIGINT  NOT NULL COMMENT '角色ID',
    `permission_id`   BIGINT  NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`role_id`, `permission_id`),
    INDEX `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';


-- ═══════════════════════════════════════════════════
-- 种子数据 — 角色
-- ═══════════════════════════════════════════════════

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `data_scope`, `description`, `status`, `create_time`, `update_time`) VALUES
(1, '系统管理员', 'ROLE_ADMIN',    1, '全平台最高权限',                     1, NOW(), NOW()),
(2, 'HR专员',     'ROLE_HR',       2, '全量员工档案与考勤，薪资核算与审批', 1, NOW(), NOW()),
(3, '部门主管',   'ROLE_MANAGER',  3, '本部门及下属部门，不可见薪资',       1, NOW(), NOW()),
(4, '财务专员',   'ROLE_FINANCE',  4, '仅薪资相关（审核、成本报表）',       1, NOW(), NOW()),
(5, '普通员工',   'ROLE_EMPLOYEE', 5, '仅本人数据',                         1, NOW(), NOW());


-- ═══════════════════════════════════════════════════
-- 种子数据 — 权限菜单树
-- ═══════════════════════════════════════════════════

-- 一级目录: 系统管理
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(1,  0, '系统管理', 'sys',          1, NULL,               1, NOW(), NOW()),
(2,  1, '用户管理', 'sys:user',     2, '/sys/user',        1, NOW(), NOW()),
(3,  2, '查看',     'sys:user:view',   3, NULL, 1, NOW(), NOW()),
(4,  2, '新增',     'sys:user:create', 3, NULL, 2, NOW(), NOW()),
(5,  2, '编辑',     'sys:user:edit',   3, NULL, 3, NOW(), NOW()),
(6,  2, '删除',     'sys:user:delete', 3, NULL, 4, NOW(), NOW()),
(7,  1, '角色管理', 'sys:role',     2, '/sys/role',        2, NOW(), NOW()),
(8,  7, '查看',     'sys:role:view',   3, NULL, 1, NOW(), NOW()),
(9,  7, '新增',     'sys:role:create', 3, NULL, 2, NOW(), NOW()),
(10, 7, '编辑',     'sys:role:edit',   3, NULL, 3, NOW(), NOW()),
(11, 7, '删除',     'sys:role:delete', 3, NULL, 4, NOW(), NOW()),
(12, 1, '菜单管理', 'sys:perm',     2, '/sys/permission',  3, NOW(), NOW()),
(13, 12,'查看',     'sys:perm:view',   3, NULL, 1, NOW(), NOW()),
(14, 12,'管理',     'sys:perm:manage', 3, NULL, 2, NOW(), NOW());

-- 一级目录: 组织架构
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(15, 0, '组织架构', 'org',             1, NULL,               2, NOW(), NOW()),
(16, 15,'部门管理', 'org:dept',        2, '/org/dept',        1, NOW(), NOW()),
(17, 16,'查看',     'org:dept:view',   3, NULL, 1, NOW(), NOW()),
(18, 16,'管理',     'org:dept:manage', 3, NULL, 2, NOW(), NOW()),
(19, 15,'职位管理', 'org:position',    2, '/org/position',    2, NOW(), NOW()),
(20, 19,'查看',     'org:position:view',   3, NULL, 1, NOW(), NOW()),
(21, 19,'管理',     'org:position:manage', 3, NULL, 2, NOW(), NOW());

-- 一级目录: 员工管理
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(22, 0, '员工管理', 'emp',              1, NULL,               3, NOW(), NOW()),
(23, 22,'员工档案', 'emp:profile',      2, '/emp/profile',     1, NOW(), NOW()),
(24, 23,'查看',     'emp:view',         3, NULL, 1, NOW(), NOW()),
(25, 23,'新增',     'emp:create',       3, NULL, 2, NOW(), NOW()),
(26, 23,'编辑',     'emp:edit',         3, NULL, 3, NOW(), NOW()),
(27, 23,'删除',     'emp:delete',       3, NULL, 4, NOW(), NOW()),
(28, 23,'查看薪资', 'emp:salary:view',  3, NULL, 5, NOW(), NOW());

-- 一级目录: 考勤管理
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(29, 0, '考勤管理', 'att',              1, NULL,                4, NOW(), NOW()),
(30, 29,'打卡记录', 'att:record',       2, '/att/record',       1, NOW(), NOW()),
(31, 30,'查看',     'att:record:view',  3, NULL, 1, NOW(), NOW()),
(32, 30,'管理',     'att:record:manage',3, NULL, 2, NOW(), NOW()),
(33, 29,'考勤组管理','att:group',       2, '/att/group',        2, NOW(), NOW()),
(34, 33,'查看',     'att:group:view',   3, NULL, 1, NOW(), NOW()),
(35, 33,'管理',     'att:group:manage', 3, NULL, 2, NOW(), NOW()),
(36, 29,'请假申请', 'att:leave',        2, '/att/leave',        3, NOW(), NOW()),
(37, 36,'查看',     'att:leave:view',   3, NULL, 1, NOW(), NOW()),
(38, 36,'申请',     'att:leave:apply',  3, NULL, 2, NOW(), NOW()),
(39, 36,'审批',     'att:leave:approve',3, NULL, 3, NOW(), NOW()),
(40, 29,'加班记录', 'att:overtime',     2, '/att/overtime',     4, NOW(), NOW()),
(41, 40,'查看',     'att:overtime:view',   3, NULL, 1, NOW(), NOW()),
(42, 40,'申请',     'att:overtime:apply',  3, NULL, 2, NOW(), NOW()),
(43, 40,'审批',     'att:overtime:approve',3, NULL, 3, NOW(), NOW());

-- 一级目录: 薪资管理
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(44, 0, '薪资管理', 'salary',           1, NULL,                5, NOW(), NOW()),
(45, 44,'薪资账套', 'salary:account',   2, '/salary/account',   1, NOW(), NOW()),
(46, 45,'查看',     'salary:account:view',   3, NULL, 1, NOW(), NOW()),
(47, 45,'管理',     'salary:account:manage', 3, NULL, 2, NOW(), NOW()),
(48, 44,'薪资核算', 'salary:calc',      2, '/salary/calc',      2, NOW(), NOW()),
(49, 48,'查看',     'salary:calc:view',     3, NULL, 1, NOW(), NOW()),
(50, 48,'核算',     'salary:calc:calc',     3, NULL, 2, NOW(), NOW()),
(51, 48,'审批',     'salary:calc:approve',  3, NULL, 3, NOW(), NOW()),
(52, 44,'社保配置', 'salary:social',    2, '/salary/social',    3, NOW(), NOW()),
(53, 52,'查看',     'salary:social:view',   3, NULL, 1, NOW(), NOW()),
(54, 52,'配置',     'salary:social:config', 3, NULL, 2, NOW(), NOW());

-- 一级目录: 审批管理
INSERT INTO `sys_permission` (`id`, `parent_id`, `menu_name`, `permission_code`, `type`, `path`, `sort_order`, `create_time`, `update_time`) VALUES
(55, 0, '审批管理', 'approval',          1, NULL,                6, NOW(), NOW()),
(56, 55,'审批工作台','approval:workbench',2, '/approval/workbench', 1, NOW(), NOW()),
(57, 56,'查看',     'approval:view',     3, NULL, 1, NOW(), NOW()),
(58, 56,'审批',     'approval:approve',  3, NULL, 2, NOW(), NOW());


-- ═══════════════════════════════════════════════════
-- 种子数据 — 角色-权限关联
-- ═══════════════════════════════════════════════════

-- ROLE_ADMIN (id=1): 全部权限 (id=1..58)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),
(1,11),(1,12),(1,13),(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),
(1,21),(1,22),(1,23),(1,24),(1,25),(1,26),(1,27),(1,28),(1,29),(1,30),
(1,31),(1,32),(1,33),(1,34),(1,35),(1,36),(1,37),(1,38),(1,39),(1,40),
(1,41),(1,42),(1,43),(1,44),(1,45),(1,46),(1,47),(1,48),(1,49),(1,50),
(1,51),(1,52),(1,53),(1,54),(1,55),(1,56),(1,57),(1,58);

-- ROLE_HR (id=2): 组织架构+全部员工+考勤+薪资+审批
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2,15),(2,16),(2,17),(2,18),(2,19),(2,20),(2,21),
(2,22),(2,23),(2,24),(2,25),(2,26),(2,27),(2,28),
(2,29),(2,30),(2,31),(2,32),(2,33),(2,34),(2,35),(2,36),(2,37),(2,38),(2,39),(2,40),(2,41),(2,42),(2,43),
(2,44),(2,45),(2,46),(2,47),(2,48),(2,49),(2,50),(2,51),(2,52),(2,53),(2,54),
(2,55),(2,56),(2,57),(2,58);

-- ROLE_MANAGER (id=3): 部门/员工查看+考勤查看+请假/加班审批+审批工作台
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3,15),(3,16),(3,17),(3,19),(3,20),
(3,22),(3,23),(3,24),
(3,29),(3,30),(3,31),(3,33),(3,34),(3,36),(3,37),(3,38),(3,39),(3,40),(3,41),(3,42),(3,43),
(3,55),(3,56),(3,57),(3,58);

-- ROLE_FINANCE (id=4): 员工薪资查看+薪资管理全部
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(4,22),(4,23),(4,28),
(4,44),(4,45),(4,46),(4,47),(4,48),(4,49),(4,50),(4,51),(4,52),(4,53),(4,54);

-- ROLE_EMPLOYEE (id=5): 本人数据查看+请假/加班申请+本人工资条查看
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(5,22),(5,23),(5,24),
(5,29),(5,30),(5,31),(5,36),(5,38),(5,40),(5,42),
(5,44),(5,48),(5,49);


-- ═══════════════════════════════════════════════════
-- 种子数据 — 默认管理员账号
-- ═══════════════════════════════════════════════════
-- 用户名: admin
-- 密码: Admin@123 (BCrypt 加密)
-- 角色: ROLE_ADMIN

INSERT INTO `sys_user` (`id`, `username`, `password`, `status`, `login_fail_count`, `pwd_update_time`, `last_login_time`, `create_time`, `update_time`)
VALUES (1, 'admin', '$2b$10$KAZw4wP/LnbFiVA8Ov8pEO.mI9T9hNa.PcxbyxrAHiCgpSRX8F8se', 1, 0, NOW(), NULL, NOW(), NOW());

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);
