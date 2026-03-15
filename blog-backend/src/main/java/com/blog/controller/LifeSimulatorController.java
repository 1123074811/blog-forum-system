package com.blog.controller;

import com.blog.annotation.RateLimit;
import com.blog.pojo.dto.ApiResponse;
import com.blog.service.ZhipuAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/life-simulator")
@RequiredArgsConstructor
public class LifeSimulatorController {

    private final ZhipuAiService zhipuAiService;

    @PostMapping("/event")
    @RateLimit(key = "life-simulator", count = 100, time = 86400, limitType = RateLimit.LimitType.USER, message = "每日模拟次数已达上限")
    public ApiResponse<Map<String, Object>> generateEvent(@RequestBody Map<String, Object> request, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ApiResponse.error("请先登录");
        }
        int age = (int) request.get("age");
        Map<String, Object> attributes = (Map<String, Object>) request.get("attributes");
        Number wealthNum = (Number) request.get("wealth");
        long wealth = wealthNum.longValue();
        List<String> recentEvents = (List<String>) request.get("recentEvents");

        Map<String, Object> event = zhipuAiService.generateLifeEvent(age, attributes, wealth, recentEvents);
        return ApiResponse.success(event);
    }
}
