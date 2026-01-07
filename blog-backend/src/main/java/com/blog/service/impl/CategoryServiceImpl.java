package com.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.constant.AppConstants;
import com.blog.entity.Category;
import com.blog.mapper.CategoryMapper;
import com.blog.service.CategoryService;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final CacheUtil cacheUtil;

    @Override
    @SuppressWarnings("unchecked")
    public List<Category> listCached() {
        return cacheUtil.getWithPassThrough(
            AppConstants.CACHE_CATEGORY_LIST, List.class,
            AppConstants.CACHE_CATEGORY_TTL_MINUTES, TimeUnit.MINUTES,
            this::list
        );
    }

    @Override
    public void clearCache() {
        cacheUtil.delete(AppConstants.CACHE_CATEGORY_LIST);
    }
}
