package com.blog.pojo.dto;

import lombok.Data;

@Data
public class RevokeSessionsRequest {
    private Long userId;
    private String role;
    private boolean revokeAll;
}
