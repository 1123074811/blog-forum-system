package com.blog.service.impl;

import com.blog.constant.AppConstants;
import com.blog.mapper.ArticleMapper;
import com.blog.pojo.entity.Article;
import com.blog.service.ViewCountSyncService;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 浏览量同步服务实现
 * 定时将 Redis 中的浏览量批量同步到数据库，避免频繁写库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountSyncServiceImpl implements ViewCountSyncService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ArticleMapper articleMapper;
    private final CacheUtil cacheUtil;

    /**
     * 定时同步浏览量（每分钟执行一次）
     */
    @Override
    @Scheduled(fixedRate = 60000)
    public void syncAllViewCounts() {
        log.info("开始同步文章浏览量到数据库...");
        long startTime = System.currentTimeMillis();
        int syncCount = 0;

        try {
            // 获取所有浏览量缓存的 key
            Set<String> keys = redisTemplate.keys(AppConstants.CACHE_ARTICLE_VIEW_PREFIX + "*");

            if (keys == null || keys.isEmpty()) {
                log.debug("没有需要同步的浏览量数据");
                return;
            }

            // 批量同步
            for (String key : keys) {
                try {
                    // 提取文章ID
                    String articleIdStr = key.replace(AppConstants.CACHE_ARTICLE_VIEW_PREFIX, "");
                    Long articleId = Long.parseLong(articleIdStr);

                    // 获取 Redis 中的浏览量
                    Number viewCount = cacheUtil.get(key);
                    if (viewCount == null) {
                        continue;
                    }

                    // 更新数据库
                    Article article = articleMapper.selectById(articleId);
                    if (article != null) {
                        article.setViewCount(viewCount.intValue());
                        articleMapper.updateById(article);
                        syncCount++;
                    }

                } catch (Exception e) {
                    log.error("同步文章浏览量失败，key: {}", key, e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("浏览量同步完成，同步 {} 篇文章，耗时 {}ms", syncCount, duration);

        } catch (Exception e) {
            log.error("批量同步浏览量失败", e);
        }
    }

    /**
     * 同步单篇文章的浏览量
     */
    @Override
    public void syncViewCount(Long articleId) {
        try {
            String key = AppConstants.CACHE_ARTICLE_VIEW_PREFIX + articleId;
            Number viewCount = cacheUtil.get(key);

            if (viewCount != null) {
                Article article = articleMapper.selectById(articleId);
                if (article != null) {
                    article.setViewCount(viewCount.intValue());
                    articleMapper.updateById(article);
                    log.debug("同步文章 {} 的浏览量: {}", articleId, viewCount);
                }
            }
        } catch (Exception e) {
            log.error("同步文章浏览量失败，文章ID: {}", articleId, e);
        }
    }
}
