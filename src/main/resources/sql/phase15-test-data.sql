-- ============================================================
-- Phase 15: 完整测试数据
-- 覆盖所有模块的 API 测试场景
-- 运行环境: 基于 phase1-phase14 之后的数据库增量插入
-- 特性: 幂等（INSERT IGNORE），不会与已有数据冲突
-- 创建日期: 2026-07-16
-- ============================================================

SET NAMES utf8mb4;

-- ══════════════════════════════════════════════════════════════
-- 0. 修复已有数据问题
-- ══════════════════════════════════════════════════════════════

-- 修复 attendance_record 中 employee_id=1 的问题（改为 admin 对应的 employee_id=19）
-- 如果 employee_id=1 的记录存在且没有对应员工，则修正
UPDATE `attendance_record`
SET `employee_id` = 19
WHERE `employee_id` = 1
  AND NOT EXISTS (SELECT 1 FROM `employee` WHERE `id` = 1);

-- 修复 leave_balance 中 employee_id=2 的问题
DELETE FROM `leave_balance` WHERE `employee_id` = 2 AND NOT EXISTS (SELECT 1 FROM `employee` WHERE `id` = 2);


-- ══════════════════════════════════════════════════════════════
-- 1. 部门 (Department) — 测试 3.x 接口
--    GET /api/v1/dept/tree  → 验证树结构
--    POST /api/v1/dept      → 创建新部门
--    PUT /api/v1/dept/{id}  → 更新部门
--    DELETE /api/v1/dept/{id} → 删除空部门
-- ══════════════════════════════════════════════════════════════

INSERT IGNORE INTO `department` (`id`, `parent_id`, `dept_name`, `dept_code`, `sort_order`, `level`, `status`, `manager_id`, `description`, `create_time`, `update_time`) VALUES
(2, 1, '人力资源部', 'DEPT_HR',   2, 2, 1, NULL, '负责全公司人力资源管理与招聘',   NOW(), NOW()),
(4, 3, '产品部',     'DEPT_PROD', 2, 2, 1, NULL, '产品设计与需求管理',             NOW(), NOW());


-- ══════════════════════════════════════════════════════════════
-- 2. 职位 (Position) — 测试 4.x 接口
--    GET  /api/v1/position/list        → 查询所有
--    GET  /api/v1/position/list?sequence=P → 按序列筛选
--    GET  /api/v1/position/{id}        → 按ID查询
--    POST /api/v1/position             → 创建
--    PUT  /api/v1/position/{id}        → 更新
--    DELETE /api/v1/position/{id}      → 删除（无人使用的）
-- ══════════════════════════════════════════════════════════════

INSERT IGNORE INTO `position` (`id`, `position_name`, `position_code`, `sequence`, `dept_id`, `grade_range`, `default_probation_months`, `description`, `is_standard`, `status`, `create_time`, `update_time`) VALUES
-- M 管理序列
(1, '总经理',       'M5', 'M', NULL, 'M4-M5', 6, '公司最高管理者',                 1, 1, NOW(), NOW()),
(2, '部门经理',     'M3', 'M', NULL, 'M2-M3', 3, '部门负责人',                     1, 1, NOW(), NOW()),
-- P 专业序列
(3, 'HR主管',       'P4', 'P', NULL, 'P3-P5', 3, '人力资源主管',                   1, 1, NOW(), NOW()),
(4, '高级工程师',   'P3', 'P', NULL, 'P1-P5', 3, '软件开发高级岗',                 1, 1, NOW(), NOW()),
(5, '前端开发工程师','P2', 'P', NULL, 'P1-P4', 3, '前端开发岗位',                   1, 1, NOW(), NOW()),
(6, '初级工程师',   'P1', 'P', NULL, 'P1-P2', 3, '软件开发初级岗（用于删除测试）', 1, 1, NOW(), NOW()),
-- S 支持序列
(7, '行政专员',     'S2', 'S', NULL, 'S1-S3', 3, '行政支持岗',                     1, 1, NOW(), NOW());


-- ══════════════════════════════════════════════════════════════
-- 3. 考勤组 (Attendance Group) — 测试 5.4-5.7 接口
--    GET    /api/v1/attendance/groups      → 列表
--    POST   /api/v1/attendance/groups      → 创建
--    PUT    /api/v1/attendance/groups/{id} → 更新
--    DELETE /api/v1/attendance/groups/{id} → 删除
-- ══════════════════════════════════════════════════════════════

INSERT IGNORE INTO `attendance_group` (`id`, `group_name`, `group_type`, `start_time`, `end_time`, `flex_threshold`, `absent_half_day_threshold`, `lunch_break_start`, `lunch_break_end`, `late_threshold_minutes`, `early_threshold_minutes`, `create_time`, `update_time`) VALUES
(2, '弹性班制', 2, '09:30:00', '18:30:00', 30, 120, '12:00:00', '13:00:00', 30, 30, NOW(), NOW());


-- ══════════════════════════════════════════════════════════════
-- 4. 考勤记录 (Attendance Record) — 测试 5.1-5.3, 6.x 接口
--    POST /api/v1/attendance/punch-in     → 上班打卡
--    POST /api/v1/attendance/punch-out    → 下班打卡
--    GET  /api/v1/attendance/records/{id} → 查询打卡记录
--    GET  /api/v1/attendance-statistics/personal → 个人统计
--    GET  /api/v1/attendance-statistics/dept     → 部门统计
--
--    注意：record 1,2 已通过修复归属到 employee_id=19
-- ══════════════════════════════════════════════════════════════

-- 为 employee 20 (王浩然) 创建本周考勤记录
INSERT IGNORE INTO `attendance_record` (`employee_id`, `group_id`, `attendance_date`, `punch_in_time`, `punch_out_time`, `punch_in_status`, `punch_out_status`, `create_time`, `update_time`) VALUES
(20, 1, '2026-07-14', '09:05:00', '18:00:00', 'LATE',           'NORMAL',        NOW(), NOW()),
(20, 1, '2026-07-15', '08:55:00', '17:45:00', 'NORMAL',         'EARLY',         NOW(), NOW()),
(20, 1, '2026-07-16', '08:50:00', NULL,        'NORMAL',         NULL,            NOW(), NOW());

-- 为 employee 21 (陈思宇) 创建含缺卡的记录（用于测试补卡 7.x）
INSERT IGNORE INTO `attendance_record` (`employee_id`, `group_id`, `attendance_date`, `punch_in_time`, `punch_out_time`, `punch_in_status`, `punch_out_status`, `create_time`, `update_time`) VALUES
(21, 1, '2026-07-14', NULL,        '18:05:00', 'MISSING_PUNCH',  'NORMAL',        NOW(), NOW()),
(21, 1, '2026-07-15', '09:00:00', NULL,        'NORMAL',         'MISSING_PUNCH', NOW(), NOW()),
(21, 1, '2026-07-16', '10:30:00', NULL,        'ABSENT_HALF_DAY', NULL,           NOW(), NOW());

-- employee 162 (李四) 正常打卡
INSERT IGNORE INTO `attendance_record` (`employee_id`, `group_id`, `attendance_date`, `punch_in_time`, `punch_out_time`, `punch_in_status`, `punch_out_status`, `create_time`, `update_time`) VALUES
(162, 1, '2026-07-15', '09:00:00', '18:00:00', 'NORMAL', 'NORMAL', NOW(), NOW()),
(162, 1, '2026-07-16', '09:00:00', '18:00:00', 'NORMAL', 'NORMAL', NOW(), NOW());


-- ══════════════════════════════════════════════════════════════
-- 5. 假期余额 (Leave Balance) — 测试 9.2-9.3 接口
--    GET  /api/v1/leave/balance/{employeeId}      → 查询余额
--    POST /api/v1/leave/balance/annual/init       → 初始化年假
-- ══════════════════════════════════════════════════════════════

-- admin(employee 19) 的年假和调休余额
INSERT IGNORE INTO `leave_balance` (`id`, `employee_id`, `leave_type`, `total_days`, `used_days`, `remaining_days`, `year`, `create_time`, `update_time`) VALUES
(2, 19, 1, 10.0, 2.0, 8.0, 2026, NOW(), NOW()),
(3, 19, 2, 5.0,  0.0, 5.0, 2026, NOW(), NOW());

-- 王浩然(employee 20) 的年假
INSERT IGNORE INTO `leave_balance` (`id`, `employee_id`, `leave_type`, `total_days`, `used_days`, `remaining_days`, `year`, `create_time`, `update_time`) VALUES
(4, 20, 1, 5.0,  0.0, 5.0, 2026, NOW(), NOW()),
(5, 20, 2, 2.0,  0.0, 2.0, 2026, NOW(), NOW());

-- 陈思宇(employee 21) 年假
INSERT IGNORE INTO `leave_balance` (`id`, `employee_id`, `leave_type`, `total_days`, `used_days`, `remaining_days`, `year`, `create_time`, `update_time`) VALUES
(6, 21, 1, 15.0, 3.0, 12.0, 2026, NOW(), NOW()),
(7, 21, 2, 3.0,  0.0, 3.0,  2026, NOW(), NOW());


-- ══════════════════════════════════════════════════════════════
-- 6. 加班记录 (Overtime Record) — 测试 8.1 调休接口
--    POST /api/v1/comp-leave/convert/{employeeId} → 加班折算调休
-- ══════════════════════════════════════════════════════════════

INSERT IGNORE INTO `overtime_record` (`id`, `employee_id`, `overtime_date`, `start_time`, `end_time`, `hours`, `converted_to_comp`, `create_time`) VALUES
(1, 19, '2026-07-13', '2026-07-13 18:00:00', '2026-07-13 22:00:00', 4.0,  0, NOW()),
(2, 19, '2026-07-14', '2026-07-14 18:00:00', '2026-07-14 21:00:00', 3.0,  0, NOW()),
(3, 20, '2026-07-12', '2026-07-12 09:00:00', '2026-07-12 18:00:00', 8.0,  0, NOW());


-- ══════════════════════════════════════════════════════════════
-- 7. 薪资方案 (Salary Plan) — 测试 20.x 接口
--    GET    /api/v1/salary/plans      → 列表
--    POST   /api/v1/salary/plans      → 创建
--    GET    /api/v1/salary/plans/{id} → 详情
--    PUT    /api/v1/salary/plans/{id} → 更新
-- ══════════════════════════════════════════════════════════════

INSERT IGNORE INTO `salary_plan` (`id`, `plan_name`, `description`, `status`, `create_time`, `update_time`) VALUES
(1, '标准薪资方案', '适用于正式员工的标准薪资结构', 1, NOW(), NOW());


-- ══════════════════════════════════════════════════════════════
-- 8. 工作日历 (Work Calendar) — 测试 12.x 接口
--    GET    /api/v1/calendar          → 查询
--    POST   /api/v1/calendar/batch    → 批量保存
--    DELETE /api/v1/calendar?date=    → 删除
-- ══════════════════════════════════════════════════════════════

INSERT IGNORE INTO `work_calendar` (`id`, `calendar_date`, `day_type`, `name`, `year`, `create_time`) VALUES
(1, '2026-10-01', 1, '国庆节',      2026, NOW()),
(2, '2026-10-02', 1, '国庆节',      2026, NOW()),
(3, '2026-10-03', 1, '国庆节',      2026, NOW()),
(4, '2026-10-10', 2, '国庆调班补班', 2026, NOW());


-- ══════════════════════════════════════════════════════════════
-- 9. 验证：列出所有测试数据统计
-- ══════════════════════════════════════════════════════════════

SELECT '======== 测试数据统计 ========' AS '';
SELECT CONCAT('department:        ', COUNT(*), ' 条') FROM `department`;
SELECT CONCAT('position:          ', COUNT(*), ' 条') FROM `position`;
SELECT CONCAT('attendance_group:  ', COUNT(*), ' 条') FROM `attendance_group`;
SELECT CONCAT('attendance_record: ', COUNT(*), ' 条') FROM `attendance_record`;
SELECT CONCAT('leave_balance:     ', COUNT(*), ' 条') FROM `leave_balance`;
SELECT CONCAT('overtime_record:   ', COUNT(*), ' 条') FROM `overtime_record`;
SELECT CONCAT('salary_plan:       ', COUNT(*), ' 条') FROM `salary_plan`;
SELECT CONCAT('work_calendar:     ', COUNT(*), ' 条') FROM `work_calendar`;
SELECT CONCAT('grade_salary_range:', COUNT(*), ' 条') FROM `grade_salary_range`;

SELECT '======== 考勤记录明细 ========' AS '';
SELECT id, employee_id, attendance_date, punch_in_status, punch_out_status
FROM `attendance_record` ORDER BY employee_id, attendance_date;

SELECT 'phase15-test-data 执行完成' AS result;
