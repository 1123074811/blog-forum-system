package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.entity.ArticleTag;
import com.blog.pojo.entity.Tag;
import com.blog.mapper.ArticleTagMapper;
import com.blog.service.TagService;
import com.blog.util.CacheUtil;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final ArticleTagMapper articleTagMapper;
    private final CacheUtil cacheUtil;

    private static final String TAG_HOT_KEY = "tag:list:hot";

    @GetMapping
    public ApiResponse<List<Tag>> getTags() {
        @SuppressWarnings("unchecked")
        List<Tag> list = cacheUtil.get(TAG_HOT_KEY);
        if (list == null) {
            list = tagService.list();
            Map<Long, Long> countMap = articleTagMapper.selectList(null).stream()
                    .collect(Collectors.groupingBy(ArticleTag::getTagId, Collectors.counting()));
            list.forEach(tag -> tag.setArticleCount(countMap.getOrDefault(tag.getId(), 0L)));
            list.sort(Comparator.comparing(Tag::getArticleCount).reversed());
            cacheUtil.set(TAG_HOT_KEY, list, 30, TimeUnit.MINUTES);
        }
        return ApiResponse.success(list);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Tag> createTag(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ApiResponse.error("标签名不能为空");
        }
        Tag existing = tagService.getOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name.trim()));
        if (existing != null) {
            return ApiResponse.success(existing);
        }
        Tag tag = new Tag();
        tag.setName(name.trim());
        tag.setCreatedAt(DateUtil.now());
        tagService.save(tag);
        tag.setArticleCount(0L);
        tagService.clearCache();
        cacheUtil.delete(TAG_HOT_KEY);
        return ApiResponse.success(tag);
    }
}
