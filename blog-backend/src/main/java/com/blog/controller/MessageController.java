package com.blog.controller;

import com.blog.context.BaseContext;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.entity.Conversation;
import com.blog.pojo.entity.Message;
import com.blog.pojo.entity.User;
import com.blog.service.MessageService;
import com.blog.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;
    private final MinioService minioService;

    private Long requireCurrentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "???");
        }
        return userId;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<Conversation>> getConversations() {
        return ApiResponse.success(messageService.getConversations(requireCurrentUserId()));
    }

    @GetMapping("/conversations/{id}")
    public ApiResponse<List<Message>> getMessages(@PathVariable Long id) {
        return ApiResponse.success(messageService.getMessages(id, requireCurrentUserId()));
    }

    @PostMapping("/send")
    public ApiResponse<Message> sendMessage(@RequestBody Map<String, Object> body) {
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        String content = (String) body.get("content");
        String type = (String) body.getOrDefault("type", "text");
        String fileUrl = (String) body.get("fileUrl");
        String fileName = (String) body.get("fileName");

        Message msg = messageService.sendMessage(requireCurrentUserId(), receiverId, content, type, fileUrl, fileName);
        if (msg == null) {
            return ApiResponse.error("?????????????");
        }
        return ApiResponse.success(msg);
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String originalName = file.getOriginalFilename();
        try {
            String url = minioService.upload(file, "messages");
            return ApiResponse.success(Map.of("url", url, "name", originalName != null ? originalName : "file"));
        } catch (Exception e) {
            return ApiResponse.error("??????: " + e.getMessage());
        }
    }

    @GetMapping("/unread")
    public ApiResponse<Integer> getUnreadCount() {
        return ApiResponse.success(messageService.getUnreadCount(requireCurrentUserId()));
    }

    @GetMapping("/friends")
    public ApiResponse<List<User>> getFriends() {
        return ApiResponse.success(messageService.getFriends(requireCurrentUserId()));
    }

    @GetMapping("/check-mutual/{userId}")
    public ApiResponse<Boolean> checkMutual(@PathVariable Long userId) {
        return ApiResponse.success(messageService.isMutualFollow(requireCurrentUserId(), userId));
    }
}
