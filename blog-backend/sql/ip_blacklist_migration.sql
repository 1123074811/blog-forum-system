-- ============================================
-- IP blacklist table migration (MySQL-compatible)
-- Purpose:
-- 1) Persist banned IPs in DB
-- 2) Unban by deleting the corresponding record
-- ============================================

USE blog_forum;

-- 1) Create table for fresh deployments
CREATE TABLE IF NOT EXISTS ip_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip VARCHAR(64) NOT NULL,
    reason VARCHAR(500) NOT NULL DEFAULT 'security_policy',
    admin_id BIGINT NULL,
    ban_type TINYINT NOT NULL DEFAULT 1 COMMENT '1=permanent,2=temporary',
    expire_time DATETIME NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=active,0=inactive',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_ip (ip),
    INDEX idx_status_expire (status, expire_time)
);

-- 2) For existing tables: add missing columns in a version-compatible way
SET @db := DATABASE();

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = @db AND table_name = 'ip_blacklist' AND column_name = 'reason');
SET @sql := IF(@exists = 0,
               'ALTER TABLE ip_blacklist ADD COLUMN reason VARCHAR(500) NOT NULL DEFAULT ''security_policy''',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = @db AND table_name = 'ip_blacklist' AND column_name = 'admin_id');
SET @sql := IF(@exists = 0,
               'ALTER TABLE ip_blacklist ADD COLUMN admin_id BIGINT NULL',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = @db AND table_name = 'ip_blacklist' AND column_name = 'ban_type');
SET @sql := IF(@exists = 0,
               'ALTER TABLE ip_blacklist ADD COLUMN ban_type TINYINT NOT NULL DEFAULT 1 COMMENT ''1=permanent,2=temporary''',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = @db AND table_name = 'ip_blacklist' AND column_name = 'expire_time');
SET @sql := IF(@exists = 0,
               'ALTER TABLE ip_blacklist ADD COLUMN expire_time DATETIME NULL',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = @db AND table_name = 'ip_blacklist' AND column_name = 'status');
SET @sql := IF(@exists = 0,
               'ALTER TABLE ip_blacklist ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT ''1=active,0=inactive''',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = @db AND table_name = 'ip_blacklist' AND column_name = 'created_at');
SET @sql := IF(@exists = 0,
               'ALTER TABLE ip_blacklist ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = @db AND table_name = 'ip_blacklist' AND column_name = 'updated_at');
SET @sql := IF(@exists = 0,
               'ALTER TABLE ip_blacklist ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Normalize column definitions
ALTER TABLE ip_blacklist
    MODIFY COLUMN ip VARCHAR(64) NOT NULL,
    MODIFY COLUMN reason VARCHAR(500) NOT NULL DEFAULT 'security_policy',
    MODIFY COLUMN admin_id BIGINT NULL,
    MODIFY COLUMN ban_type TINYINT NOT NULL DEFAULT 1 COMMENT '1=permanent,2=temporary',
    MODIFY COLUMN expire_time DATETIME NULL,
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '1=active,0=inactive',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 4) Add missing indexes in a version-compatible way
SET @exists := (SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = @db AND table_name = 'ip_blacklist' AND index_name = 'uk_ip');
SET @sql := IF(@exists = 0,
               'ALTER TABLE ip_blacklist ADD UNIQUE INDEX uk_ip (ip)',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = @db AND table_name = 'ip_blacklist' AND index_name = 'idx_status_expire');
SET @sql := IF(@exists = 0,
               'ALTER TABLE ip_blacklist ADD INDEX idx_status_expire (status, expire_time)',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
