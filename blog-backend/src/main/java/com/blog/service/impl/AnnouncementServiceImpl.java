package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.pojo.entity.Announcement;
import com.blog.mapper.AnnouncementMapper;
import com.blog.service.AnnouncementService;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    private final CacheUtil cacheUtil;
    private static final String CACHE_ACTIVE_ANNOUNCEMENTS = "announcement:active";
    private static final long CACHE_TTL_MINUTES = 10L;

    @Override
    @SuppressWarnings("unchecked")
    public List<Announcement> getActiveAnnouncements() {
        return cacheUtil.getWithPassThrough(
            CACHE_ACTIVE_ANNOUNCEMENTS, List.class, CACHE_TTL_MINUTES, TimeUnit.MINUTES,
            () -> {
                LocalDateTime now = LocalDateTime.now();
                return this.list(new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getIsActive, true)
                        .and(w -> w.isNull(Announcement::getStartTime).or().le(Announcement::getStartTime, now))
                        .and(w -> w.isNull(Announcement::getEndTime).or().ge(Announcement::getEndTime, now))
                        .orderByDesc(Announcement::getIsPinned)
                        .orderByAsc(Announcement::getSortOrder)
                        .orderByDesc(Announcement::getCreatedAt));
            }
        );
    }

    @Override
    public List<Announcement> getAllAnnouncements() {
        return this.list(new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getIsPinned)
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getCreatedAt));
    }

    @Override
    public void clearCache() {
        cacheUtil.delete(CACHE_ACTIVE_ANNOUNCEMENTS);
    }
}