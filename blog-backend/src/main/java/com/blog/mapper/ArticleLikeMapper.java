package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.pojo.entity.ArticleLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {

    /**
     * 批量查询文章点赞数，避免 N+1 查询
     * 返回 [{articleId: x, likeCount: y}, ...]
     */
    @Select("<script>" +
            "SELECT article_id AS articleId, COUNT(*) AS likeCount " +
            "FROM article_likes " +
            "WHERE article_id IN " +
            "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "GROUP BY article_id" +
            "</script>")
    List<Map<String, Object>> batchCountByArticleIds(@Param("articleIds") List<Long> articleIds);
}
