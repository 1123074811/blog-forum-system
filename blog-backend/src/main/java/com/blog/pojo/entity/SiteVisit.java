package com.blog.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("site_visits")
public class SiteVisit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String visitDate;  // 日期 yyyy-MM-dd
    private Integer pv;        // 页面浏览量
    private Integer uv;        // 独立访客数
    private Integer newUsers;  // 新增用户
    private Integer newArticles; // 新增文章
    private Integer newComments; // 新增评论
}
