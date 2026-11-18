-- ============================================
-- 安全修复批次 1：令牌版本号 + 订单幂等键按家属隔离
-- 适用版本：已执行 01~05 的 escort_db
-- 幂等性：本脚本通过 information_schema 判断后执行，可重复运行。
-- 升级顺序：01-schema.sql → 02-init-data.sql → 03 → 04 → 05 → 本脚本 06
-- ============================================

USE escort_db;

-- 1) sys_user 增加 token_version：
--    登录时写入 JWT 的 tv 声明；登出 / 修改密码 / 账号被禁用时递增，
--    拦截器比对数据库中的版本号，从而让旧 Token 立即失效。
SET @db := DATABASE();
SET @has_col := (SELECT COUNT(*)
                   FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = @db
                    AND TABLE_NAME = 'sys_user'
                    AND COLUMN_NAME = 'token_version');
SET @sql := IF(@has_col = 0,
    'ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT ''JWT令牌版本号'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 订单幂等唯一键由 (request_id) 改为 (family_id, request_id)：
--    不同家属可以使用相同 requestId 各自下单，同时同一家属同一请求号仍然幂等。
SET @has_old_key := (SELECT COUNT(*)
                       FROM information_schema.STATISTICS
                      WHERE TABLE_SCHEMA = @db
                        AND TABLE_NAME = 'escort_order'
                        AND INDEX_NAME = 'uk_request_id');
SET @sql := IF(@has_old_key > 0,
    'ALTER TABLE escort_order DROP INDEX uk_request_id',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_new_key := (SELECT COUNT(*)
                       FROM information_schema.STATISTICS
                      WHERE TABLE_SCHEMA = @db
                        AND TABLE_NAME = 'escort_order'
                        AND INDEX_NAME = 'uk_family_request');
SET @sql := IF(@has_new_key = 0,
    'ALTER TABLE escort_order ADD UNIQUE KEY uk_family_request (family_id, request_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证（可选）：
-- SHOW COLUMNS FROM sys_user LIKE 'token_version';
-- SHOW INDEX FROM escort_order WHERE Key_name IN ('uk_request_id','uk_family_request');
