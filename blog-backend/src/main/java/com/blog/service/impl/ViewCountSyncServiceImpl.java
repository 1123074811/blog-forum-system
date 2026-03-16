package com.blog.service.impl;

import com.blog.constant.AppConstants;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ViewCountSyncService;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

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
     * 定时同步浏览量（每5分钟执行一次，减少数据库写压力）
     */
    @Override
    @Scheduled(fixedRate = 300000)
    public void syncAllViewCounts() {
        long startTime = System.currentTimeMillis();

        try {
            Set<String> keys = redisTemplate.keys(AppConstants.CACHE_ARTICLE_VIEW_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            // 批量读取 Redis 值，减少网络往返
            List<Object> values = redisTemplate.opsForValue().multiGet(new ArrayList<>(keys));
            List<Map<String, Object>> batchList = new ArrayList<>();

            int i = 0;
            for (String key : keys) {
                Object val = values != null ? values.get(i++) : null;
                if (val == null) continue;
                try {
                    Long articleId = Long.parseLong(key.replace(AppConstants.CACHE_ARTICLE_VIEW_PREFIX, ""));
                    Map<String, Object> item = new HashMap<>();
                    item.put("articleId", articleId);
                    item.put("viewCount", ((Number) val).intValue());
                    batchList.add(item);
                } catch (Exception e) {
                    log.warn("解析浏览量 key 失败: {}", key);
                }
            }

            if (!batchList.isEmpty()) {
                // 单次批量 UPDATE，替代逐条更新
                articleMapper.batchUpdateViewCount(batchList);
                log.info("浏览量批量同步完成，共 {} 篇，耗时 {}ms",
                        batchList.size(), System.currentTimeMillis() - startTime);
            }

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
                Map<String, Object> item = new HashMap<>();
                item.put("articleId", articleId);
                item.put("viewCount", viewCount.intValue());
                articleMapper.batchUpdateViewCount(List.of(item));
            }
        } catch (Exception e) {
            log.error("同步文章浏览量失败，文章ID: {}", articleId, e);
        }
    }
}
