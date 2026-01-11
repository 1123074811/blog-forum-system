package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.entity.Recitation;
import com.blog.mapper.RecitationMapper;
import com.blog.service.RecitationService;
import org.springframework.stereotype.Service;

@Service
public class RecitationServiceImpl extends ServiceImpl<RecitationMapper, Recitation> implements RecitationService {

    @Override
    public Page<Recitation> getUserRecitations(Long userId, int page, int limit) {
        Page<Recitation> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<Recitation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recitation::getUserId, userId)
               .orderByDesc(Recitation::getUpdatedAt);
        return this.page(pageParam, wrapper);
    }

    @Override
    public Recitation getLatestUnfinished(Long userId) {
        LambdaQueryWrapper<Recitation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recitation::getUserId, userId)
               .eq(Recitation::getCompleted, false)
               .orderByDesc(Recitation::getUpdatedAt)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }
}
