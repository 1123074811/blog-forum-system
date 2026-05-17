package com.blog.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("reports")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reason;
    private String description;
    private String status;
    private Long handledBy;
    private String handledAt;
    private String handleNote;
    private String createdAt;

    @TableField(exist = false)
    private String reporterName;
    @TableField(exist = false)
    private String targetTitle;
    @TableField(exist = false)
    private String targetContent;
    @TableField(exist = false)
    private String handlerName;
}
