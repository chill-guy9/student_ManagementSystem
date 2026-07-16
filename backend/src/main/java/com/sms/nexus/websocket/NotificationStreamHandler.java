package com.sms.nexus.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStreamHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    // Map: adminId -> session
    private static final Map<String, WebSocketSession> adminSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String adminId = (String) session.getAttributes().get("adminId");
        if (adminId != null) {
            adminSessions.put(adminId, session);
            log.info("Notification WebSocket connected: adminId={}", adminId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
        String type = (String) msg.get("type");

        if ("ping".equals(type)) {
            sendToSession(session, Map.of("type", "pong", "data", System.currentTimeMillis()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String adminId = (String) session.getAttributes().get("adminId");
        if (adminId != null) {
            adminSessions.remove(adminId);
            log.info("Notification WebSocket disconnected: adminId={}, status={}", adminId, status);
        }
    }

    /**
     * Send a notification to a specific admin.
     */
    public void sendToAdmin(String adminId, Map<String, Object> data) {
        WebSocketSession session = adminSessions.get(adminId);
        if (session != null && session.isOpen()) {
            sendToSession(session, data);
        }
    }

    private void sendToSession(WebSocketSession session, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("Failed to send WebSocket message", e);
        }
    }
}
