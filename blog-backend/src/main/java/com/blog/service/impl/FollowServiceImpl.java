package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.entity.Follow;
import com.blog.mapper.FollowMapper;
import com.blog.service.FollowService;
import com.blog.util.DateUtil;
import org.springframework.stereotype.Service;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Override
    public void follow(Long followerId, Long followingId) {
        if (!isFollowing(followerId, followingId)) {
            Follow follow = new Follow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            follow.setCreatedAt(DateUtil.now());
            save(follow);
        }
    }

    @Override
    public void unfollow(Long followerId, Long followingId) {
        remove(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowingId, followingId));
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return count(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowingId, followingId)) > 0;
    }

    @Override
    public long getFollowerCount(Long userId) {
        return count(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowingId, userId));
    }

    @Override
    public long getFollowingCount(Long userId) {
        return count(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId));
    }
}
