package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.dto.PageResponse;
import com.blog.pojo.entity.Article;
import com.blog.pojo.entity.ArticleFavorite;
import com.blog.pojo.entity.ArticleLike;
import com.blog.pojo.entity.User;
import com.blog.mapper.ArticleFavoriteMapper;
import com.blog.mapper.ArticleLikeMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.ArticleService;
import com.blog.service.NotificationService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleInteractionController {

    private final ArticleLikeMapper likeMapper;
    private final ArticleFavoriteMapper favoriteMapper;
    private final ArticleService articleService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final com.blog.service.HotArticleService hotArticleService;

    // 点赞/取消点赞
    @PostMapping("/{id}/like")
    public ApiResponse<?> toggleLike(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ArticleLike existing = likeMapper.selectOne(
                new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getUserId, userId)
                        .eq(ArticleLike::getArticleId, id));
        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            // 更新热度分数
            hotArticleService.updateArticleHotScore(id);
            return ApiResponse.success(Map.of("liked", false));
        }
        ArticleLike like = new ArticleLike();
        like.setUserId(userId);
        like.setArticleId(id);
        like.setCreatedAt(DateUtil.now());
        likeMapper.insert(like);
        // 发送点赞通知
        Article article = articleService.getById(id);
        if (article != null) {
            notificationService.send(article.getUserId(), userId, "like", id, "赞了你的文章");
        }
        // 更新热度分数
        hotArticleService.updateArticleHotScore(id);
        return ApiResponse.success(Map.of("liked", true));
    }

    // 收藏/取消收藏
    @PostMapping("/{id}/favorite")
    public ApiResponse<?> toggleFavorite(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ArticleFavorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, userId)
                        .eq(ArticleFavorite::getArticleId, id));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            // 更新热度分数
            hotArticleService.updateArticleHotScore(id);
            return ApiResponse.success(Map.of("favorited", false));
        }
        ArticleFavorite fav = new ArticleFavorite();
        fav.setUserId(userId);
        fav.setArticleId(id);
        fav.setCreatedAt(DateUtil.now());
        favoriteMapper.insert(fav);
        // 发送收藏通知
        Article article = articleService.getById(id);
        if (article != null) {
            notificationService.send(article.getUserId(), userId, "favorite", id, "收藏了你的文章");
        }
        // 更新热度分数
        hotArticleService.updateArticleHotScore(id);
        return ApiResponse.success(Map.of("favorited", true));
    }

    // 获取文章点赞收藏状态
    @GetMapping("/{id}/interaction")
    public ApiResponse<?> getInteraction(@PathVariable Long id, Authentication auth) {
        Long likeCount = likeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, id));
        Long favCount = favoriteMapper.selectCount(new LambdaQueryWrapper<ArticleFavorite>().eq(ArticleFavorite::getArticleId, id));
        boolean liked = false, favorited = false;
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            liked = likeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getUserId, userId).eq(ArticleLike::getArticleId, id)) > 0;
            favorited = favoriteMapper.selectCount(new LambdaQueryWrapper<ArticleFavorite>()
                    .eq(ArticleFavorite::getUserId, userId).eq(ArticleFavorite::getArticleId, id)) > 0;
        }
        return ApiResponse.success(Map.of("likeCount", likeCount, "favoriteCount", favCount, "liked", liked, "favorited", favorited));
    }

    // 我的收藏列表
    @GetMapping("/my/favorites")
    public ApiResponse<?> myFavorites(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int limit,
                                      Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Page<ArticleFavorite> pageObj = new Page<>(page, limit);
        Page<ArticleFavorite> favPage = favoriteMapper.selectPage(pageObj,
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, userId)
                        .orderByDesc(ArticleFavorite::getCreatedAt));
        List<Long> articleIds = favPage.getRecords().stream().map(ArticleFavorite::getArticleId).toList();
        if (articleIds.isEmpty()) {
            return ApiResponse.success(new PageResponse<>(List.of(), 0L, page, limit));
        }
        List<Article> articles = articleService.listByIds(articleIds);
        // 填充用户信息
        Map<Long, User> userMap = userMapper.selectBatchIds(
                articles.stream().map(Article::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));
        for (Article article : articles) {
            User u = userMap.get(article.getUserId());
            if (u != null) {
                article.setAuthorName(u.getNickname() != null ? u.getNickname() : u.getUsername());
                article.setAuthorAvatar(u.getAvatar());
            }
        }
        return ApiResponse.success(new PageResponse<>(articles, favPage.getTotal(), page, limit));
    }
}
