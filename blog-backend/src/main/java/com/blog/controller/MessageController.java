package com.blog.controller;

import com.blog.dto.ApiResponse;
import com.blog.entity.Conversation;
import com.blog.entity.Message;
import com.blog.entity.User;
import com.blog.service.MessageService;
import com.blog.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;
    private final JwtUtil jwtUtil;

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @GetMapping("/conversations")
    public ApiResponse<List<Conversation>> getConversations(HttpServletRequest request) {
        return ApiResponse.success(messageService.getConversations(getUserId(request)));
    }

    @GetMapping("/conversations/{id}")
    public ApiResponse<List<Message>> getMessages(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(messageService.getMessages(id, getUserId(request)));
    }

    @PostMapping("/send")
    public ApiResponse<Message> sendMessage(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        String content = (String) body.get("content");
        String type = (String) body.getOrDefault("type", "text");
        String fileUrl = (String) body.get("fileUrl");
        String fileName = (String) body.get("fileName");

        Message msg = messageService.sendMessage(getUserId(request), receiverId, content, type, fileUrl, fileName);
        if (msg == null) {
            return ApiResponse.error("非互关好友只能发送一条消息");
        }
        return ApiResponse.success(msg);
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : "";
        String newName = UUID.randomUUID() + ext;
        String uploadDir = System.getProperty("user.dir") + "/uploads/messages/";
        new File(uploadDir).mkdirs();
        file.transferTo(new File(uploadDir + newName));
        String url = "/uploads/messages/" + newName;
        return ApiResponse.success(Map.of("url", url, "name", originalName != null ? originalName : newName));
    }

    @GetMapping("/unread")
    public ApiResponse<Integer> getUnreadCount(HttpServletRequest request) {
        return ApiResponse.success(messageService.getUnreadCount(getUserId(request)));
    }

    @GetMapping("/friends")
    public ApiResponse<List<User>> getFriends(HttpServletRequest request) {
        return ApiResponse.success(messageService.getFriends(getUserId(request)));
    }

    @GetMapping("/check-mutual/{userId}")
    public ApiResponse<Boolean> checkMutual(@PathVariable Long userId, HttpServletRequest request) {
        return ApiResponse.success(messageService.isMutualFollow(getUserId(request), userId));
    }
}
