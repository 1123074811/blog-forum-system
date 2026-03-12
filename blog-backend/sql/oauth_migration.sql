-- OAuth 登录功能数据库迁移脚本
-- 执行日期: 2026-03-06

USE blog_forum;

-- 1. 添加 OAuth 相关字段
ALTER TABLE users 
ADD COLUMN github_id VARCHAR(50) DEFAULT NULL COMMENT 'GitHub用户ID',
ADD COLUMN gitee_id VARCHAR(50) DEFAULT NULL COMMENT 'Gitee用户ID';

-- 2. 为 OAuth 字段添加索引（提高查询性能）
ALTER TABLE users 
ADD INDEX idx_github_id (github_id),
ADD INDEX idx_gitee_id (gitee_id);

-- 3. 修改 email 和 password 字段允许为空（OAuth 用户可能没有邮箱和密码）
ALTER TABLE users 
MODIFY COLUMN email VARCHAR(100) DEFAULT NULL,
MODIFY COLUMN password VARCHAR(255) DEFAULT NULL;

-- 4. 添加 banned 字段（如果不存在）
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS banned TINYINT(1) DEFAULT 0 COMMENT '是否被封禁';

-- 5. 验证修改结果
DESCRIBE users;

-- 6. 查看现有的 OAuth 用户（如果有）
SELECT id, username, nickname, avatar, github_id, gitee_id, created_at 
FROM users 
WHERE github_id IS NOT NULL OR gitee_id IS NOT NULL;
