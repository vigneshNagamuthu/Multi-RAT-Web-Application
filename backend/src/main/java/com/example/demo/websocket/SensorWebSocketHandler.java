package com.example.demo.websocket;

import com.example.demo.model.SensorPacket;
import com.example.demo.service.MPTCPPacketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class SensorWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isSchedulerStarted = false;

    @Autowired(required = false)
    private MPTCPPacketService mptcpPacketService;

    public SensorWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("✅ Sensor WebSocket connected: " + session.getId());
        
        // Start sending packets every 500ms (only once)
        if (!isSchedulerStarted) {
            startSendingPackets();
            isSchedulerStarted = true;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("❌ Sensor WebSocket disconnected: " + session.getId());
    }

    private void startSendingPackets() {
        scheduler.scheduleAtFixedRate(() -> {
            if (sessions.isEmpty()) {
                return;
            }

            // ONLY send packets if MPTCP capture is active
            if (mptcpPacketService != null && mptcpPacketService.isCapturing()) {
                List<SensorPacket> packets = mptcpPacketService.getRecentPackets(5);
                
                if (!packets.isEmpty()) {
                    System.out.println("📡 Sending " + packets.size() + " REAL packets to frontend");
                    
                    try {
                        String json = objectMapper.writeValueAsString(packets);
                        
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
            // If capture is NOT active, don't send anything (no dummy data!)
        }, 0, 500, TimeUnit.MILLISECONDS);
    }
}