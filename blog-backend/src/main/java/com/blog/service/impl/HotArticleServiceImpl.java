package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.constant.AppConstants;
import com.blog.mapper.*;
import com.blog.pojo.entity.*;
import com.blog.service.HotArticleService;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotArticleServiceImpl implements HotArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleFavoriteMapper articleFavoriteMapper;
    private final CommentMapper commentMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserMapper userMapper;
    private final CacheUtil cacheUtil;

    private static final String HOT_ARTICLES_KEY = "hot:articles";
    private static final String HOT_ARTICLES_CATEGORY_PREFIX = "hot:articles:category:";

    /**
     * 定时更新热门文章排行榜（每5分钟执行一次）
     */
    @Override
    @Scheduled(fixedRate = 300000)
    public void updateHotArticles() {
        log.info("开始更新热门文章排行榜...");
        long startTime = System.currentTimeMillis();

        try {
            // 查询所有已发布的文章
            List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, "published")
            );

            if (articles.isEmpty()) {
                log.info("没有已发布的文章，跳过更新");
                return;
            }

            // 批量查询统计数据
            List<Long> articleIds = articles.stream().map(Article::getId).toList();

            // 点赞数统计
            Map<Long, Long> likeCountMap = articleLikeMapper.selectList(
                new LambdaQueryWrapper<ArticleLike>().in(ArticleLike::getArticleId, articleIds)
            ).stream().collect(Collectors.groupingBy(ArticleLike::getArticleId, Collectors.counting()));

            // 收藏数统计
            Map<Long, Long> favoriteCountMap = articleFavoriteMapper.selectList(
                new LambdaQueryWrapper<ArticleFavorite>().in(ArticleFavorite::getArticleId, articleIds)
            ).stream().collect(Collectors.groupingBy(ArticleFavorite::getArticleId, Collectors.counting()));

            // 评论数统计
            Map<Long, Long> commentCountMap = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().in(Comment::getArticleId, articleIds)
            ).stream().collect(Collectors.groupingBy(Comment::getArticleId, Collectors.counting()));

            // 计算热度分数并存入 Redis ZSet
            long currentTime = System.currentTimeMillis();
            Map<Long, Set<ZSetOperations.TypedTuple<Object>>> categoryScores = new HashMap<>();

            for (Article article : articles) {
                double score = calculateHotScore(
                    article,
                    likeCountMap.getOrDefault(article.getId(), 0L),
                    favoriteCountMap.getOrDefault(article.getId(), 0L),
                    commentCountMap.getOrDefault(article.getId(), 0L),
                    currentTime
                );

                // 存入全局热门榜
                redisTemplate.opsForZSet().add(HOT_ARTICLES_KEY, article.getId(), score);

                // 存入分类热门榜
                if (article.getCategoryId() != null) {
                    String categoryKey = HOT_ARTICLES_CATEGORY_PREFIX + article.getCategoryId();
                    categoryScores.computeIfAbsent(article.getCategoryId(), k -> new HashSet<>())
                        .add(ZSetOperations.TypedTuple.of(article.getId(), score));
                }
            }

            // 批量写入分类热门榜
            categoryScores.forEach((categoryId, tuples) -> {
                String categoryKey = HOT_ARTICLES_CATEGORY_PREFIX + categoryId;
                redisTemplate.opsForZSet().add(categoryKey, tuples);
            });

            // 只保留前1000篇热门文章
            redisTemplate.opsForZSet().removeRange(HOT_ARTICLES_KEY, 0, -1001);
            categoryScores.keySet().forEach(categoryId -> {
                String categoryKey = HOT_ARTICLES_CATEGORY_PREFIX + categoryId;
                redisTemplate.opsForZSet().removeRange(categoryKey, 0, -501);
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("热门文章排行榜更新完成，处理 {} 篇文章，耗时 {}ms", articles.size(), duration);

        } catch (Exception e) {
            log.error("更新热门文章排行榜失败", e);
        }
    }

    /**
     * 计算文章热度分数
     * 算法：(浏览量*1 + 点赞*5 + 评论*3 + 收藏*4) / (时间差/小时 + 2)^1.5
     */
    private double calculateHotScore(Article article, long likeCount, long favoriteCount,
                                     long commentCount, long currentTime) {
        // 基础分数
        double baseScore = (article.getViewCount() != null ? article.getViewCount() : 0)
                + likeCount * 5
                + commentCount * 3
                + favoriteCount * 4;

        // 时间衰减
        try {
            LocalDateTime createdAt = LocalDateTime.parse(
                article.getCreatedAt(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            long createdTime = createdAt.atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli();
            double hoursSinceCreated = (currentTime - createdTime) / (1000.0 * 3600.0);
            double timeFactor = Math.pow(hoursSinceCreated + 2, 1.5);
            return baseScore / timeFactor;
        } catch (Exception e) {
            log.warn("解析文章创建时间失败，文章ID: {}", article.getId());
            return baseScore;
        }
    }

    /**
     * 从缓存获取热门文章列表
     */
    @Override
    public Page<Article> getHotArticlesFromCache(int page, int limit, Long categoryId,
                                                  String search, Long currentUserId) {
        try {
            // 确定使用哪个排行榜
            String key = categoryId != null
                ? HOT_ARTICLES_CATEGORY_PREFIX + categoryId
                : HOT_ARTICLES_KEY;

            // 从 Redis ZSet 获取文章ID（倒序，分数高的在前）
            long start = (long) (page - 1) * limit;
            long end = start + limit - 1;
            Set<Object> articleIds = redisTemplate.opsForZSet().reverseRange(key, start, end);

            if (articleIds == null || articleIds.isEmpty()) {
                log.warn("热门文章缓存为空，key: {}", key);
                return new Page<>(page, limit, 0);
            }

            // 批量查询文章详情
            List<Long> ids = articleIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .toList();

            List<Article> articles = articleMapper.selectBatchIds(ids);

            // 搜索过滤
            if (StringUtils.hasText(search)) {
                articles = articles.stream()
                    .filter(a -> a.getTitle().contains(search) || a.getContent().contains(search))
                    .toList();
            }

            // 保持 Redis 中的顺序
            Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));
            List<Article> sortedArticles = ids.stream()
                .map(articleMap::get)
                .filter(Objects::nonNull)
                .toList();

            // 填充作者信息和浏览量
            fillAuthorInfo(sortedArticles, currentUserId);
            fillViewCountFromCache(sortedArticles);

            // 获取总数
            Long total = redisTemplate.opsForZSet().zCard(key);

            Page<Article> result = new Page<>(page, limit, total != null ? total : 0);
            result.setRecords(sortedArticles);
            return result;

        } catch (Exception e) {
            log.error("从缓存获取热门文章失败", e);
            // 降级：返回空结果
            return new Page<>(page, limit, 0);
        }
    }

    /**
     * 填充作者信息
     */
    private void fillAuthorInfo(List<Article> articles, Long currentUserId) {
        if (articles.isEmpty()) return;

        List<Long> userIds = articles.stream().map(Article::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        Map<Long, Long> likeCountMap = articleIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, id))
        ));

        // 查询当前用户点赞的文章
        Set<Long> likedArticleIds = new HashSet<>();
        if (currentUserId != null) {
            likedArticleIds = articleLikeMapper.selectList(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getUserId, currentUserId)
                    .in(ArticleLike::getArticleId, articleIds))
                    .stream().map(ArticleLike::getArticleId).collect(Collectors.toSet());
        }

        for (Article article : articles) {
            User user = userMap.get(article.getUserId());
            if (user != null) {
                article.setAuthorName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                article.setAuthorAvatar(user.getAvatar());
            }
            article.setLikeCount(likeCountMap.getOrDefault(article.getId(), 0L));
            article.setLiked(likedArticleIds.contains(article.getId()));
        }
    }

    /**
     * 填充浏览量
     */
    private void fillViewCountFromCache(List<Article> articles) {
        if (articles.isEmpty()) return;
        for (Article article : articles) {
            Number views = cacheUtil.get(AppConstants.CACHE_ARTICLE_VIEW_PREFIX + article.getId());
            if (views != null) {
                article.setViewCount(views.intValue());
            }
        }
    }

    /**
     * 更新单篇文章的热度分数（用于实时更新）
     */
    @Override
    public void updateArticleHotScore(Long articleId) {
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null || !"published".equals(article.getStatus())) {
                return;
            }

            // 查询统计数据
            long likeCount = articleLikeMapper.selectCount(
                new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, articleId)
            );
            long favoriteCount = articleFavoriteMapper.selectCount(
                new LambdaQueryWrapper<ArticleFavorite>().eq(ArticleFavorite::getArticleId, articleId)
            );
            long commentCount = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getArticleId, articleId)
            );

            // 计算热度分数
            double score = calculateHotScore(article, likeCount, favoriteCount, commentCount,
                System.currentTimeMillis());

            // 更新全局榜
            redisTemplate.opsForZSet().add(HOT_ARTICLES_KEY, articleId, score);

            // 更新分类榜
            if (article.getCategoryId() != null) {
                String categoryKey = HOT_ARTICLES_CATEGORY_PREFIX + article.getCategoryId();
                redisTemplate.opsForZSet().add(categoryKey, articleId, score);
            }

        } catch (Exception e) {
            log.error("更新文章热度分数失败，文章ID: {}", articleId, e);
        }
    }
}
