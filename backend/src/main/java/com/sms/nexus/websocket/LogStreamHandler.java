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
public class LogStreamHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private static final Map<String, WebSocketSession> subscribers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String adminId = (String) session.getAttributes().get("adminId");
        log.info("Log stream WebSocket connected: adminId={}", adminId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
        String type = (String) msg.get("type");

        switch (type) {
            case "log.subscribe" -> {
                subscribers.put(session.getId(), session);
                sendToSession(session, Map.of("type", "log.subscribed", "data", "Subscribed to log stream"));
                log.info("Admin subscribed to log stream: sessionId={}", session.getId());
            }
            case "log.unsubscribe" -> {
                subscribers.remove(session.getId());
                sendToSession(session, Map.of("type", "log.unsubscribed", "data", "Unsubscribed from log stream"));
            }
            case "ping" -> sendToSession(session, Map.of("type", "pong", "data", System.currentTimeMillis()));
            default -> sendToSession(session, Map.of("type", "error", "data", "Unknown message type: " + type));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        subscribers.remove(session.getId());
        String adminId = (String) session.getAttributes().get("adminId");
        log.info("Log stream WebSocket disconnected: adminId={}, status={}", adminId, status);
    }

    /**
     * Broadcast a new log entry to all subscribers.
     */
    public void broadcastLog(Map<String, Object> logData) {
        Map<String, Object> message = Map.of("type", "log.new", "data", logData);
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize log broadcast message", e);
            return;
        }

        for (WebSocketSession session : subscribers.values()) {
            sendToSession(session, json);
        }
    }

    private void sendToSession(WebSocketSession session, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            sendToSession(session, json);
        } catch (Exception e) {
            log.error("Failed to serialize WebSocket message", e);
        }
    }

    private void sendToSession(WebSocketSession session, String json) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("Failed to send WebSocket message", e);
            }
        }
    }
}
