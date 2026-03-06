package com.blog.converter;

import com.blog.pojo.entity.User;
import com.blog.pojo.vo.UserVO;
import org.springframework.stereotype.Component;

/**
 * 用户实体与VO转换器
 */
@Component
public class UserConverter {

    public UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
