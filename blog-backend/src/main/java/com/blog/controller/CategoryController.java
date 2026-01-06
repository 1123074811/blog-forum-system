package com.blog.controller;

import com.blog.constant.AppConstants;
import com.blog.dto.ApiResponse;
import com.blog.entity.Category;
import com.blog.service.CategoryService;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CacheUtil cacheUtil;

    private static final String CATEGORY_LIST_KEY = "category:list";

    @GetMapping
    @SuppressWarnings("unchecked")
    public ApiResponse<List<Category>> getCategories() {
        List<Category> list = cacheUtil.get(CATEGORY_LIST_KEY);
        if (list == null) {
            list = categoryService.list();
            cacheUtil.set(CATEGORY_LIST_KEY, list, 60, TimeUnit.MINUTES);
        }
        return ApiResponse.success(list);
    }
}
