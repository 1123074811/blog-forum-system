package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.ApiResponse;
import com.blog.dto.PageResponse;
import com.blog.entity.Recitation;
import com.blog.service.RecitationService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recitation")
@RequiredArgsConstructor
public class RecitationController {

    private final RecitationService recitationService;

    @GetMapping
    public ApiResponse<PageResponse<Recitation>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Page<Recitation> result = recitationService.getUserRecitations(userId, page, limit);
        return ApiResponse.success(new PageResponse<>(result.getRecords(), result.getTotal(), page, limit));
    }

    @GetMapping("/latest")
    public ApiResponse<Recitation> getLatest(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(recitationService.getLatestUnfinished(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Recitation> getById(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Recitation r = recitationService.getById(id);
        if (r == null || !r.getUserId().equals(userId)) {
            return ApiResponse.error("记录不存在");
        }
        return ApiResponse.success(r);
    }

    @PostMapping
    public ApiResponse<Recitation> create(@RequestBody Recitation recitation, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        recitation.setUserId(userId);
        recitation.setProgress(0);
        recitation.setDuration(0);
        recitation.setCompleted(false);
        recitation.setCreatedAt(DateUtil.now());
        recitation.setUpdatedAt(DateUtil.now());
        recitationService.save(recitation);
        return ApiResponse.success(recitation);
    }

    @PutMapping("/{id}")
    public ApiResponse<Recitation> update(@PathVariable Long id, @RequestBody Recitation recitation, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Recitation existing = recitationService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return ApiResponse.error("记录不存在");
        }
        existing.setUserInput(recitation.getUserInput());
        existing.setProgress(recitation.getProgress());
        existing.setDuration(recitation.getDuration());
        existing.setTimingEnabled(recitation.getTimingEnabled());
        if (recitation.getCompleted() != null) {
            existing.setCompleted(recitation.getCompleted());
        }
        existing.setUpdatedAt(DateUtil.now());
        recitationService.updateById(existing);
        return ApiResponse.success(existing);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Recitation r = recitationService.getById(id);
        if (r == null || !r.getUserId().equals(userId)) {
            return ApiResponse.error("记录不存在");
        }
        recitationService.removeById(id);
        return ApiResponse.success("删除成功");
    }
}
