package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.pojo.entity.SiteInfo;

public interface SiteInfoService extends IService<SiteInfo> {
    SiteInfo getSiteInfo();
    SiteInfo updateSiteInfo(SiteInfo siteInfo);
}