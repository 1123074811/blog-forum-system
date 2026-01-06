-- Blog System Database Schema

CREATE DATABASE IF NOT EXISTS blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE blog;

-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    avatar VARCHAR(255),
    bio TEXT,
    role VARCHAR(20) DEFAULT 'user',
    created_at VARCHAR(30),
    updated_at VARCHAR(30)
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
