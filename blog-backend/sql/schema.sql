-- Blog System Database Schema

CREATE DATABASE IF NOT EXISTS blog_forum DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE blog_forum;

-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    bio TEXT,
    role VARCHAR(20) DEFAULT 'user',
    github_id VARCHAR(50) DEFAULT NULL COMMENT 'GitHub用户ID',
    gitee_id VARCHAR(50) DEFAULT NULL COMMENT 'Gitee用户ID',
    banned TINYINT(1) DEFAULT 0 COMMENT '是否被封禁',
    activated TINYINT(1) DEFAULT 0,
    created_at VARCHAR(30),
    updated_at VARCHAR(30),
    INDEX idx_github_id (github_id),
    INDEX idx_gitee_id (gitee_id)
);

-- Categories table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    created_at VARCHAR(30)
);

-- Tags table
CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at VARCHAR(30)
);

-- Articles table
CREATE TABLE articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT,
    category_id BIGINT,
    status VARCHAR(20) DEFAULT 'draft',
    view_count INT DEFAULT 0,
    created_at VARCHAR(30),
    updated_at VARCHAR(30),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_articles_created_at (created_at),
    INDEX idx_articles_status (status)
);

-- Article-Tags relation table
CREATE TABLE article_tags (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Comments table
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    like_count INT DEFAULT 0,
    created_at VARCHAR(30),
    updated_at VARCHAR(30),
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- Comment likes table
CREATE TABLE comment_likes (
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at VARCHAR(30),
    PRIMARY KEY (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Follows table
CREATE TABLE follows (
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at VARCHAR(30),
    PRIMARY KEY (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Files table
CREATE TABLE files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(255),
    file_size BIGINT,
    file_type VARCHAR(100),
    created_at VARCHAR(30),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insert default admin user (password: admin123)
INSERT INTO users (username, password, email, role, created_at, updated_at)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@blog.com', 'admin', NOW(), NOW());

-- Insert sample categories
INSERT INTO categories (name, description, created_at) VALUES
('技术', '技术相关文章', NOW()),
('生活', '生活随笔', NOW()),
('教程', '教程和指南', NOW());

-- Insert sample tags
INSERT INTO tags (name, created_at) VALUES
('Java', NOW()),
('Spring Boot', NOW()),
('Vue', NOW()),
('前端', NOW()),
('后端', NOW());

-- Quiz banks table
CREATE TABLE IF NOT EXISTS quiz_banks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    question_count INT DEFAULT 0,
    created_at VARCHAR(30),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_quiz_user_id (user_id),
    INDEX idx_quiz_created_at (created_at)
);

-- Questions table
CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_bank_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    question LONGTEXT NOT NULL,
    options LONGTEXT,
    answer LONGTEXT NOT NULL,
    explanation LONGTEXT,
    sort_order INT DEFAULT 0,
    FOREIGN KEY (quiz_bank_id) REFERENCES quiz_banks(id) ON DELETE CASCADE,
    INDEX idx_question_bank_id (quiz_bank_id),
    INDEX idx_question_sort (sort_order)
);

-- Albums table
CREATE TABLE IF NOT EXISTS albums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    cover_url VARCHAR(500),
    is_public TINYINT(1) DEFAULT 0,
    is_anonymous TINYINT(1) DEFAULT 0,
    media_count INT DEFAULT 0,
    created_at VARCHAR(30),
    updated_at VARCHAR(30),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_album_user_id (user_id),
    INDEX idx_album_public (is_public)
);

-- Media table (photos and videos)
CREATE TABLE IF NOT EXISTS media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    album_id BIGINT,
    title VARCHAR(255),
    description TEXT,
    url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    type VARCHAR(20) NOT NULL,
    file_size BIGINT,
    is_public TINYINT(1) DEFAULT 0,
    is_anonymous TINYINT(1) DEFAULT 0,
    source VARCHAR(20) DEFAULT 'user',
    created_at VARCHAR(30),
    updated_at VARCHAR(30),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE SET NULL,
    INDEX idx_media_user_id (user_id),
    INDEX idx_media_album_id (album_id),
    INDEX idx_media_public (is_public),
    INDEX idx_media_type (type)
);

-- Site visits statistics table
CREATE TABLE IF NOT EXISTS site_visits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_date VARCHAR(10) NOT NULL UNIQUE,
    pv INT DEFAULT 0,
    uv INT DEFAULT 0,
    new_users INT DEFAULT 0,
    new_articles INT DEFAULT 0,
    new_comments INT DEFAULT 0,
    INDEX idx_visit_date (visit_date)
);

-- Conversations table (私信会话)
CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    last_message_id BIGINT,
    last_message_time VARCHAR(30),
    user1_unread INT DEFAULT 0,
    user2_unread INT DEFAULT 0,
    created_at VARCHAR(30),
    FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_conversation (user1_id, user2_id),
    INDEX idx_conv_user1 (user1_id),
    INDEX idx_conv_user2 (user2_id)
);

-- Messages table (私信消息)
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT,
    type VARCHAR(20) DEFAULT 'text',
    file_url VARCHAR(500),
    file_name VARCHAR(255),
    is_read TINYINT(1) DEFAULT 0,
    created_at VARCHAR(30),
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_msg_conv (conversation_id),
    INDEX idx_msg_sender (sender_id),
    INDEX idx_msg_receiver (receiver_id),
    INDEX idx_msg_time (created_at)
);

-- Site info table (网站信息)
CREATE TABLE IF NOT EXISTS site_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_name VARCHAR(100) DEFAULT '墨香阁',
    site_description TEXT,
    site_keywords VARCHAR(255) DEFAULT '博客,技术,分享',
    site_logo VARCHAR(500),
    site_favicon VARCHAR(500),
    icp_number VARCHAR(50),
    police_number VARCHAR(50),
    contact_email VARCHAR(100),
    contact_phone VARCHAR(20),
    contact_address VARCHAR(255),
    contact_qq VARCHAR(20),
    contact_wechat VARCHAR(50),
    github_url VARCHAR(255),
    gitee_url VARCHAR(255),
    created_at VARCHAR(30),
    updated_at VARCHAR(30)
);

-- Announcements table (公告板)
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    type VARCHAR(20) DEFAULT 'info',
    is_pinned TINYINT(1) DEFAULT 0,
    is_active TINYINT(1) DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at VARCHAR(30),
    updated_at VARCHAR(30),
    INDEX idx_announcement_active (is_active),
    INDEX idx_announcement_pinned (is_pinned),
    INDEX idx_announcement_sort (sort_order),
    INDEX idx_announcement_created_by (created_by)
);

-- Insert default site info
INSERT INTO site_info (site_name, site_description, site_keywords, contact_email, created_at, updated_at) VALUES
('墨香阁', '一个优雅的博客系统，分享技术与生活', '博客,技术,分享,Vue,Spring Boot', 'admin@blog.com', NOW(), NOW());

-- Insert sample announcements
INSERT INTO announcements (title, content, type, is_pinned, created_by, created_at, updated_at) VALUES
('欢迎来到墨香阁', '欢迎大家来到墨香阁博客系统！这里是一个分享技术与生活的平台，希望大家能够在这里找到有价值的内容。', 'info', 1, 1, NOW(), NOW()),
('网站功能介绍', '本站支持文章发布、评论互动、私信聊天、相册分享、刷题练习等功能，欢迎大家体验使用！', 'success', 0, 1, NOW(), NOW());

-- Recitation table (背诵记录)
CREATE TABLE IF NOT EXISTS recitation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100),
    content LONGTEXT NOT NULL,
    user_input LONGTEXT,
    progress INT DEFAULT 0,
    duration INT DEFAULT 0,
    timing_enabled TINYINT(1) DEFAULT 0,
    completed TINYINT(1) DEFAULT 0,
    created_at VARCHAR(30),
    updated_at VARCHAR(30),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_recitation_user (user_id),
    INDEX idx_recitation_completed (completed)
);
