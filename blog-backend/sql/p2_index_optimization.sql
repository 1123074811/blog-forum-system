-- ============================================
-- P2: 数据库索引审计与优化
-- 清理重复索引 + 添加缺失索引
-- ============================================

DELIMITER $$

-- 安全删除索引（存在则删）
CREATE PROCEDURE IF NOT EXISTS `safe_drop_index`(IN tbl VARCHAR(64), IN idx VARCHAR(64))
BEGIN
    DECLARE idx_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_exists FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx;
    IF idx_exists > 0 THEN
        SET @stmt = CONCAT('ALTER TABLE `', tbl, '` DROP INDEX `', idx, '`');
        PREPARE stmt FROM @stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

-- 安全创建索引（不存在则建）
CREATE PROCEDURE IF NOT EXISTS `safe_add_index`(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols TEXT)
BEGIN
    DECLARE idx_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_exists FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx;
    IF idx_exists = 0 THEN
        SET @stmt = CONCAT('ALTER TABLE `', tbl, '` ADD INDEX `', idx, '` (', cols, ')');
        PREPARE stmt FROM @stmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- 1. 清理 articles 表重复索引
CALL safe_drop_index('articles', 'idx_article_search');
CALL safe_drop_index('articles', 'idx_article_status_created');
CALL safe_drop_index('articles', 'idx_article_user_status');

-- 2. messages 表: 私信查询高频索引
CALL safe_add_index('messages', 'idx_conversation_created', 'conversation_id, created_at DESC');
CALL safe_add_index('messages', 'idx_sender_receiver', 'sender_id, receiver_id');
CALL safe_add_index('messages', 'idx_receiver_read', 'receiver_id, is_read');

-- 3. notifications 表: 通知查询索引
CALL safe_add_index('notifications', 'idx_user_read', 'user_id, is_read');
CALL safe_add_index('notifications', 'idx_user_created', 'user_id, created_at DESC');

-- 4. comments 表: 嵌套评论查询
CALL safe_add_index('comments', 'idx_parent_id', 'parent_id');

-- 5. media 表: 用户相册媒体查询
CALL safe_add_index('media', 'idx_user_album', 'user_id, album_id');
CALL safe_add_index('media', 'idx_album_created', 'album_id, created_at DESC');

-- 6. follows 表: 反向关注查询
CALL safe_add_index('follows', 'idx_following', 'following_id');

-- 7. conversations 表: 会话查询
CALL safe_add_index('conversations', 'idx_user1_user2', 'user1_id, user2_id');

-- 8. tree_hole 表: 按时间排序
CALL safe_add_index('tree_hole', 'idx_created_at', 'created_at DESC');

-- 9. recitation 表: 用户背诵记录查询
CALL safe_add_index('recitation', 'idx_user_created', 'user_id, created_at DESC');

-- 10. quiz_banks 表: 公开题库查询
CALL safe_add_index('quiz_banks', 'idx_public_created', 'is_public, created_at DESC');
CALL safe_add_index('quiz_banks', 'idx_user_created', 'user_id, created_at DESC');

-- 11. questions 表: 题库题目查询
CALL safe_add_index('questions', 'idx_quiz_bank', 'quiz_bank_id');

-- 12. security_event 表: 安全审计查询
CALL safe_add_index('security_event', 'idx_user_id', 'user_id');
CALL safe_add_index('security_event', 'idx_created_at', 'created_at DESC');
CALL safe_add_index('security_event', 'idx_ip', 'ip');

-- 13. admin_ban 表: 管理员操作查询
CALL safe_add_index('admin_ban', 'idx_admin_id', 'admin_id');
CALL safe_add_index('admin_ban', 'idx_user_id', 'user_id');

-- 14. operation_log 表
CALL safe_add_index('operation_log', 'idx_user_id', 'user_id');
CALL safe_add_index('operation_log', 'idx_created_at', 'created_at DESC');

-- 15. files 表
CALL safe_add_index('files', 'idx_user_id', 'user_id');
CALL safe_add_index('files', 'idx_file_type', 'file_type');

-- 清理存储过程
DROP PROCEDURE IF EXISTS safe_drop_index;
DROP PROCEDURE IF EXISTS safe_add_index;
