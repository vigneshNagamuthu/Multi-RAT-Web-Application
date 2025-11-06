package com.example.demo.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class StreamingWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final ObjectMapper objectMapper;
    private boolean isStreaming = false;

    public StreamingWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("✅ Streaming WebSocket connected: " + session.getId());
        
        // Send initial "stopped" status to new clients
        if (!isStreaming) {
            sendStoppedStatus();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("❌ Streaming WebSocket disconnected: " + session.getId());
    }

    /**
     * Send real metrics from FFmpeg (called by VideoStreamingService)
     */
    public void sendMetricsToClients(Map<String, Object> metrics) {
        isStreaming = true;
        
        try {
            String json = objectMapper.writeValueAsString(metrics);
            sendToAllSessions(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop real metrics and clear display
     */
    public void stopRealMetrics() {
        System.out.println("🛑 Stopping real metrics");
        isStreaming = false;
        sendStoppedStatus();
    }

    /**
     * Send stopped status with all metrics at 0
     */
    private void sendStoppedStatus() {
        try {
            Map<String, Object> stoppedMetrics = new HashMap<>();
            stoppedMetrics.put("frameRate", 0);
            stoppedMetrics.put("bitrate", 0);
            stoppedMetrics.put("droppedFrames", 0);
            stoppedMetrics.put("latency", 0);
            stoppedMetrics.put("packetLoss", 0);
            stoppedMetrics.put("scheduler", "LRTT");
            stoppedMetrics.put("port", 6060);
            stoppedMetrics.put("status", "stopped");
            stoppedMetrics.put("timestamp", System.currentTimeMillis());
            
            String json = objectMapper.writeValueAsString(stoppedMetrics);
            sendToAllSessions(json);
            
            System.out.println("✅ Stopped status sent - metrics cleared to 0");
        } catch (Exception e) {
            e.printStackTrace();
        }
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