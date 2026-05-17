package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.pojo.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("<script>" +
            "SELECT article_id AS articleId, COUNT(*) AS commentCount " +
            "FROM comments " +
            "WHERE article_id IN " +
            "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "GROUP BY article_id" +
            "</script>")
    List<Map<String, Object>> batchCountByArticleIds(@Param("articleIds") List<Long> articleIds);

    @Select("SELECT LEFT(created_at, 10) AS dateKey, COUNT(*) AS cnt " +
            "FROM comments WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY dateKey ORDER BY dateKey")
    List<Map<String, Object>> countByDateRange(@Param("start") String start, @Param("end") String end);

    @Select("SELECT LEFT(created_at, 7) AS dateKey, COUNT(*) AS cnt " +
            "FROM comments WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY dateKey ORDER BY dateKey")
    List<Map<String, Object>> countByMonthRange(@Param("start") String start, @Param("end") String end);

    @Select("SELECT LEFT(created_at, 4) AS dateKey, COUNT(*) AS cnt " +
            "FROM comments WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY dateKey ORDER BY dateKey")
    List<Map<String, Object>> countByYearRange(@Param("start") String start, @Param("end") String end);
}
