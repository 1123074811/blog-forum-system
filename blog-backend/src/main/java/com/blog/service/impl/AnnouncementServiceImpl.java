package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.pojo.entity.Announcement;
import com.blog.mapper.AnnouncementMapper;
import com.blog.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    @Override
    public List<Announcement> getActiveAnnouncements() {
        return this.list(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getIsActive, true)
                .orderByDesc(Announcement::getIsPinned)
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getCreatedAt));
    }

    @Override
    public List<Announcement> getAllAnnouncements() {
        return this.list(new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getIsPinned)
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getCreatedAt));
    }
}