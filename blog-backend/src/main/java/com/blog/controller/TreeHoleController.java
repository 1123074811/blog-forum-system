package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.entity.TreeHole;
import com.blog.service.SensitiveWordFilterService;
import com.blog.service.TreeHoleService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tree-hole")
@RequiredArgsConstructor
public class TreeHoleController {

    private final TreeHoleService treeHoleService;
    private final SensitiveWordFilterService sensitiveWordFilterService;

    @GetMapping
    public ApiResponse<List<TreeHole>> list() {
        List<TreeHole> list = treeHoleService.list(
            new LambdaQueryWrapper<TreeHole>().orderByDesc(TreeHole::getId).last("LIMIT 100")
        );
        return ApiResponse.success(list);
    }

    @PostMapping
    public ApiResponse<TreeHole> create(@RequestBody Map<String, String> body) {
        TreeHole hole = new TreeHole();
        hole.setContent(sensitiveWordFilterService.filter(body.get("content")));
        hole.setColor(body.get("color"));
        hole.setCreatedAt(DateUtil.now());
        treeHoleService.save(hole);
        return ApiResponse.success(hole);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        treeHoleService.removeById(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Boolean> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids != null && !ids.isEmpty()) {
            treeHoleService.removeByIds(ids);
        }
        return ApiResponse.success(true);
    }
}
