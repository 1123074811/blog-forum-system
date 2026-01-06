package com.blog.controller;

import com.blog.dto.ApiResponse;
import com.blog.entity.Notification;
import com.blog.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<Notification>> getNotifications(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Boolean> markAsRead(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        notificationService.markAsRead(id, userId);
        return ApiResponse.success(true);
    }

    @PostMapping("/read-all")
    public ApiResponse<Boolean> markAllAsRead(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        notificationService.markAllAsRead(userId);
        return ApiResponse.success(true);
    }
}
