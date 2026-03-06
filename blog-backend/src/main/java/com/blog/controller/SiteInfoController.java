package com.blog.controller;

import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.entity.SiteInfo;
import com.blog.service.SiteInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SiteInfoController {

    private final SiteInfoService siteInfoService;

    @GetMapping("/site-info")
    public ApiResponse<SiteInfo> getSiteInfo() {
        return ApiResponse.success(siteInfoService.getSiteInfo());
    }

    @PutMapping("/admin/site-info")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SiteInfo> updateSiteInfo(@RequestBody SiteInfo siteInfo) {
        return ApiResponse.success(siteInfoService.updateSiteInfo(siteInfo));
    }
}