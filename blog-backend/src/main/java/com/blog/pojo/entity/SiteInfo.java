package com.blog.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("site_info")
public class SiteInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String siteName;
    private String siteDescription;
    private String siteKeywords;
    private String siteLogo;
    private String siteFavicon;
    private String icpNumber;
    private String policeNumber;
    private String contactEmail;
    private String contactPhone;
    private String contactAddress;
    private String contactQq;
    private String contactWechat;
    private String githubUrl;
    private String giteeUrl;
    private String createdAt;
    private String updatedAt;
}