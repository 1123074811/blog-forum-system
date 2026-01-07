package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String type; // text, image, file, emoji
    private String fileUrl;
    private String fileName;
    private Boolean isRead;
    private String createdAt;

    @TableField(exist = false)
    private User sender;
}
