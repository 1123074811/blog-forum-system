package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.pojo.entity.Follow;
import com.blog.pojo.entity.User;

import java.util.List;

public interface FollowService extends IService<Follow> {
    void follow(Long followerId, Long followingId);
    void unfollow(Long followerId, Long followingId);
    boolean isFollowing(Long followerId, Long followingId);
    long getFollowerCount(Long userId);
    long getFollowingCount(Long userId);
    List<User> getMutualFollows(Long userId);
}
