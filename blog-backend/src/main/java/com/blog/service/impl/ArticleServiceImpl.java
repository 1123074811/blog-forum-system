package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.constant.AppConstants;
import com.blog.event.ArticlePublishedEvent;
import com.blog.exception.BusinessException;
import com.blog.exception.ErrorCode;
import com.blog.mapper.*;
import com.blog.pojo.dto.ArticleRequest;
import com.blog.pojo.entity.*;
import com.blog.service.ArticleService;
import com.blog.service.HotArticleService;
import com.blog.util.CacheUtil;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final TagMapper tagMapper;
    private final FollowMapper followMapper;
    private final CacheUtil cacheUtil;
    private final HotArticleService hotArticleService;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;

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
            // 只有本人才能看到自己的草稿，其他人只能看已发布的
            if (!userId.equals(currentUserId)) {
                wrapper.eq(Article::getStatus, "published");
            }
        } else {
            wrapper.eq(Article::getStatus, "published");
        }

        if (categoryId != null) {
            wrapper.eq(Article::getCategoryId, categoryId);
        }

        if (StringUtils.hasText(search)) {
            String keyword = search.trim();
            wrapper.apply("MATCH(title, content) AGAINST({0} IN BOOLEAN MODE)", "*" + keyword + "*");
        }

        if (AppConstants.SORT_LATEST.equals(sort)) {
            wrapper.orderByDesc(Article::getCreatedAt);
        } else if (AppConstants.SORT_POPULAR.equals(sort)) {
            // 热门文章使用 Redis ZSet 预计算结果，避免每次查询都计算权�?
            return hotArticleService.getHotArticlesFromCache(page, limit, categoryId, search, currentUserId);
        } else {
            wrapper.orderByDesc(Article::getCreatedAt);
        }

        Page<Article> result = page(pageParam, wrapper);
        fillAuthorInfo(result.getRecords(), currentUserId);
        fillViewCountFromCache(result.getRecords());
        return result;
    }

    // 改为 public，供 HotArticleService 调用
    public void fillAuthorInfo(List<Article> articles) {
        fillAuthorInfo(articles, null);
    }

    public void fillAuthorInfo(List<Article> articles, Long currentUserId) {
        if (articles.isEmpty()) return;
        List<Long> userIds = articles.stream().map(Article::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        // 批量查询点赞数，避免 N+1 查询
        Map<Long, Long> likeCountMap = articleLikeMapper.batchCountByArticleIds(articleIds)
                .stream().collect(Collectors.toMap(
                        m -> ((Number) m.get("articleId")).longValue(),
                        m -> ((Number) m.get("likeCount")).longValue()
                ));
        // 查询当前用户点赞的文�?
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

    // 改为 public，供 HotArticleService 调用
    public void fillViewCountFromCache(List<Article> articles) {
        if (articles.isEmpty()) return;
        // 批量从 Redis 获取浏览量，减少网络往返
        List<String> keys = articles.stream()
                .map(a -> AppConstants.CACHE_ARTICLE_VIEW_PREFIX + a.getId())
                .toList();
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            Object val = values != null ? values.get(i) : null;
            if (val != null) {
                article.setViewCount(((Number) val).intValue());
            }
            // Redis 没有时保留数据库值，不再逐个回查数据库
        }
    }

    @Override
    public void incrementViewCount(Long articleId) {
        String key = AppConstants.CACHE_ARTICLE_VIEW_PREFIX + articleId;

        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(exists)) {
            Article article = getById(articleId);
            int baseViews = (article != null && article.getViewCount() != null) ? article.getViewCount() : 0;
            redisTemplate.opsForValue().set(key, baseViews);
        }

        Long views = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);

        if (views != null && views % 10 == 0) {
            Article article = getById(articleId);
            if (article != null) {
                article.setViewCount(views.intValue());
                updateById(article);
                // 更新热度分数
                hotArticleService.updateArticleHotScore(articleId);
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
            } else {
                // Redis中没有数据时，从数据库读取并回填
                Article dbArticle = getById(id);
                if (dbArticle != null && dbArticle.getViewCount() != null) {
                    article.setViewCount(dbArticle.getViewCount());
                    // 回填到Redis
                    cacheUtil.set(AppConstants.CACHE_ARTICLE_VIEW_PREFIX + id, 
                                 dbArticle.getViewCount(), 30, TimeUnit.MINUTES);
                }
            }
            // 填充标签
            List<Long> tagIds = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id)
            ).stream().map(ArticleTag::getTagId).toList();
            if (!tagIds.isEmpty()) {
                article.setTags(tagMapper.selectBatchIds(tagIds));
            }
        }
        return article;
    }

    public void clearArticleCache(Long id) {
        // 清理文章详情缓存
        cacheUtil.delete(AppConstants.CACHE_ARTICLE_PREFIX + id);
        // 清理文章列表缓存（使用模糊匹配）
        cacheUtil.deleteByPattern(AppConstants.CACHE_ARTICLE_LIST_PREFIX + "*");
        // 更新热度分数
        hotArticleService.updateArticleHotScore(id);
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

        // 填充作者信�?
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

        // 如果是发布状态，发布事件通知粉丝
        if ("published".equals(article.getStatus())) {
            eventPublisher.publishEvent(new ArticlePublishedEvent(
                this, article.getId(), userId, article.getTitle()
            ));
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

        // 记录旧状�?
        String oldStatus = article.getStatus();

        if (request.getTitle() != null) article.setTitle(request.getTitle());
        if (request.getContent() != null) article.setContent(request.getContent());
        if (request.getCategoryId() != null) article.setCategoryId(request.getCategoryId());
        if (request.getStatus() != null) article.setStatus(request.getStatus());
        article.setUpdatedAt(DateUtil.now());

        updateById(article);

        // 更新标签
        if (request.getTags() != null) {
            articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
            if (!request.getTags().isEmpty()) {
                for (Long tagId : request.getTags()) {
                    ArticleTag at = new ArticleTag();
                    at.setArticleId(id);
                    at.setTagId(tagId);
                    articleTagMapper.insert(at);
                }
                // 设置返回对象的标�?
                article.setTags(tagMapper.selectBatchIds(request.getTags()));
            } else {
                article.setTags(List.of());
            }
        }

        // 如果从草稿变为发布，发布事件通知粉丝
        if (!"published".equals(oldStatus) && "published".equals(article.getStatus())) {
            eventPublisher.publishEvent(new ArticlePublishedEvent(
                this, article.getId(), userId, article.getTitle()
            ));
        }

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
