package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.pojo.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

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

    @Select("SELECT LEFT(created_at, 10) AS dateKey, COUNT(*) AS cnt " +
            "FROM articles WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY dateKey ORDER BY dateKey")
    List<Map<String, Object>> countByDateRange(@Param("start") String start, @Param("end") String end);

    @Select("SELECT LEFT(created_at, 7) AS dateKey, COUNT(*) AS cnt " +
            "FROM articles WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY dateKey ORDER BY dateKey")
    List<Map<String, Object>> countByMonthRange(@Param("start") String start, @Param("end") String end);

    @Select("SELECT LEFT(created_at, 4) AS dateKey, COUNT(*) AS cnt " +
            "FROM articles WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY dateKey ORDER BY dateKey")
    List<Map<String, Object>> countByYearRange(@Param("start") String start, @Param("end") String end);

    @Select("SELECT status, COUNT(*) AS cnt FROM articles " +
            "WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY status")
    List<Map<String, Object>> countByStatusInRange(@Param("start") String start, @Param("end") String end);

    @Select("SELECT user_id AS userId, COUNT(*) AS cnt FROM articles " +
            "WHERE created_at >= #{start} AND created_at < #{end} " +
            "GROUP BY user_id ORDER BY cnt DESC LIMIT #{limit}")
    List<Map<String, Object>> countActiveAuthors(@Param("start") String start, @Param("end") String end, @Param("limit") int limit);
}
