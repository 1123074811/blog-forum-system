package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.constant.AppConstants;
import com.blog.mapper.ArticleFavoriteMapper;
import com.blog.mapper.ArticleLikeMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserMapper;
import com.blog.pojo.entity.Article;
import com.blog.pojo.entity.ArticleFavorite;
import com.blog.pojo.entity.ArticleLike;
import com.blog.pojo.entity.Comment;
import com.blog.pojo.entity.User;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Scheduled(fixedRate = 300000)
    public void updateHotArticles() {
        log.info("开始更新热门文章排行榜...");
        long startTime = System.currentTimeMillis();

        try {
            String thirtyDaysAgo = LocalDateTime.now().minusDays(30).format(DATE_TIME_FORMATTER);

            List<Article> articles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getStatus, "published")
                            .ge(Article::getCreatedAt, thirtyDaysAgo)
                            .select(Article::getId, Article::getCategoryId, Article::getCreatedAt, Article::getViewCount)
            );

            if (articles.isEmpty()) {
                log.info("没有近30天发布的文章，跳过热门榜更新");
                return;
            }

            List<Long> articleIds = articles.stream().map(Article::getId).toList();
            Map<Long, Long> likeCountMap = toCountMap(articleLikeMapper.batchCountByArticleIds(articleIds), "likeCount");
            Map<Long, Long> favoriteCountMap = toCountMap(articleFavoriteMapper.batchCountByArticleIds(articleIds), "favoriteCount");
            Map<Long, Long> commentCountMap = toCountMap(commentMapper.batchCountByArticleIds(articleIds), "commentCount");

            long currentTime = System.currentTimeMillis();
            Set<ZSetOperations.TypedTuple<Object>> globalTuples = new HashSet<>();
            Map<Long, Set<ZSetOperations.TypedTuple<Object>>> categoryScores = new HashMap<>();

            for (Article article : articles) {
                double score = calculateHotScore(
                        article,
                        likeCountMap.getOrDefault(article.getId(), 0L),
                        favoriteCountMap.getOrDefault(article.getId(), 0L),
                        commentCountMap.getOrDefault(article.getId(), 0L),
                        currentTime
                );

                globalTuples.add(ZSetOperations.TypedTuple.of(article.getId(), score));

                if (article.getCategoryId() != null) {
                    categoryScores.computeIfAbsent(article.getCategoryId(), k -> new HashSet<>())
                            .add(ZSetOperations.TypedTuple.of(article.getId(), score));
                }
            }

            redisTemplate.delete(HOT_ARTICLES_KEY);
            redisTemplate.opsForZSet().add(HOT_ARTICLES_KEY, globalTuples);

            categoryScores.forEach((categoryId, tuples) -> {
                String categoryKey = HOT_ARTICLES_CATEGORY_PREFIX + categoryId;
                redisTemplate.delete(categoryKey);
                redisTemplate.opsForZSet().add(categoryKey, tuples);
                redisTemplate.opsForZSet().removeRange(categoryKey, 0, -501);
            });

            redisTemplate.opsForZSet().removeRange(HOT_ARTICLES_KEY, 0, -1001);

            long duration = System.currentTimeMillis() - startTime;
            log.info("热门文章排行榜更新完成，处理 {} 篇，耗时 {}ms", articles.size(), duration);
        } catch (Exception e) {
            log.error("更新热门文章排行榜失败", e);
        }
    }

    private Map<Long, Long> toCountMap(List<Map<String, Object>> rows, String countKey) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        return rows.stream().collect(Collectors.toMap(
                m -> ((Number) m.get("articleId")).longValue(),
                m -> ((Number) m.get(countKey)).longValue()
        ));
    }

    private double calculateHotScore(Article article, long likeCount, long favoriteCount,
                                     long commentCount, long currentTime) {
        double baseScore = (article.getViewCount() != null ? article.getViewCount() : 0)
                + likeCount * 5
                + commentCount * 3
                + favoriteCount * 4;

        try {
            LocalDateTime createdAt = LocalDateTime.parse(article.getCreatedAt(), DATE_TIME_FORMATTER);
            long createdTime = createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            double hoursSinceCreated = (currentTime - createdTime) / (1000.0 * 3600.0);
            double timeFactor = Math.pow(hoursSinceCreated + 2, 1.5);
            return baseScore / timeFactor;
        } catch (Exception e) {
            log.warn("解析文章创建时间失败，文章ID: {}", article.getId());
            return baseScore;
        }
    }

    @Override
    public Page<Article> getHotArticlesFromCache(int page, int limit, Long categoryId,
                                                 String search, Long currentUserId) {
        try {
            String key = categoryId != null
                    ? HOT_ARTICLES_CATEGORY_PREFIX + categoryId
                    : HOT_ARTICLES_KEY;

            long start = (long) (page - 1) * limit;
            long end = start + limit - 1;
            Set<Object> articleIds = redisTemplate.opsForZSet().reverseRange(key, start, end);

            if (articleIds == null || articleIds.isEmpty()) {
                log.warn("热门文章缓存为空，key: {}", key);
                return new Page<>(page, limit, 0);
            }

            List<Long> ids = articleIds.stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .toList();

            List<Article> articles = articleMapper.selectBatchIds(ids);

            if (StringUtils.hasText(search)) {
                articles = articles.stream()
                        .filter(a -> a.getTitle().contains(search) || a.getContent().contains(search))
                        .toList();
            }

            Map<Long, Article> articleMap = articles.stream()
                    .collect(Collectors.toMap(Article::getId, a -> a));
            List<Article> sortedArticles = ids.stream()
                    .map(articleMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            fillAuthorInfo(sortedArticles, currentUserId);
            fillViewCountFromCache(sortedArticles);

            Long total = redisTemplate.opsForZSet().zCard(key);
            Page<Article> result = new Page<>(page, limit, total != null ? total : 0);
            result.setRecords(sortedArticles);
            return result;

        } catch (Exception e) {
            log.error("从缓存获取热门文章失败", e);
            return new Page<>(page, limit, 0);
        }
    }

    private void fillAuthorInfo(List<Article> articles, Long currentUserId) {
        if (articles.isEmpty()) return;

        List<Long> userIds = articles.stream().map(Article::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        Map<Long, Long> likeCountMap = toCountMap(articleLikeMapper.batchCountByArticleIds(articleIds), "likeCount");

        Set<Long> likedArticleIds = new HashSet<>();
        if (currentUserId != null && !articleIds.isEmpty()) {
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

    private void fillViewCountFromCache(List<Article> articles) {
        if (articles.isEmpty()) return;
        for (Article article : articles) {
            Number views = cacheUtil.get(AppConstants.CACHE_ARTICLE_VIEW_PREFIX + article.getId());
            if (views != null) {
                article.setViewCount(views.intValue());
            }
        }
    }

    @Override
    public void updateArticleHotScore(Long articleId) {
        try {
            Article article = articleMapper.selectById(articleId);
            if (article == null || !"published".equals(article.getStatus())) {
                return;
            }

            long likeCount = articleLikeMapper.selectCount(
                    new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, articleId)
            );
            long favoriteCount = articleFavoriteMapper.selectCount(
                    new LambdaQueryWrapper<ArticleFavorite>().eq(ArticleFavorite::getArticleId, articleId)
            );
            long commentCount = commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>().eq(Comment::getArticleId, articleId)
            );

            double score = calculateHotScore(article, likeCount, favoriteCount, commentCount,
                    System.currentTimeMillis());

            redisTemplate.opsForZSet().add(HOT_ARTICLES_KEY, articleId, score);

            if (article.getCategoryId() != null) {
                String categoryKey = HOT_ARTICLES_CATEGORY_PREFIX + article.getCategoryId();
                redisTemplate.opsForZSet().add(categoryKey, articleId, score);
            }

        } catch (Exception e) {
            log.error("更新文章热度分数失败，文章ID: {}", articleId, e);
        }
    }
}
