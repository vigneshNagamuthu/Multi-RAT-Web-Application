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
import java.util.Map;
import java.util.Set;

@Component
public class StreamingWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("Streaming WebSocket connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("Streaming WebSocket disconnected: " + session.getId());
    }

    /**
     * Send real metrics from FFmpeg (called by VideoStreamingService)
     */
    public void sendMetricsToClients(Map<String, Object> metrics) {
        try {
            String json = objectMapper.writeValueAsString(metrics);
            sendToAllSessions(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when streaming stops
     */
    public void stopRealMetrics() {
        // Placeholder for future cleanup if needed
    }

    private void sendToAllSessions(String message) {
        synchronized (sessions) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}