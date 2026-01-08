package com.blog.controller;

import com.blog.dto.ApiResponse;
import com.blog.dto.TranslateRequest;
import com.blog.service.ZhipuAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TranslateController {

    private final ZhipuAiService zhipuAiService;

    @PostMapping("/translate")
    public ApiResponse<String> translate(@RequestBody TranslateRequest request) {
        log.info("收到翻译请求: text={}, context={}", request.getText(), request.getContext());
        
        try {
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                log.warn("翻译文本为空");
                return ApiResponse.error("翻译文本不能为空");
            }

            String context = request.getContext() != null ? request.getContext() : "database_field";
            log.info("开始调用智谱AI翻译: text={}, context={}", request.getText().trim(), context);
            
            String result = zhipuAiService.translateDatabaseName(request.getText().trim(), context);
            
            if (result != null && !result.isEmpty()) {
                log.info("翻译成功: {} -> {}", request.getText().trim(), result);
                return ApiResponse.success(result);
            } else {
                log.warn("翻译结果为空: {}", request.getText().trim());
                return ApiResponse.error("翻译失败，请稍后重试");
            }
        } catch (Exception e) {
            log.error("翻译接口调用失败: text={}", request.getText(), e);
            return ApiResponse.error("翻译服务暂时不可用");
        }
    }
}