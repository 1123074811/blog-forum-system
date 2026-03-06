package com.blog.pojo.dto;

import lombok.Data;

@Data
public class VerifyCodeRequest {
    private String username;
    private String code;
}
