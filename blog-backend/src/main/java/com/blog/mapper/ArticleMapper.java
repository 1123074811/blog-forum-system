package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.pojo.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 批量更新文章浏览量，使用 CASE WHEN 单条 SQL，兼容 MySQL 默认配置
     */
    @Update("<script>" +
            "UPDATE articles SET view_count = CASE id " +
            "<foreach collection='list' item='item'>" +
            "WHEN #{item.articleId} THEN #{item.viewCount} " +
            "</foreach>" +
            "END " +
            "WHERE id IN " +
            "<foreach collection='list' item='item' open='(' separator=',' close=')'>" +
            "#{item.articleId}" +
            "</foreach>" +
            "</script>")
    void batchUpdateViewCount(@Param("list") List<Map<String, Object>> list);
}
