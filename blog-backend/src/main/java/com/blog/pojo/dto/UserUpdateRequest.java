package com.blog.pojo.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 用户更新请求
 */
@Data
public class UserUpdateRequest {
    @Size(max = 255, message = "头像URL长度不能超过255")
    private String avatar;

    @Size(min = 2, max = 50, message = "昵称长度必须在2-50之间")
    private String nickname;

    @Size(max = 200, message = "个人简介长度不能超过200")
    private String bio;
}
