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


-- ═══════════════════════════════════════════════════
-- 5. 测试数据: 10 条员工记录
-- ═══════════════════════════════════════════════════
-- 覆盖全部状态: 0-待入职 / 1-试用期 / 2-正式 / 3-待离职 / 4-已离职
-- 覆盖全部入职类型: 1-社招 / 2-校招 / 3-内推 / 4-调动

INSERT INTO `employee` (
    employee_no, name,
    gender, phone, email, id_card, birthday, registered_address, current_address,
    dept_id, position_id, grade,
    report_to, work_location, entry_type,
    salary_account_id, base_salary, bank_account, bank_name,
    status, entry_date, regular_date, resign_date, user_id
) VALUES

-- 1. 总经理 | M5 | 正式 | 社招 | 创始人级别
('HR20240001', '张建国',
 1, '13800010001', 'zhangjianguo@hrms.com',
 '110101198005152536', '1980-05-15', '北京市朝阳区建国路88号', '北京市海淀区中关村大街1号',
 1, 1, 'M5',
 NULL, '北京总部', 1,
 1, 50000.00, '6222020200001234567', '中国工商银行北京分行',
 2, '2019-03-01', '2019-09-01', NULL, NULL),

-- 2. 技术经理 | M3 | 正式 | 社招 | 汇报给张建国
('HR20240002', '李明辉',
 1, '13800010002', 'liminghui@hrms.com',
 '310101198507123014', '1985-07-12', '上海市浦东新区张江高科技园区', '北京市朝阳区望京SOHO T1-1506',
 1, 2, 'M3',
 1, '北京总部', 1,
 1, 35000.00, '6222020200001234568', '中国建设银行北京分行',
 2, '2020-06-15', '2020-12-15', NULL, NULL),

-- 3. HR主管 | P4 | 正式 | 社招 | 汇报给张建国
('HR20240003', '王雪梅',
 2, '13900020001', 'wangxuemei@hrms.com',
 '320102199010082346', '1990-10-08', '南京市鼓楼区中山北路200号', '北京市朝阳区大望路万达广场3号楼',
 1, 3, 'P4',
 1, '北京总部', 1,
 1, 22000.00, '6222020200001234569', '招商银行北京分行',
 2, '2020-09-01', '2021-03-01', NULL, NULL),

-- 4. 高级工程师 | P3 | 试用期 | 校招 | 汇报给李明辉
('HR20260001', '赵志远',
 1, '13600030001', 'zhaozhiyuan@hrms.com',
 '410102200008152052', '2000-08-15', '郑州市金水区文化路85号', '北京市昌平区回龙观东大街矩阵小区12-3-501',
 1, 4, 'P3',
 2, '北京总部', 2,
 1, 15000.00, '6222020200001234570', '中国银行北京分行',
 1, '2026-07-01', NULL, NULL, NULL),

-- 5. 行政专员 | S2 | 正式 | 社招 | 汇报给王雪梅
('HR20240005', '陈晓芳',
 2, '13700040001', 'chenxiaofang@hrms.com',
 '330102199203210642', '1992-03-21', '杭州市西湖区文三路259号', '北京市通州区梨园镇云景里小区8-2-302',
 1, 5, 'S2',
 3, '北京总部', 1,
 1, 9000.00, '6222020200001234571', '中国农业银行北京分行',
 2, '2021-04-12', '2021-10-12', NULL, NULL),

-- 6. 高级工程师 | P4 | 正式 | 内推 | 汇报给李明辉
('HR20240006', '刘伟强',
 1, '13500050001', 'liuweiqiang@hrms.com',
 '440102199306180338', '1993-06-18', '广州市天河区天河路385号', '北京市大兴区亦庄经济技术开发区荣京东街3号院',
 1, 4, 'P4',
 2, '北京总部', 3,
 1, 28000.00, '6222020200001234572', '交通银行北京分行',
 2, '2022-03-15', '2022-09-15', NULL, NULL),

-- 7. 高级工程师 | P2 | 试用期 | 校招 | 汇报给李明辉
('HR20260002', '孙丽华',
 2, '13600070001', 'sunlihua@hrms.com',
 '510102200106150521', '2001-06-15', '成都市武侯区人民南路四段11号', '北京市丰台区宋家庄地铁站附近鑫兆雅园15-1-1202',
 1, 4, 'P2',
 2, '北京总部', 2,
 1, 12000.00, '6222020200001234573', '中国邮政储蓄银行北京分行',
 1, '2026-07-06', NULL, NULL, NULL),

-- 8. 行政专员 | S1 | 待入职 | 校招 | 7月下旬入职
('HR20260003', '周文博',
 1, '13600080001', 'zhouwenbo@hrms.com',
 '420102200209280017', '2002-09-28', '武汉市武昌区中南路7号', '待确认',
 1, 5, 'S1',
 3, '北京总部', 2,
 1, 7000.00, '6222020200001234574', '中国工商银行北京分行',
 0, '2026-07-20', NULL, NULL, NULL),

-- 9. 高级工程师 | P5 | 待离职 | 社招 | 已提离职申请，最后工作日8月15日
('HR20240009', '吴思远',
 1, '13500090001', 'wusiyuan@hrms.com',
 '350102198812050032', '1988-12-05', '福州市鼓楼区五四路158号', '北京市海淀区上地信息路10号南天大厦2-901',
 1, 4, 'P5',
 2, '北京总部', 1,
 1, 38000.00, '6222020200001234575', '中国建设银行北京分行',
 3, '2023-02-01', '2023-08-01', '2026-08-15', NULL),

-- 10. 部门经理 | M2 | 已离职 | 调动 | 2025年底离职
('HR20240010', '郑海龙',
 1, '13500100001', 'zhenghailong@hrms.com',
 '370102198203120019', '1982-03-12', '济南市历下区经十路17513号', '已迁出',
 1, 2, 'M2',
 1, '上海分部', 4,
 1, 30000.00, '6222020200001234576', '招商银行上海分行',
 4, '2021-08-01', '2022-02-01', '2025-12-31', NULL);
