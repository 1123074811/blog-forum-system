package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("conversations")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long user1Id;
    private Long user2Id;
    private Long lastMessageId;
    private String lastMessageTime;
    private Integer user1Unread;
    private Integer user2Unread;
    private String createdAt;

    @TableField(exist = false)
    private User otherUser;
    @TableField(exist = false)
    private Message lastMessage;
}
