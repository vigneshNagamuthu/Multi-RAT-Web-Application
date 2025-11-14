package com.example.demo.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class SensorWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("Sensor WebSocket connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("Sensor WebSocket disconnected: " + session.getId());
    }
    
    // Broadcast status update to all clients
    public void broadcastStatus(java.util.Map<String, Object> status) {
        try {
            java.util.Map<String, Object> message = new java.util.HashMap<>();
            message.put("type", "status");
            message.put("data", status);
            
            String json = objectMapper.writeValueAsString(message);
            
            synchronized (sessions) {
                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(json));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}