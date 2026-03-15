package com.blog.pojo.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String captchaId;
    private String captchaCode;
    private String emailCode; // 邮箱验证码
}
