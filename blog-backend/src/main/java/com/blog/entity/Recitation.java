package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("recitation")
public class Recitation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    @TableField("`content`")
    private String content;
    private String userInput;
    private Integer progress;
    private Integer duration;
    private Boolean timingEnabled;
    private Boolean completed;
    private String createdAt;
    private String updatedAt;
}
