package com.blog.controller;

import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.entity.Announcement;
import com.blog.service.AnnouncementService;
import com.blog.util.DateUtil;
import com.blog.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/announcements")
    public ApiResponse<List<Announcement>> getAnnouncements() {
        return ApiResponse.success(announcementService.getActiveAnnouncements());
    }

    @GetMapping("/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Announcement>> getAllAnnouncements() {
        return ApiResponse.success(announcementService.getAllAnnouncements());
    }

    @PostMapping("/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Announcement> createAnnouncement(@RequestBody Announcement announcement) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("未认证，无法创建公告");
        }
        announcement.setCreatedBy(userId);
        announcement.setCreatedAt(DateUtil.getCurrentDateTime());
        announcement.setUpdatedAt(DateUtil.getCurrentDateTime());
        if (announcement.getIsActive() == null) {
            announcement.setIsActive(true);
        }
        if (announcement.getIsPinned() == null) {
            announcement.setIsPinned(false);
        }
        if (announcement.getSortOrder() == null) {
            announcement.setSortOrder(0);
        }
        announcementService.save(announcement);
        announcementService.clearCache();
        return ApiResponse.success(announcement);
    }

    @PutMapping("/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Announcement> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        announcement.setUpdatedAt(DateUtil.getCurrentDateTime());
        announcementService.updateById(announcement);
        announcementService.clearCache();
        return ApiResponse.success(announcement);
    }

    @DeleteMapping("/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.removeById(id);
        announcementService.clearCache();
        return ApiResponse.success("删除成功", null);
    }
}