package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.pojo.entity.SiteVisit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SiteVisitMapper extends BaseMapper<SiteVisit> {

    @Select("SELECT COALESCE(SUM(uv), 0) FROM site_visits")
    int sumTotalUv();
}
