package com.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.pojo.entity.SiteInfo;
import com.blog.mapper.SiteInfoMapper;
import com.blog.service.SiteInfoService;
import com.blog.util.CacheUtil;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SiteInfoServiceImpl extends ServiceImpl<SiteInfoMapper, SiteInfo> implements SiteInfoService {

    private final CacheUtil cacheUtil;
    private static final String CACHE_SITE_INFO = "site:info";
    private static final long CACHE_TTL_MINUTES = 30L;

    @Override
    public SiteInfo getSiteInfo() {
        return cacheUtil.getWithPassThrough(
            CACHE_SITE_INFO, SiteInfo.class, CACHE_TTL_MINUTES, TimeUnit.MINUTES,
            () -> this.list().stream().findFirst().orElse(null)
        );
    }

    @Override
    public SiteInfo updateSiteInfo(SiteInfo siteInfo) {
        SiteInfo existing = this.list().stream().findFirst().orElse(null);
        if (existing != null) {
            siteInfo.setId(existing.getId());
            siteInfo.setCreatedAt(existing.getCreatedAt());
        }
        siteInfo.setUpdatedAt(DateUtil.getCurrentDateTime());
        this.saveOrUpdate(siteInfo);
        cacheUtil.delete(CACHE_SITE_INFO);
        return siteInfo;
    }
}