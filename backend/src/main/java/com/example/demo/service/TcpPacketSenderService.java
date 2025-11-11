package com.example.demo.service;

import com.example.demo.model.SensorPacket;
import com.example.demo.websocket.SensorWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

@Service
public class TcpPacketSenderService {
    
    private final String AWS_SERVER = "13.212.221.200";
    private final int AWS_PORT = 5000;
    
    @Autowired
    private SensorWebSocketHandler webSocketHandler;
    
    private ScheduledExecutorService scheduler;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private boolean isRunning = false;
    private int sentPackets = 0;
    private int receivedPackets = 0;
    private List<Integer> receivedSequences = new CopyOnWriteArrayList<>();
    private Map<Integer, Long> sentTimestamps = new ConcurrentHashMap<>();
    private List<Long> latencies = new CopyOnWriteArrayList<>();
    private int delayedPackets = 0; // Packets with >1000ms latency
    
    // Packet sending rate (packets per second)
    private int packetsPerSecond = 10;
    
    public synchronized boolean startSending() {
        if (isRunning) {
            return false;
        }
        
        try {
            // Connect to AWS server
            System.out.println("📡 Connecting to AWS server: " + AWS_SERVER + ":" + AWS_PORT);
            socket = new Socket(AWS_SERVER, AWS_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            isRunning = true;
            sentPackets = 0;
            receivedPackets = 0;
            receivedSequences.clear();
            sentTimestamps.clear();
            latencies.clear();
            delayedPackets = 0;
            
            // Start packet sender thread
            scheduler = Executors.newScheduledThreadPool(2);
            
            // Send packets at specified rate
            long delay = 1000 / packetsPerSecond; // milliseconds between packets
            scheduler.scheduleAtFixedRate(this::sendPacket, 0, delay, TimeUnit.MILLISECONDS);
            
            // Start receiver thread
            scheduler.submit(this::receivePackets);
            
            System.out.println("✅ TCP packet transmission started");
            return true;
            
        } catch (IOException e) {
            System.err.println("❌ Failed to connect to AWS server: " + e.getMessage());
            isRunning = false;
            return false;
        }
    }
    
    public synchronized void stopSending() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
        
        System.out.println("🛑 TCP packet transmission stopped");
    }
    
    private void sendPacket() {
        if (!isRunning || out == null) {
            return;
        }
        
        try {
            sentPackets++;
            long timestamp = System.currentTimeMillis();
            sentTimestamps.put(sentPackets, timestamp);
            
            // Send packet as: SEQ:TIMESTAMP
            String packet = sentPackets + ":" + timestamp;
            out.println(packet);
            
            if (sentPackets % 100 == 0) {
                System.out.println("📤 Sent " + sentPackets + " packets");
            }
            
        } catch (Exception e) {
            System.err.println("Error sending packet: " + e.getMessage());
        }
    }
    
    private void receivePackets() {
        System.out.println("👂 Listening for packet acknowledgments...");
        
        try {
            String line;
            while (isRunning && (line = in.readLine()) != null) {
                try {
                    // Parse received packet: SEQ:TIMESTAMP
                    String[] parts = line.trim().split(":");
                    int seqNum = Integer.parseInt(parts[0]);
                    long sentTime = parts.length > 1 ? Long.parseLong(parts[1]) : System.currentTimeMillis();
                    
                    receivedSequences.add(seqNum);
                    receivedPackets++;
                    
                    // Calculate latency
                    long receivedTime = System.currentTimeMillis();
                    long latency = receivedTime - sentTime;
                    latencies.add(latency);
                    
                    // Track delayed packets (>1000ms)
                    if (latency > 1000) {
                        delayedPackets++;
                        System.out.println("⚠️ Delayed packet #" + seqNum + " - Latency: " + latency + "ms");
                    }
                    
                    if (receivedPackets % 100 == 0) {
                        long avgLatency = latencies.stream().mapToLong(Long::longValue).sum() / latencies.size();
                        System.out.println("📥 Received " + receivedPackets + " packets (Avg latency: " + avgLatency + "ms)");
                    }
                    
                    // Broadcast status update every 10 packets
                    if (receivedPackets % 10 == 0) {
                        Map<String, Object> status = getStatus();
                        webSocketHandler.broadcastStatus(status);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Invalid packet format: " + line);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("Error receiving packets: " + e.getMessage());
            }
        }
    }
    
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", isRunning);
        status.put("sentPackets", sentPackets);
        status.put("receivedPackets", receivedPackets);
        status.put("lostPackets", sentPackets - receivedPackets);
        status.put("packetLossRate", sentPackets > 0 ? 
            String.format("%.2f%%", (sentPackets - receivedPackets) * 100.0 / sentPackets) : "0.00%");
        
        // Calculate latency statistics
        long avgLatency = 0;
        long minLatency = 0;
        long maxLatency = 0;
        
        if (!latencies.isEmpty()) {
            avgLatency = latencies.stream().mapToLong(Long::longValue).sum() / latencies.size();
            minLatency = latencies.stream().mapToLong(Long::longValue).min().orElse(0);
            maxLatency = latencies.stream().mapToLong(Long::longValue).max().orElse(0);
        }
        
        status.put("avgLatency", avgLatency);
        status.put("minLatency", minLatency);
        status.put("maxLatency", maxLatency);
        status.put("delayedPackets", delayedPackets);
        status.put("server", AWS_SERVER);
        status.put("port", AWS_PORT);
        status.put("packetsPerSecond", packetsPerSecond);
        status.put("receivedSequences", new ArrayList<>(receivedSequences));
        
        return status;
    }
    
    public void reset() {
        if (isRunning) {
            stopSending();
        }
        sentPackets = 0;
        receivedPackets = 0;
        receivedSequences.clear();
        sentTimestamps.clear();
        latencies.clear();
        delayedPackets = 0;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void setPacketsPerSecond(int rate) {
        this.packetsPerSecond = Math.max(1, Math.min(100, rate)); // Limit 1-100 pps
    }
}
