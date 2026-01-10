package com.blog.controller;

import com.blog.dto.ApiResponse;
import com.blog.service.ZhipuAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/life-simulator")
@RequiredArgsConstructor
public class LifeSimulatorController {

    private final ZhipuAiService zhipuAiService;

    @PostMapping("/event")
    public ApiResponse<Map<String, Object>> generateEvent(@RequestBody Map<String, Object> request) {
        int age = (int) request.get("age");
        Map<String, Object> attributes = (Map<String, Object>) request.get("attributes");
        Number wealthNum = (Number) request.get("wealth");
        long wealth = wealthNum.longValue();
        List<String> recentEvents = (List<String>) request.get("recentEvents");

        Map<String, Object> event = zhipuAiService.generateLifeEvent(age, attributes, wealth, recentEvents);
        return ApiResponse.success(event);
    }
}
