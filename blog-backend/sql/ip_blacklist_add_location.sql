-- ============================================
-- IP blacklist: add location column
-- ============================================

USE blog_forum;

-- Add location column if not exists
SET @db := DATABASE();

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = @db AND table_name = 'ip_blacklist' AND column_name = 'location');
SET @sql := IF(@exists = 0,
               'ALTER TABLE ip_blacklist ADD COLUMN location VARCHAR(255) NOT NULL DEFAULT '''' AFTER reason',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Normalize the column
ALTER TABLE ip_blacklist
    MODIFY COLUMN location VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'IP地理位置';
