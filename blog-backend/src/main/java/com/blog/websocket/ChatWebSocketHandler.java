package com.blog.websocket;

import com.blog.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // userId -> session
    private static final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = getTokenFromSession(session);
        if (token != null && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            sessions.put(userId, session);
            log.info("WebSocket connected: userId={}", userId);
            broadcastPresence(userId, true);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 心跳检测
        if ("ping".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String token = getTokenFromSession(session);
        if (token != null && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            sessions.remove(userId);
            log.info("WebSocket disconnected: userId={}", userId);
            broadcastPresence(userId, false);
        }
    }

    public void sendToUser(Long userId, Object message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (Exception e) {
                log.error("Failed to send message to user {}", userId, e);
            }
        }
    }

    public boolean isOnline(Long userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    public int getOnlineUserCount() {
        int count = 0;
        for (WebSocketSession session : sessions.values()) {
            if (session != null && session.isOpen()) {
                count++;
            }
        }
        return count;
    }

    private void broadcastPresence(Long userId, boolean online) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "presence_update");
        payload.put("userId", userId);
        payload.put("online", online);
        sessions.values().forEach(session -> {
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
                } catch (Exception e) {
                    log.error("Failed to broadcast presence for user {}", userId, e);
                }
            }
        });
    }

    private String getTokenFromSession(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null && query.startsWith("token=")) {
            return query.substring(6);
        }
        return null;
    }
}
