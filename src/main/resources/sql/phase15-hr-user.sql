-- ============================================================
-- Phase 15 补充: 创建 HR 审批用户
-- 用途: 测试审批流程（请假审批、补卡审批、薪资审批等）
-- 密码: 123456
-- ============================================================

-- 删除旧 hruser 记录（如果存在且有问题）
DELETE FROM `sys_user_role` WHERE `user_id` = 2;
DELETE FROM `sys_user` WHERE `id` = 2;

-- 1. 创建 HR 用户
INSERT INTO `sys_user` (`id`, `username`, `password`, `status`, `login_fail_count`, `force_change_pwd`, `create_time`, `update_time`)
VALUES (2, 'hruser', '$2b$10$JV7eDP757D3tpf6teeUAsuxR3Ac/FqkeW4EpLPtsRTIN07Y9.ltHy', 1, 0, 0, NOW(), NOW());

-- 2. 分配 ROLE_HR 角色（role_id=2）
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (2, 2);

-- 3. 关联员工档案：王浩然(employee_id=20) → hruser(user_id=2)
UPDATE `employee` SET `user_id` = 2 WHERE `id` = 20;

-- 4. 验证
SELECT 'HR 用户创建完成' AS result;
SELECT u.id, u.username, u.status, r.role_code
FROM `sys_user` u
LEFT JOIN `sys_user_role` ur ON u.id = ur.user_id
LEFT JOIN `sys_role` r ON ur.role_id = r.id
WHERE u.id = 2;
