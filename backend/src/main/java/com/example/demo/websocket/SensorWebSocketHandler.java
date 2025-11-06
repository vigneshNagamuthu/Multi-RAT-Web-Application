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
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class SensorWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isSchedulerStarted = false;
    
    private int lastSentSequence = 0;  // ✅ Track what we've already sent

    @Autowired(required = false)
    private MPTCPPacketService mptcpPacketService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("✅ Sensor WebSocket connected: " + session.getId());

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
            if (sessions.isEmpty()) return;

            if (mptcpPacketService != null && mptcpPacketService.isCapturing()) {
                // ✅ Get ALL packets (not just recent)
                List<SensorPacket> allPackets = mptcpPacketService.getAllPackets();
                
                if (allPackets.isEmpty()) return;
                
                // ✅ Only send NEW packets (those we haven't sent yet)
                List<SensorPacket> newPackets = new ArrayList<>();
                for (SensorPacket packet : allPackets) {
                    if (packet.getSequenceNumber() > lastSentSequence) {
                        newPackets.add(packet);
                    }
                }
                
                if (!newPackets.isEmpty()) {
                    // Update last sent sequence
                    lastSentSequence = newPackets.get(newPackets.size() - 1).getSequenceNumber();
                    
                    // ✅ Calculate statistics
                    long totalPackets = allPackets.size();
                    long lostPackets = allPackets.stream().filter(SensorPacket::isLost).count();
                    double lossRate = totalPackets > 0 ? (lostPackets * 100.0 / totalPackets) : 0.0;
                    
                    // ✅ Build payload with ONLY new packets
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("packets", newPackets);      // Only NEW packets
                    payload.put("total", totalPackets);      // Total count
                    payload.put("lost", lostPackets);        // Lost count
                    payload.put("lossRate", lossRate);       // Loss percentage
                    
                    try {
                        String json = objectMapper.writeValueAsString(payload);
                        broadcastToAll(json);
                        
                        System.out.printf("📡 Sent %d new packets to UI | Total: %d | Lost: %d | Loss: %.2f%%%n",
                                newPackets.size(), totalPackets, lostPackets, lossRate);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);  // ✅ Check every 100ms for new packets
    }

    private void broadcastToAll(String json) {
        synchronized (sessions) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        System.err.println("⚠️ Failed to send WebSocket message to " + session.getId());
                    }
                }
            }
        }
    }

    /**
     * Reset when user clicks Reset button
     */
    public void resetCounters() {
        lastSentSequence = 0;
        System.out.println("🔄 Reset WebSocket counters");
    }
}