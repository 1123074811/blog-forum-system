package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.pojo.dto.PageResponse;
import com.blog.exception.BusinessException;
import com.blog.exception.ErrorCode;
import org.springframework.security.core.Authentication;

/**
 * 基础Controller，提供公共方法
 */
public abstract class BaseController {

    /**
     * 获取当前登录用户ID
     */
    protected Long getCurrentUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return (Long) auth.getPrincipal();
    }

    /**
     * 获取当前登录用户ID（可选）
     */
    protected Long getCurrentUserIdOptional(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }

    /**
     * 检查权限
     */
    protected void checkPermission(Long resourceOwnerId, Long currentUserId) {
        if (!resourceOwnerId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
    }

    /**
     * 转换分页结果
     */
    protected <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getRecords(),
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize()
        );
    }
}
