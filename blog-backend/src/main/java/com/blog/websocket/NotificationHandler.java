package com.blog.websocket;

import com.blog.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class NotificationHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = getQueryParam(session, "token");
        if (token == null || !jwtUtil.validateToken(token) || !jwtUtil.isAccessToken(token)) {
            closeSilently(session, CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            return;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            closeSilently(session, CloseStatus.NOT_ACCEPTABLE.withReason("Invalid user"));
            return;
        }
        sessions.put(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.values().remove(session);
    }

    public void sendNotification(Long userId, Object notification) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(message));
            } catch (IOException ignored) {
            }
        }
    }

    public void broadcast(Object notification) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    String message = objectMapper.writeValueAsString(notification);
                    session.sendMessage(new TextMessage(message));
                } catch (IOException ignored) {
                }
            }
        });
    }

    private String getQueryParam(WebSocketSession session, String key) {
        if (session.getUri() == null || session.getUri().getQuery() == null) {
            return null;
        }
        String[] pairs = session.getUri().getQuery().split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && key.equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }

    private void closeSilently(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignored) {
        }
    }
}
