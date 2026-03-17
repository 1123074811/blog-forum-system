package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.pojo.entity.ArticleFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleFavoriteMapper extends BaseMapper<ArticleFavorite> {

    @Select("<script>" +
            "SELECT article_id AS articleId, COUNT(*) AS favoriteCount " +
            "FROM article_favorites " +
            "WHERE article_id IN " +
            "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "GROUP BY article_id" +
            "</script>")
    List<Map<String, Object>> batchCountByArticleIds(@Param("articleIds") List<Long> articleIds);
}
