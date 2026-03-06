package com.blog.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@TableName("media")
public class Media {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long albumId;
    private String title;
    private String description;
    private String url;
    private String thumbnailUrl;
    private String type;
    private Long fileSize;
    @TableField("is_public")
    @JsonProperty("isPublic")
    private Boolean isPublic;
    @TableField("is_anonymous")
    @JsonProperty("isAnonymous")
    private Boolean isAnonymous;
    private String source; // 来源: user-用户上传, bing-必应壁纸
    private String createdAt;
    private String updatedAt;

    @TableField(exist = false)
    private String nickname;
    @TableField(exist = false)
    private String avatar;
}
