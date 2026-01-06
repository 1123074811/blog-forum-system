package com.blog.entity;

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
    private Integer mediaCount;
    private String createdAt;
    private String updatedAt;

    @TableField(exist = false)
    private List<String> coverUrls; // 前3张图片URL用于堆叠展示
}
