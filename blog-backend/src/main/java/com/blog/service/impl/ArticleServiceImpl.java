package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.constant.AppConstants;
import com.blog.pojo.dto.ArticleRequest;
import com.blog.pojo.entity.*;
import com.blog.exception.BusinessException;
import com.blog.exception.ErrorCode;
import com.blog.mapper.*;
import com.blog.service.ArticleService;
import com.blog.util.CacheUtil;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final UserMapper userMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleFavoriteMapper articleFavoriteMapper;
    private final CommentMapper commentMapper;
    private final ArticleTagMapper articleTagMapper;
    private final FollowMapper followMapper;
    private final CacheUtil cacheUtil;

    @Override
    public Page<Article> getArticles(int page, int limit, Long categoryId, Long userId, String search, String sort) {
        return getArticles(page, limit, categoryId, userId, search, sort, null);
    }

    @Override
    public Page<Article> getArticles(int page, int limit, Long categoryId, Long userId, String search, String sort, Long currentUserId) {
        Page<Article> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(Article::getUserId, userId);
        } else {
            wrapper.eq(Article::getStatus, "published");
        }

        if (categoryId != null) {
            wrapper.eq(Article::getCategoryId, categoryId);
        }

        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(Article::getTitle, search).or().like(Article::getContent, search));
        }

        if (AppConstants.SORT_LATEST.equals(sort)) {
            wrapper.orderByDesc(Article::getCreatedAt);
        } else if (AppConstants.SORT_POPULAR.equals(sort)) {
            // 热门文章使用权重计算：先查出所有已发布文章，计算权重排序后再分页
            wrapper.orderByDesc(Article::getCreatedAt);
            // 查询所有符合条件的文章（不分页）
            List<Article> allArticles = list(wrapper);

            if (!allArticles.isEmpty()) {
                fillAuthorInfo(allArticles, currentUserId);
                fillViewCountFromCache(allArticles);

                // 计算权重并排序：(浏览量*1 + 点赞*5 + 评论*3 + 收藏*4) / (时间差/小时 + 2)^1.5
                // 类似Reddit热度算法，时间越久权重越低
                List<Long> ids = allArticles.stream().map(Article::getId).toList();
                Map<Long, Long> favoriteMap = articleFavoriteMapper.selectList(
                        new LambdaQueryWrapper<ArticleFavorite>().in(ArticleFavorite::getArticleId, ids))
                        .stream().collect(Collectors.groupingBy(ArticleFavorite::getArticleId, Collectors.counting()));
                Map<Long, Long> commentMap = commentMapper.selectList(
                        new LambdaQueryWrapper<Comment>().in(Comment::getArticleId, ids))
                        .stream().collect(Collectors.groupingBy(Comment::getArticleId, Collectors.counting()));

                long currentTime = System.currentTimeMillis();
                allArticles.forEach(a -> {
                    // 基础分数（使用double避免整数除法）
                    double baseScore = (a.getViewCount() != null ? a.getViewCount() : 0)
                            + (a.getLikeCount() != null ? a.getLikeCount() * 5 : 0)
                            + commentMap.getOrDefault(a.getId(), 0L) * 3
                            + favoriteMap.getOrDefault(a.getId(), 0L) * 4;

                    // 时间衰减：计算文章发布到现在的小时数
                    try {
                        long createdTime = java.time.LocalDateTime.parse(a.getCreatedAt(),
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                        double hoursSinceCreated = (currentTime - createdTime) / (1000.0 * 3600.0);
                        // 使用幂函数衰减，越久的文章权重越低
                        double timeFactor = Math.pow(hoursSinceCreated + 2, 1.5);
                        // 使用浮点数计算，避免整数除法导致结果为0
                        double finalScoreDouble = baseScore / timeFactor;
                        long finalScore = Math.round(finalScoreDouble);
                        a.setHotScore(finalScore);
                    } catch (Exception e) {
                        // 解析失败则使用基础分数
                        a.setHotScore(Math.round(baseScore));
                    }
                });

                // 按权重排序
                allArticles.sort(Comparator.comparing(Article::getHotScore).reversed());

                // 手动分页
                int start = (page - 1) * limit;
                int end = Math.min(start + limit, allArticles.size());
                List<Article> pagedArticles = start < allArticles.size()
                    ? allArticles.subList(start, end)
                    : List.of();

                // 构造分页结果
                Page<Article> result = new Page<>(page, limit, allArticles.size());
                result.setRecords(pagedArticles);
                return result;
            }

            // 如果没有文章，返回空分页
            return new Page<>(page, limit, 0);
        } else {
            wrapper.orderByDesc(Article::getCreatedAt);
        }

        Page<Article> result = page(pageParam, wrapper);
        fillAuthorInfo(result.getRecords(), currentUserId);
        fillViewCountFromCache(result.getRecords());
        return result;
    }

    private void fillAuthorInfo(List<Article> articles) {
        fillAuthorInfo(articles, null);
    }

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
        java.util.Set<Long> likedArticleIds = new java.util.HashSet<>();
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
    public void incrementViewCount(Long articleId) {
        // 使用Redis原子递增，高并发安全
        String key = AppConstants.CACHE_ARTICLE_VIEW_PREFIX + articleId;
        Long views = cacheUtil.increment(key, 30, TimeUnit.MINUTES);
        // 每10次写入数据库
        if (views != null && views % 10 == 0) {
            Article article = getById(articleId);
            if (article != null) {
                article.setViewCount(views.intValue());
                updateById(article);
            }
        }
    }

    @Override
    public Article getArticleWithAuthor(Long id) {
        // 使用分布式锁防止热点文章缓存击穿
        Article article = cacheUtil.getWithLock(
            AppConstants.CACHE_ARTICLE_PREFIX + id, Article.class, 30, TimeUnit.MINUTES,
            () -> getById(id)
        );
        if (article != null) {
            fillAuthorInfo(List.of(article), null);
            Number views = cacheUtil.get(AppConstants.CACHE_ARTICLE_VIEW_PREFIX + id);
            if (views != null) {
                article.setViewCount(views.intValue());
            }
        }
        return article;
    }

    public void clearArticleCache(Long id) {
        cacheUtil.delete(AppConstants.CACHE_ARTICLE_PREFIX + id);
    }

    @Override
    public Page<Article> getFollowingArticles(int page, int limit, Long currentUserId) {
        // 获取关注的用户ID列表
        List<Long> followingIds = followMapper.selectList(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, currentUserId)
        ).stream().map(Follow::getFollowingId).toList();

        if (followingIds.isEmpty()) {
            return new Page<>(page, limit, 0);
        }

        // 查询关注用户的文章，按时间倒序
        Page<Article> pageObj = new Page<>(page, limit);
        Page<Article> result = page(pageObj,
                new LambdaQueryWrapper<Article>()
                        .in(Article::getUserId, followingIds)
                        .eq(Article::getStatus, "published")
                        .orderByDesc(Article::getCreatedAt));

        // 填充作者信息
        fillAuthorInfo(result.getRecords(), currentUserId);
        fillViewCountFromCache(result.getRecords());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Article createArticle(ArticleRequest request, Long userId) {
        Article article = new Article();
        article.setUserId(userId);
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setCategoryId(request.getCategoryId());
        article.setStatus(request.getStatus() != null ? request.getStatus() : "draft");
        article.setViewCount(0);
        article.setCreatedAt(DateUtil.now());
        article.setUpdatedAt(DateUtil.now());

        save(article);

        // 保存标签关联
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (Long tagId : request.getTags()) {
                ArticleTag at = new ArticleTag();
                at.setArticleId(article.getId());
                at.setTagId(tagId);
                articleTagMapper.insert(at);
            }
        }

        return article;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Article updateArticle(Long id, ArticleRequest request, Long userId) {
        Article article = getById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ARTICLE_PERMISSION_DENIED);
        }

        if (request.getTitle() != null) article.setTitle(request.getTitle());
        if (request.getContent() != null) article.setContent(request.getContent());
        if (request.getCategoryId() != null) article.setCategoryId(request.getCategoryId());
        if (request.getStatus() != null) article.setStatus(request.getStatus());
        article.setUpdatedAt(DateUtil.now());

        updateById(article);
        clearArticleCache(id);
        return article;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id, Long userId) {
        Article article = getById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ARTICLE_PERMISSION_DENIED);
        }

        removeById(id);
        clearArticleCache(id);
    }
}
