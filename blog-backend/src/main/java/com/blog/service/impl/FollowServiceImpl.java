package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.pojo.entity.Follow;
import com.blog.pojo.entity.User;
import com.blog.mapper.FollowMapper;
import com.blog.service.FollowService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_FOLLOWER_COUNT = "follow:follower:";
    private static final String CACHE_FOLLOWING_COUNT = "follow:following:";
    private static final long FOLLOW_CACHE_TTL = 10L;

    @Override
    public void follow(Long followerId, Long followingId) {
        if (!isFollowing(followerId, followingId)) {
            Follow follow = new Follow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            follow.setCreatedAt(DateUtil.now());
            save(follow);
            // 清除计数缓存
            evictCountCache(followerId, followingId);
        }
    }

    @Override
    public void unfollow(Long followerId, Long followingId) {
        remove(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowingId, followingId));
        evictCountCache(followerId, followingId);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return count(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowingId, followingId)) > 0;
    }

    @Override
    public long getFollowerCount(Long userId) {
        String key = CACHE_FOLLOWER_COUNT + userId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) return ((Number) cached).longValue();
        long count = baseMapper.countFollowers(userId);
        redisTemplate.opsForValue().set(key, count, FOLLOW_CACHE_TTL, TimeUnit.MINUTES);
        return count;
    }

    @Override
    public long getFollowingCount(Long userId) {
        String key = CACHE_FOLLOWING_COUNT + userId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) return ((Number) cached).longValue();
        long count = baseMapper.countFollowing(userId);
        redisTemplate.opsForValue().set(key, count, FOLLOW_CACHE_TTL, TimeUnit.MINUTES);
        return count;
    }

    @Override
    public List<User> getMutualFollows(Long userId) {
        // 单条SQL查询，消除N+1问题
        List<User> mutuals = baseMapper.findMutualFollows(userId);
        mutuals.forEach(u -> u.setPassword(null));
        return mutuals;
    }

    private void evictCountCache(Long followerId, Long followingId) {
        redisTemplate.delete(CACHE_FOLLOWER_COUNT + followingId);
        redisTemplate.delete(CACHE_FOLLOWING_COUNT + followerId);
    }
}
