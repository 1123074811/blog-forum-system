package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.pojo.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.service.UserService;
import com.blog.constant.AppConstants;
import com.blog.util.CacheUtil;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final CacheUtil cacheUtil;

    @Override
    public User findByUsername(String username) {
        return cacheUtil.getWithPassThrough(
            AppConstants.CACHE_USER_PREFIX + "name:" + username, User.class, AppConstants.CACHE_USER_TTL_MINUTES, TimeUnit.MINUTES,
            () -> getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username))
        );
    }

    @Override
    public User findByEmail(String email) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }

    @Override
    public User getById(java.io.Serializable id) {
        return cacheUtil.getWithPassThrough(
            AppConstants.CACHE_USER_PREFIX + id, User.class, AppConstants.CACHE_USER_TTL_MINUTES, TimeUnit.MINUTES,
            () -> super.getById(id)
        );
    }

    @Override
    public User register(String username, String password, String email) {
        User user = new User();
        user.setUsername(username);
        user.setNickname(AppConstants.DEFAULT_NICKNAME_PREFIX + System.currentTimeMillis() % 100000);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(AppConstants.ROLE_USER);
        user.setCreatedAt(DateUtil.now());
        user.setUpdatedAt(DateUtil.now());
        save(user);
        return user;
    }

    @Override
    public boolean existsByUsername(String username) {
        return count(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return count(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0;
    }

    @Override
    public void clearUserCache(Long id, String username) {
        cacheUtil.delete(AppConstants.CACHE_USER_PREFIX + id);
        cacheUtil.delete(AppConstants.CACHE_USER_PREFIX + "name:" + username);
    }
}
