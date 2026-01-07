package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.constant.AppConstants;
import com.blog.entity.Tag;
import com.blog.mapper.TagMapper;
import com.blog.service.TagService;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private final CacheUtil cacheUtil;

    @Override
    @SuppressWarnings("unchecked")
    public List<Tag> listCached() {
        return cacheUtil.getWithPassThrough(
            AppConstants.CACHE_TAG_LIST, List.class,
            AppConstants.CACHE_CATEGORY_TTL_MINUTES, TimeUnit.MINUTES,
            this::list
        );
    }

    @Override
    public void clearCache() {
        cacheUtil.delete(AppConstants.CACHE_TAG_LIST);
    }
}
