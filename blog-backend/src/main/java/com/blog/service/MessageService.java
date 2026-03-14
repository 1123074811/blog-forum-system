package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.pojo.entity.Conversation;
import com.blog.pojo.entity.Message;
import com.blog.pojo.entity.User;
import com.blog.mapper.ConversationMapper;
import com.blog.mapper.MessageMapper;
import com.blog.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageMapper messageMapper;
    private final ConversationMapper conversationMapper;
    private final UserService userService;
    private final FollowService followService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // 检查是否互相关注
    public boolean isMutualFollow(Long userId1, Long userId2) {
        return followService.isFollowing(userId1, userId2) && followService.isFollowing(userId2, userId1);
    }

    // 获取或创建会话
    public Conversation getOrCreateConversation(Long userId1, Long userId2) {
        Long smallId = Math.min(userId1, userId2);
        Long largeId = Math.max(userId1, userId2);

        Conversation conv = conversationMapper.selectOne(
            new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUser1Id, smallId)
                .eq(Conversation::getUser2Id, largeId)
        );

        if (conv == null) {
            conv = new Conversation();
            conv.setUser1Id(smallId);
            conv.setUser2Id(largeId);
            conv.setUser1Unread(0);
            conv.setUser2Unread(0);
            conv.setCreatedAt(now());
            conversationMapper.insert(conv);
        }
        return conv;
    }

    // 发送消息
    public Message sendMessage(Long senderId, Long receiverId, String content, String type, String fileUrl, String fileName) {
        boolean mutual = isMutualFollow(senderId, receiverId);
        Conversation conv = getOrCreateConversation(senderId, receiverId);

        // 非互关只能发一条
        if (!mutual) {
            long count = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                    .eq(Message::getConversationId, conv.getId())
                    .eq(Message::getSenderId, senderId)
            );
            if (count >= 1) {
                return null;
            }
        }

        Message msg = new Message();
        msg.setConversationId(conv.getId());
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setType(type != null ? type : "text");
        msg.setFileUrl(fileUrl);
        msg.setFileName(fileName);
        msg.setIsRead(false);
        msg.setCreatedAt(now());
        messageMapper.insert(msg);

        // 更新会话
        Long smallId = Math.min(senderId, receiverId);
        boolean senderIsUser1 = senderId.equals(smallId);

        LambdaUpdateWrapper<Conversation> update = new LambdaUpdateWrapper<Conversation>()
            .eq(Conversation::getId, conv.getId())
            .set(Conversation::getLastMessageId, msg.getId())
            .set(Conversation::getLastMessageTime, msg.getCreatedAt());

        if (senderIsUser1) {
            update.setSql("user2_unread = user2_unread + 1");
        } else {
            update.setSql("user1_unread = user1_unread + 1");
        }
        conversationMapper.update(null, update);

        // WebSocket 推送消息给接收者
        User sender = userService.getById(senderId);
        if (sender != null) {
            sender.setIsOnline(chatWebSocketHandler.isOnline(senderId));
        }
        msg.setSender(sender);
        Map<String, Object> wsMsg = new HashMap<>();
        wsMsg.put("type", "new_message");
        wsMsg.put("message", msg);
        wsMsg.put("conversationId", conv.getId());
        chatWebSocketHandler.sendToUser(receiverId, wsMsg);

        return msg;
    }

    // 获取会话列表
    public List<Conversation> getConversations(Long userId) {
        List<Conversation> list = conversationMapper.selectList(
            new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUser1Id, userId)
                .or()
                .eq(Conversation::getUser2Id, userId)
                .orderByDesc(Conversation::getLastMessageTime)
        );

        for (Conversation conv : list) {
            Long otherId = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
            User otherUser = userService.getById(otherId);
            if (otherUser != null) {
                otherUser.setIsOnline(chatWebSocketHandler.isOnline(otherId));
            }
            conv.setOtherUser(otherUser);
            if (conv.getLastMessageId() != null) {
                conv.setLastMessage(messageMapper.selectById(conv.getLastMessageId()));
            }
        }
        return list;
    }

    // 获取消息列表
    public List<Message> getMessages(Long conversationId, Long userId) {
        // 标记已读
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv != null) {
            boolean isUser1 = conv.getUser1Id().equals(userId);
            LambdaUpdateWrapper<Conversation> update = new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId);
            if (isUser1) {
                update.set(Conversation::getUser1Unread, 0);
            } else {
                update.set(Conversation::getUser2Unread, 0);
            }
            conversationMapper.update(null, update);

            messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getReceiverId, userId)
                .set(Message::getIsRead, true));
        }

        List<Message> messages = messageMapper.selectList(
            new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getCreatedAt)
        );

        for (Message msg : messages) {
            User sender = userService.getById(msg.getSenderId());
            if (sender != null) {
                sender.setIsOnline(chatWebSocketHandler.isOnline(msg.getSenderId()));
            }
            msg.setSender(sender);
        }
        return messages;
    }

    // 获取未读消息数
    public int getUnreadCount(Long userId) {
        List<Conversation> convs = conversationMapper.selectList(
            new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUser1Id, userId)
                .or()
                .eq(Conversation::getUser2Id, userId)
        );

        int total = 0;
        for (Conversation conv : convs) {
            if (conv.getUser1Id().equals(userId)) {
                total += conv.getUser1Unread() != null ? conv.getUser1Unread() : 0;
            } else {
                total += conv.getUser2Unread() != null ? conv.getUser2Unread() : 0;
            }
        }
        return total;
    }

    // 获取好友列表（互相关注）
    public List<User> getFriends(Long userId) {
        List<User> users = followService.getMutualFollows(userId);
        for (User user : users) {
            if (user != null && user.getId() != null) {
                user.setIsOnline(chatWebSocketHandler.isOnline(user.getId()));
            }
        }
        return users;
    }
}
