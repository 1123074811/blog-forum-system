package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("announcements")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String type; // info, success, warning, error
    private Boolean isPinned;
    private Boolean isActive;
    private Integer sortOrder;
    private Long createdBy;
    private String createdAt;
    private String updatedAt;
}