-- ============================================================
-- Phase 14: 修复 department、position、attendance_record 表缺失字段
-- 运行环境: 已有数据库增量迁移（幂等，可重复执行）
-- 创建日期: 2026-07-16
-- ============================================================

-- 清空已有的校验存储过程
DROP PROCEDURE IF EXISTS `ensure_col_phase14`;

DELIMITER //
CREATE PROCEDURE `ensure_col_phase14`(
    IN tbl     VARCHAR(64),
    IN col     VARCHAR(64),
    IN col_def VARCHAR(512)
)
BEGIN
    DECLARE cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO cnt
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = tbl
      AND COLUMN_NAME  = col;

    IF cnt = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', col_def);
        PREPARE s FROM @ddl;
        EXECUTE s;
        DEALLOCATE PREPARE s;
        SELECT CONCAT('OK: added column ', col, ' to table ', tbl) AS result;
    ELSE
        SELECT CONCAT('SKIP: column ', col, ' already exists in table ', tbl) AS result;
    END IF;
END //
DELIMITER ;

-- ══════════════════════════════════════════════════════════════
-- 1. department 表：补充 description 列
-- ══════════════════════════════════════════════════════════════
CALL `ensure_col_phase14`(
    'department',
    'description',
    'VARCHAR(500) NULL COMMENT ''部门描述/职能说明'' AFTER `manager_id`'
);

-- 如果总公司部门已存在但 description 为空，更新默认值
UPDATE `department`
SET `description` = '公司总部，统筹管理全公司业务'
WHERE `dept_code` = 'ROOT'
  AND (`description` IS NULL OR `description` = '');


-- ══════════════════════════════════════════════════════════════
-- 2. position 表：补充 dept_id 列
-- ══════════════════════════════════════════════════════════════
CALL `ensure_col_phase14`(
    'position',
    'dept_id',
    'BIGINT NULL COMMENT ''所属部门ID（为空=全公司通用）'' AFTER `sequence`'
);


-- ══════════════════════════════════════════════════════════════
-- 3. position 表：补充 is_standard 列
-- ══════════════════════════════════════════════════════════════
CALL `ensure_col_phase14`(
    'position',
    'is_standard',
    'TINYINT NOT NULL DEFAULT 1 COMMENT ''是否标准职位: 1-标准 0-非标准'' AFTER `description`'
);

-- 修复已有职位的 is_standard 为默认值 1（如果之前为 NULL）
UPDATE `position`
SET `is_standard` = 1
WHERE `is_standard` IS NULL;


-- ══════════════════════════════════════════════════════════════
-- 4. attendance_record 表：punch_out_status 改为允许 NULL
--    原因：上班打卡时尚未下班，punch_out_status 应为 NULL
-- ══════════════════════════════════════════════════════════════
ALTER TABLE `attendance_record`
    MODIFY COLUMN `punch_out_status` VARCHAR(32) DEFAULT NULL
    COMMENT '下班打卡状态: NORMAL/EARLY/MISSING_PUNCH/ABSENT_HALF_DAY（上班打卡时为空）';


-- ══════════════════════════════════════════════════════════════
-- 5. 插入补卡测试数据（管理员 employeeId=1 的缺卡记录）
--    用于测试 7.1 发起补卡申请 和 7.2 审批补卡 流程
-- ══════════════════════════════════════════════════════════════
INSERT IGNORE INTO `attendance_record`
    (employee_id, group_id, attendance_date, punch_in_time, punch_out_time, punch_in_status, punch_out_status, create_time, update_time)
VALUES
    (1, 1, '2026-07-15', NULL, NULL, 'MISSING_PUNCH', 'MISSING_PUNCH', NOW(), NOW());


-- ══════════════════════════════════════════════════════════════
-- 6. 验证结果
-- ══════════════════════════════════════════════════════════════

-- 检查 department 表结构
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'department'
ORDER BY ORDINAL_POSITION;

-- 检查 position 表结构
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'position'
ORDER BY ORDINAL_POSITION;

-- 检查种子数据
SELECT 'department' AS tbl, id, dept_name, description FROM `department`;
SELECT 'position'   AS tbl, id, position_name, position_code, sequence, dept_id, is_standard, status FROM `position`;

-- 检查 attendance_record punch_out_status 是否已改为可空
SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'attendance_record'
  AND COLUMN_NAME = 'punch_out_status';

-- ══════════════════════════════════════════════════════════════
-- 清理
-- ══════════════════════════════════════════════════════════════
DROP PROCEDURE IF EXISTS `ensure_col_phase14`;

SELECT 'phase14-fix-position-dept 执行完成' AS result;
