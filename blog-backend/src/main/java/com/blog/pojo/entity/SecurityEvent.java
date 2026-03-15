package com.blog.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("security_event")
public class SecurityEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;
    private Long userId;
    private String ip;
    private String userAgent;
    private String method;
    private String path;
    private String operation;
    private String result;
    private String reasonCode;
    private String detail;
    private String createdAt;
}
