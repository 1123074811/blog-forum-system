-- ============================================
-- High-concurrency index optimization script
-- ============================================

CREATE INDEX IF NOT EXISTS idx_status_created ON articles (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_status ON articles (user_id, status);
CREATE INDEX IF NOT EXISTS idx_category_status ON articles (category_id, status);

-- FULLTEXT index
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'articles' AND index_name = 'ft_title_content');
SET @sql := IF(@exist = 0,
    'ALTER TABLE articles ADD FULLTEXT INDEX ft_title_content (title, content) WITH PARSER ngram',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE INDEX IF NOT EXISTS idx_article_id ON article_likes (article_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_article ON article_likes (user_id, article_id);

CREATE INDEX IF NOT EXISTS idx_article_id ON article_favorites (article_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_article ON article_favorites (user_id, article_id);

CREATE INDEX IF NOT EXISTS idx_article_id ON comments (article_id);
CREATE INDEX IF NOT EXISTS idx_created ON comments (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_read ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_user_created ON notifications (user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_username ON users (username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_email ON users (email);
