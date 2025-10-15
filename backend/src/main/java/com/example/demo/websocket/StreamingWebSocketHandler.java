package com.example.demo.websocket;

import com.example.demo.model.StreamMetrics;
import com.example.demo.service.DummyDataGenerator;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class StreamingWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final DummyDataGenerator dataGenerator;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isSchedulerStarted = false;

    public StreamingWebSocketHandler(DummyDataGenerator dataGenerator) {
        this.dataGenerator = dataGenerator;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("Streaming WebSocket connected: " + session.getId());
        
        // Start sending metrics every 1 second (only once)
        if (!isSchedulerStarted) {
            startSendingMetrics();
            isSchedulerStarted = true;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("Streaming WebSocket disconnected: " + session.getId());
    }

    private void startSendingMetrics() {
        scheduler.scheduleAtFixedRate(() -> {
            if (sessions.isEmpty()) {
                return;
            }

            StreamMetrics metrics = dataGenerator.generateStreamMetrics();
            
            try {
                String json = objectMapper.writeValueAsString(metrics);
                
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
        }, 0, 1, TimeUnit.SECONDS);
    }
}