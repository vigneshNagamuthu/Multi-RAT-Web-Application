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

    // Recent buffer (last 100 packets) for rolling statistics
    private final List<SensorPacket> packetBuffer = new ArrayList<>();

    // ✅ Running totals (lifetime)
    private int totalPacketsReceived = 0;
    private long totalLostPackets = 0;

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
                List<SensorPacket> recentPackets = mptcpPacketService.getRecentPackets(5);

                if (!recentPackets.isEmpty()) {
                    synchronized (packetBuffer) {
                        // Add new packets to history
                        packetBuffer.addAll(recentPackets);
                        totalPacketsReceived += recentPackets.size();

                        // ✅ Count new lost packets (add to lifetime total)
                        long newlyLost = recentPackets.stream()
                                .filter(SensorPacket::isLost)
                                .count();
                        totalLostPackets += newlyLost;

                        // Trim buffer to last 100 packets
                        if (packetBuffer.size() > 100) {
                            packetBuffer.subList(0, packetBuffer.size() - 100).clear();
                        }

                        // ---- Calculate rolling (recent) loss rate ----
                        int visiblePackets = packetBuffer.size();
                        long recentLostCount = packetBuffer.stream()
                                .filter(SensorPacket::isLost)
                                .count();
                        double recentLossRate = visiblePackets > 0
                                ? (recentLostCount * 100.0 / visiblePackets)
                                : 0.0;

                        // ---- Calculate lifetime loss rate ----
                        double lifetimeLossRate = totalPacketsReceived > 0
                                ? (totalLostPackets * 100.0 / totalPacketsReceived)
                                : 0.0;

                        // ---- Build JSON payload ----
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("packets", recentPackets);      // only latest packets
                        payload.put("total", totalPacketsReceived);  // lifetime total
                        payload.put("lost", totalLostPackets);       // lifetime lost
                        payload.put("lossRate", lifetimeLossRate);   // lifetime loss %
                        payload.put("recentLossRate", recentLossRate); // rolling 100 window

                        // ---- Send JSON to frontend ----
                        try {
                            String json = objectMapper.writeValueAsString(payload);
                            broadcastToAll(json);

                            // Console log for debugging
                            System.out.printf("📊 Sent to UI | Total: %d | Lost: %d | Lifetime Loss: %.2f%% | Recent: %.2f%%%n",
                                    totalPacketsReceived, totalLostPackets, lifetimeLossRate, recentLossRate);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
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

    // Reset everything when /api/sensor/reset is called
    public void clearPackets() {
        synchronized (packetBuffer) {
            packetBuffer.clear();
        }
        totalPacketsReceived = 0;
        totalLostPackets = 0;
        System.out.println("🧹 Cleared packet buffer and reset all counters.");
    }
}
