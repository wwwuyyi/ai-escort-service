-- ============================================
-- 第四批修复：订单备注字段（BUG-042）
-- 说明：01-schema.sql 已包含 escort_order.remark；本脚本用于兼容早期旧库。
-- 幂等：通过 information_schema 判断，列已存在时跳过；可重复执行。
-- 升级顺序：01 → 02 → 03 → 04 → 05 → 06 → 本脚本 07
-- ============================================

USE escort_db;

SET @db := DATABASE();
SET @has_remark := (SELECT COUNT(*)
                      FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = @db
                       AND TABLE_NAME = 'escort_order'
                       AND COLUMN_NAME = 'remark');
SET @sql := IF(@has_remark = 0,
    'ALTER TABLE escort_order ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT ''订单备注'' AFTER refund_amount',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证（可选）：
-- SHOW COLUMNS FROM escort_order LIKE 'remark';
