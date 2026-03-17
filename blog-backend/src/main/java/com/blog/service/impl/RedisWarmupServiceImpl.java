package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.constant.AppConstants;
import com.blog.mapper.ArticleMapper;
import com.blog.pojo.entity.Article;
import com.blog.service.RedisWarmupService;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 数据预热服务实现
 * 实现 ApplicationRunner 接口，在应用启动后自动执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisWarmupServiceImpl implements RedisWarmupService, ApplicationRunner {

    private final ArticleMapper articleMapper;
    private final CacheUtil cacheUtil;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 开始Redis数据预热 ==========");
        try {
            warmupAllCache();
            log.info("========== Redis数据预热完成 ==========");
        } catch (Exception e) {
            log.error("Redis数据预热失败", e);
        }
    }

    @Override
    public void warmupViewCounts() {
        log.info("开始预热文章浏览量数据...");
        long startTime = System.currentTimeMillis();
        int count = 0;

        try {
            // 查询所有已发布的文章
            List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getStatus, "published")
                    .select(Article::getId, Article::getViewCount)
            );

            for (Article article : articles) {
                if (article.getViewCount() != null && article.getViewCount() > 0) {
                    String key = AppConstants.CACHE_ARTICLE_VIEW_PREFIX + article.getId();
                    
                    // 检查Redis中是否已有数据
                    Number existingViews = cacheUtil.get(key);
                    if (existingViews == null) {
                        // 只有Redis中没有数据时才回填
                        cacheUtil.set(key, article.getViewCount(), 30, TimeUnit.DAYS);
                        count++;
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("文章浏览量预热完成，预热 {} 篇文章，耗时 {}ms", count, duration);

        } catch (Exception e) {
            log.error("预热文章浏览量失败", e);
        }
    }

    @Override
    public void warmupAllCache() {
        // 预热浏览量数据
        warmupViewCounts();
        
        // 可以在这里添加其他需要预热的数据
        // 例如：热门文章、分类列表、标签列表等
    }
}
