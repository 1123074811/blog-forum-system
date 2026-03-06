package com.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.pojo.entity.SiteInfo;
import com.blog.mapper.SiteInfoMapper;
import com.blog.service.SiteInfoService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteInfoServiceImpl extends ServiceImpl<SiteInfoMapper, SiteInfo> implements SiteInfoService {

    @Override
    public SiteInfo getSiteInfo() {
        return this.list().stream().findFirst().orElse(null);
    }

    @Override
    public SiteInfo updateSiteInfo(SiteInfo siteInfo) {
        SiteInfo existing = getSiteInfo();
        if (existing != null) {
            siteInfo.setId(existing.getId());
            siteInfo.setCreatedAt(existing.getCreatedAt());
        }
        siteInfo.setUpdatedAt(DateUtil.getCurrentDateTime());
        this.saveOrUpdate(siteInfo);
        return siteInfo;
    }
}