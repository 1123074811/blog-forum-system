package com.blog.pojo.dto;

import lombok.Data;

@Data
public class BanIpRequest {
    private String ip;
    private String reason;
    private Integer durationMinutes;
}
