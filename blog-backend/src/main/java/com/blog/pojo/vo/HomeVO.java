package com.blog.pojo.vo;

import com.blog.pojo.dto.PageResponse;
import com.blog.pojo.entity.Announcement;
import com.blog.pojo.entity.Category;
import com.blog.pojo.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeVO {
    private PageResponse<ArticleVO> articles;
    private List<ArticleVO> hotArticles;
    private List<Category> categories;
    private List<Tag> tags;
    private List<Announcement> announcements;
}
