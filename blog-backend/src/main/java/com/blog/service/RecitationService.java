package com.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.pojo.entity.Recitation;

public interface RecitationService extends IService<Recitation> {
    Page<Recitation> getUserRecitations(Long userId, int page, int limit);
    Recitation getLatestUnfinished(Long userId);
}
