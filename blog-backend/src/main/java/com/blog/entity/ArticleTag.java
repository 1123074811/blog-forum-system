package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("article_tags")
public class ArticleTag {
    private Long articleId;
    private Long tagId;
}
