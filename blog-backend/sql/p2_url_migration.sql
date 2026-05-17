-- ============================================
-- P2: 图片URL迁移脚本
-- 将历史存储的完整MinIO URL替换为对象key或新CDN URL
-- 使用方法：根据实际部署修改 @OLD_PREFIX 和 @NEW_PREFIX
-- ============================================

-- 1. 查看当前存储的完整URL示例（审计先行）
-- SELECT id, url, thumbnail_url FROM media WHERE url LIKE 'http://%' LIMIT 10;
-- SELECT id, file_path FROM files WHERE file_path LIKE 'http://%' LIMIT 10;

-- 2. media 表 URL 迁移（将旧MinIO URL替换为新CDN域名）
-- 用法: 将 'http://localhost:9000/blog/' 替换为 'https://cdn.yourdomain.com/'
-- UPDATE media
-- SET url = REPLACE(url, 'http://localhost:9000/blog/', 'https://cdn.yourdomain.com/'),
--     thumbnail_url = REPLACE(thumbnail_url, 'http://localhost:9000/blog/', 'https://cdn.yourdomain.com/')
-- WHERE url LIKE 'http://localhost:9000/blog/%';

-- 3. files 表路径迁移
-- UPDATE files
-- SET file_path = REPLACE(file_path, 'http://localhost:9000/blog/', 'https://cdn.yourdomain.com/')
-- WHERE file_path LIKE 'http://localhost:9000/blog/%';

-- 4. 验证迁移结果
-- SELECT COUNT(*) AS remaining_old_urls FROM media WHERE url LIKE 'http://localhost:9000/%';
-- SELECT COUNT(*) AS remaining_old_paths FROM files WHERE file_path LIKE 'http://localhost:9000/%';
