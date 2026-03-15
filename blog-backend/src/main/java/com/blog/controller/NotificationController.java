package com.blog.controller;

import com.blog.context.BaseContext;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.entity.Notification;
import com.blog.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<Notification>> getNotifications() {
        Long userId = requireCurrentUserId();
        return ApiResponse.success(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount() {
        Long userId = requireCurrentUserId();
        return ApiResponse.success(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Boolean> markAsRead(@PathVariable Long id) {
        Long userId = requireCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ApiResponse.success(true);
    }

    @PostMapping("/read-all")
    public ApiResponse<Boolean> markAllAsRead() {
        Long userId = requireCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.success(true);
    }

    private Long requireCurrentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "未登录");
        }
        return userId;
    }
}
