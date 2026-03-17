package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.pojo.entity.Announcement;

import java.util.List;

public interface AnnouncementService extends IService<Announcement> {
    List<Announcement> getActiveAnnouncements();
    List<Announcement> getAllAnnouncements();
    void clearCache();
}