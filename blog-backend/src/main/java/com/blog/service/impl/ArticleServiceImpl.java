package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.constant.AppConstants;
import com.blog.entity.Article;
import com.blog.entity.ArticleLike;
import com.blog.entity.User;
import com.blog.mapper.ArticleLikeMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.ArticleService;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
            wrapper.orderByDesc(Article::getViewCount);
        } else {
            wrapper.orderByDesc(Article::getCreatedAt);
        }

        Page<Article> result = page(pageParam, wrapper);
        fillAuthorInfo(result.getRecords(), currentUserId);
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
}
