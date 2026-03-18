package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.pojo.entity.IpBlacklist;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface IpBlacklistMapper extends BaseMapper<IpBlacklist> {

    @Select("SELECT * FROM ip_blacklist WHERE ip = #{ip} AND status = 1 LIMIT 1")
    IpBlacklist findActiveByIp(@Param("ip") String ip);

    @Insert("INSERT INTO ip_blacklist (ip, reason, ban_type, expire_time, status, created_at, updated_at) " +
            "VALUES (#{ip}, #{reason}, #{banType}, #{expireTime}, 1, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "reason = #{reason}, " +
            "ban_type = #{banType}, " +
            "expire_time = #{expireTime}, " +
            "status = 1, " +
            "updated_at = NOW()")
    int upsertBan(@Param("ip") String ip,
                  @Param("reason") String reason,
                  @Param("banType") Integer banType,
                  @Param("expireTime") LocalDateTime expireTime);

    @Delete("DELETE FROM ip_blacklist WHERE ip = #{ip}")
    int deleteByIp(@Param("ip") String ip);
}
