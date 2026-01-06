package com.blog.controller;

import com.blog.constant.AppConstants;
import com.blog.dto.ApiResponse;
import com.blog.entity.Tag;
import com.blog.service.TagService;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final CacheUtil cacheUtil;

    private static final String TAG_LIST_KEY = "tag:list";

    @GetMapping
    @SuppressWarnings("unchecked")
    public ApiResponse<List<Tag>> getTags() {
        List<Tag> list = cacheUtil.get(TAG_LIST_KEY);
        if (list == null) {
            list = tagService.list();
            cacheUtil.set(TAG_LIST_KEY, list, 60, TimeUnit.MINUTES);
        }
        return ApiResponse.success(list);
    }
}
