package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.pojo.entity.Follow;
import com.blog.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    /**
     * 一次SQL查询互关好友，消除N+1问题
     */
    @Select("SELECT u.id, u.username, u.nickname, u.avatar, u.bio FROM follows f1 " +
            "JOIN follows f2 ON f1.following_id = f2.follower_id AND f2.following_id = f1.follower_id " +
            "JOIN users u ON u.id = f1.following_id " +
            "WHERE f1.follower_id = #{userId}")
    List<User> findMutualFollows(Long userId);

    /**
     * 批量查询粉丝数和关注数
     */
    @Select("SELECT COUNT(*) FROM follows WHERE following_id = #{userId}")
    long countFollowers(Long userId);

    @Select("SELECT COUNT(*) FROM follows WHERE follower_id = #{userId}")
    long countFollowing(Long userId);
}
