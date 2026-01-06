package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;        // 接收者
    private Long fromUserId;    // 发送者（系统消息为null）
    private String type;        // like/favorite/follow/article/system
    private Long targetId;      // 关联的文章/用户ID
    private String content;     // 消息内容
    private Boolean isRead;
    private String createdAt;

    @TableField(exist = false)
    private String fromUsername;
    @TableField(exist = false)
    private String fromAvatar;
}
