package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.pojo.entity.User;

public interface UserService extends IService<User> {
    User findByUsername(String username);
    User findByEmail(String email);
    User register(String username, String password, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    void clearUserCache(Long id, String username);
}
