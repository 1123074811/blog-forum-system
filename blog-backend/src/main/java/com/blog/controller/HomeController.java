package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.converter.ArticleConverter;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.dto.PageResponse;
import com.blog.pojo.entity.Announcement;
import com.blog.pojo.entity.Article;
import com.blog.pojo.entity.Category;
import com.blog.pojo.entity.Tag;
import com.blog.pojo.vo.ArticleVO;
import com.blog.pojo.vo.HomeVO;
import com.blog.service.AnnouncementService;
import com.blog.service.ArticleService;
import com.blog.service.CategoryService;
import com.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController extends BaseController {

    private final ArticleService articleService;
    private final ArticleConverter articleConverter;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final AnnouncementService announcementService;

    @GetMapping
    public ApiResponse<HomeVO> getHomeData(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(required = false) Long category,
            @RequestParam(defaultValue = "latest") String sort,
            Authentication auth) {

        Long currentUserId = getCurrentUserIdOptional(auth);

        Page<Article> articlePage = articleService.getArticles(page, limit, category, null, null, sort, currentUserId);
        PageResponse<ArticleVO> articles = new PageResponse<>(
                articleConverter.toVOList(articlePage.getRecords()),
                articlePage.getTotal(),
                page,
                limit
        );

        Page<Article> hotPage = articleService.getArticles(1, 5, null, null, null, "popular", currentUserId);
        List<ArticleVO> hotArticles = articleConverter.toVOList(hotPage.getRecords());

        List<Category> categories = categoryService.list();
        List<Tag> tags = tagService.list();
        List<Announcement> announcements = announcementService.list();

        HomeVO home = HomeVO.builder()
                .articles(articles)
                .hotArticles(hotArticles)
                .categories(categories)
                .tags(tags)
                .announcements(announcements)
                .build();

        return ApiResponse.success(home);
    }
}
