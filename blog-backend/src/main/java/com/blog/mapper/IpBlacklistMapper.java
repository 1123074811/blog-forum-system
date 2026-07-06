package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.pojo.entity.IpBlacklist;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface IpBlacklistMapper extends BaseMapper<IpBlacklist> {

    @Select("SELECT * FROM ip_blacklist WHERE ip = #{ip} AND status = 1 LIMIT 1")
    IpBlacklist findActiveByIp(@Param("ip") String ip);

    @Select("SELECT * FROM ip_blacklist " +
            "WHERE status = 1 AND (expire_time IS NULL OR expire_time > NOW()) " +
            "ORDER BY created_at DESC")
    List<IpBlacklist> findEffectiveBans();

    @Select("SELECT * FROM ip_blacklist " +
            "WHERE ip = #{ip} AND status = 1 AND (expire_time IS NULL OR expire_time > NOW()) LIMIT 1")
    IpBlacklist findEffectiveByIp(@Param("ip") String ip);

    @Update("UPDATE ip_blacklist SET status = 0, updated_at = NOW() " +
            "WHERE status = 1 AND expire_time IS NOT NULL AND expire_time <= NOW()")
    int markExpiredInactive();

    @Insert("INSERT INTO ip_blacklist (ip, reason, location, ban_type, expire_time, status, created_at, updated_at) " +
            "VALUES (#{ip}, #{reason}, #{location}, #{banType}, #{expireTime}, 1, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "reason = #{reason}, " +
            "location = #{location}, " +
            "ban_type = #{banType}, " +
            "expire_time = #{expireTime}, " +
            "status = 1, " +
            "updated_at = NOW()")
    int upsertBan(@Param("ip") String ip,
                  @Param("reason") String reason,
                  @Param("location") String location,
                  @Param("banType") Integer banType,
                  @Param("expireTime") LocalDateTime expireTime);

    @Delete("DELETE FROM ip_blacklist WHERE ip = #{ip}")
    int deleteByIp(@Param("ip") String ip);
}
