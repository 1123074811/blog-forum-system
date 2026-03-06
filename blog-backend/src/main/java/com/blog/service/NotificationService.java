package com.blog.service;

import com.blog.pojo.entity.Notification;
import java.util.List;

public interface NotificationService {
    void send(Long userId, Long fromUserId, String type, Long targetId, String content);
    List<Notification> getUserNotifications(Long userId);
    Long getUnreadCount(Long userId);
    void markAsRead(Long id, Long userId);
    void markAllAsRead(Long userId);
}
