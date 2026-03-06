package com.blog.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.List;

@Data
@TableName("albums")
public class Album {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String coverUrl;
    @TableField("is_public")
    private Boolean isPublic;
    @TableField("is_anonymous")
    private Boolean isAnonymous;
    private Integer mediaCount;
    private String createdAt;
    private String updatedAt;

    @TableField(exist = false)
    private List<String> coverUrls; // 前3张图片URL用于堆叠展示
    
    @TableField(exist = false)
    private String nickname; // 发布人昵称
    
    @TableField(exist = false)
    private String avatar; // 发布人头像
}
