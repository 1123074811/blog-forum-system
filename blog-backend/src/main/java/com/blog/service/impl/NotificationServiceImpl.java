package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.constant.AppConstants;
import com.blog.pojo.entity.Notification;
import com.blog.mapper.NotificationMapper;
import com.blog.service.NotificationService;
import com.blog.util.DateUtil;
import com.blog.websocket.NotificationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationHandler notificationHandler;

    @Override
    public void send(Long userId, Long fromUserId, String type, Long targetId, String content) {
        if (userId.equals(fromUserId)) return; // 不给自己发通知
        Notification n = new Notification();
        n.setUserId(userId);
        n.setFromUserId(fromUserId);
        n.setType(type);
        n.setTargetId(targetId);
        n.setContent(content);
        n.setIsRead(false);
        n.setCreatedAt(DateUtil.now());
        notificationMapper.insert(n);
        notificationHandler.sendNotification(userId, n);
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .last("LIMIT " + AppConstants.NOTIFICATION_LIMIT));
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false));
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        Notification n = notificationMapper.selectById(id);
        if (n != null && n.getUserId().equals(userId)) {
            n.setIsRead(true);
            notificationMapper.updateById(n);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true));
    }
}
