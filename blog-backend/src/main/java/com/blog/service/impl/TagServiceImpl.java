package com.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.pojo.entity.Tag;
import com.blog.mapper.TagMapper;
import com.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    @Cacheable(value = "tags", key = "'all'")
    public List<Tag> listCached() {
        return list();
    }

    @Override
    @CacheEvict(value = "tags", allEntries = true)
    public void clearCache() {
        // no-op, cache is evicted by annotation
    }
}
