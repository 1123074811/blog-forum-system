package com.blog.pojo.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String username;
    private String code;
    private String password;
}
