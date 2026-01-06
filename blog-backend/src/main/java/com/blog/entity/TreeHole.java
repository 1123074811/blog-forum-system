package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tree_hole")
public class TreeHole {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private String color;
    private String createdAt;
}
