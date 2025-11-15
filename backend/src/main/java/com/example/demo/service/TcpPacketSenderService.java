package com.example.demo.service;

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
    private final int LOCAL_PROXY_PORT = 5001; // Local MPTCP proxy
    
    @Autowired
    private SensorWebSocketHandler webSocketHandler;
    
    private ScheduledExecutorService scheduler;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Process proxyProcess;
    
    private boolean isRunning = false;
    private boolean useMptcp = true; // Toggle for MPTCP vs TCP
    private int sentPackets = 0;
    private int receivedPackets = 0;
    private List<Integer> receivedSequences = new CopyOnWriteArrayList<>();
    private Map<Integer, Long> sentTimestamps = new ConcurrentHashMap<>();
    private List<Long> latencies = new CopyOnWriteArrayList<>();
    private int retransmittedPackets = 0; // Packets with >1000ms latency (likely retransmitted)
    
    // Packet sending rate (packets per second)
    private int packetsPerSecond = 10;
    
    public synchronized boolean startSending() {
        if (isRunning) {
            return false;
        }
        
        try {
            if (useMptcp) {
                // MPTCP mode - use socat proxy
                System.out.println("📡 Connecting to AWS server with MPTCP: " + AWS_SERVER + ":" + AWS_PORT);
                
                // Start local MPTCP proxy using socat
                startMptcpProxy();
                
                // Give proxy time to start
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Connect to local proxy which forwards to AWS with MPTCP
                socket = new Socket("127.0.0.1", LOCAL_PROXY_PORT);
            } else {
                // Standard TCP mode - direct connection
                System.out.println("📡 Connecting to AWS server with TCP: " + AWS_SERVER + ":" + AWS_PORT);
                socket = new Socket(AWS_SERVER, AWS_PORT);
            }
            
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            isRunning = true;
            sentPackets = 0;
            receivedPackets = 0;
            receivedSequences.clear();
            sentTimestamps.clear();
            latencies.clear();
            retransmittedPackets = 0;
            
            // Start packet sender thread
            scheduler = Executors.newScheduledThreadPool(2);
            
            // Send packets at specified rate
            long delay = 1000 / packetsPerSecond; // milliseconds between packets
            scheduler.scheduleAtFixedRate(this::sendPacket, 0, delay, TimeUnit.MILLISECONDS);
            
            // Start receiver thread
            scheduler.submit(this::receivePackets);
            
            System.out.println("✅ Packet transmission started (" + (useMptcp ? "MPTCP" : "TCP") + " mode)");
            return true;
            
        } catch (IOException e) {
            System.err.println("❌ Failed to connect to AWS server: " + e.getMessage());
            isRunning = false;
            
            // If MPTCP failed, stop the proxy
            if (useMptcp) {
                stopMptcpProxy();
            }
            
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
        
        // Stop MPTCP proxy if it was used
        if (useMptcp) {
            stopMptcpProxy();
        }
        
        System.out.println("🛑 Packet transmission stopped");
    }
    
    /**
     * Start local MPTCP proxy using socat
     * Forwards local connections to AWS with MPTCP
     * Uses sourceport=5000 for custom MPTCP scheduler routing
     */
    private void startMptcpProxy() {
        // Stop any existing proxy first
        stopMptcpProxy();
        
        // Kill any lingering socat processes on port 5001 and release port 5000
        try {
            ProcessBuilder killSocat = new ProcessBuilder("bash", "-c", 
                "pkill -f 'socat.*" + LOCAL_PROXY_PORT + "' || true");
            killSocat.start().waitFor(1, TimeUnit.SECONDS);
            
            // Also kill any processes using source port 5000
            ProcessBuilder killPort5000 = new ProcessBuilder("bash", "-c",
                "fuser -k 5000/tcp || true");
            killPort5000.start().waitFor(1, TimeUnit.SECONDS);
            
            Thread.sleep(500); // Give OS time to release the ports
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        
        try {
            // Use socat to create MPTCP proxy with port 5000 for scheduler routing
            // sourceport=5000 needed for custom MPTCP scheduler to identify sensor traffic
            // SO_REUSEADDR allows immediate rebinding after stop
            ProcessBuilder pb = new ProcessBuilder(
                "bash", "-c",
                "mptcpize run socat TCP-LISTEN:" + LOCAL_PROXY_PORT + 
                ",reuseaddr,fork TCP:" + AWS_SERVER + ":" + AWS_PORT + 
                ",sourceport=5000,reuseaddr"
            );
            
            pb.redirectErrorStream(true);
            proxyProcess = pb.start();
            
            System.out.println("✅ MPTCP proxy started on port " + LOCAL_PROXY_PORT);
            System.out.println("   Forwarding to " + AWS_SERVER + ":" + AWS_PORT + " with source port 5000");
            System.out.println("   🎯 Using port 5000 for custom MPTCP scheduler routing");
            
            // Monitor proxy output in background
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proxyProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("error") || line.contains("Error")) {
                            System.err.println("[MPTCP Proxy] " + line);
                        }
                    }
                } catch (IOException e) {
                    // Ignore - proxy stopped
                }
            }).start();
            
        } catch (IOException e) {
            System.err.println("❌ Failed to start MPTCP proxy: " + e.getMessage());
            System.err.println("💡 Make sure 'socat' and 'mptcpize' are installed");
        }
    }
    
    /**
     * Stop MPTCP proxy
     */
    private void stopMptcpProxy() {
        if (proxyProcess != null && proxyProcess.isAlive()) {
            proxyProcess.destroy();
            try {
                if (!proxyProcess.waitFor(2, TimeUnit.SECONDS)) {
                    proxyProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                proxyProcess.destroyForcibly();
            }
            System.out.println("🛑 MPTCP proxy stopped");
        }
        
        // Force kill any lingering socat processes
        try {
            ProcessBuilder killSocat = new ProcessBuilder("bash", "-c", 
                "pkill -f 'socat.*" + LOCAL_PROXY_PORT + "' || true");
            killSocat.start().waitFor(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
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
                    
                    // Track retransmitted packets (>1000ms)
                    if (latency > 1000) {
                        retransmittedPackets++;
                        System.out.println("⚠️ Retransmitted packet #" + seqNum + " - Latency: " + latency + "ms");
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
        status.put("useMptcp", useMptcp);
        status.put("sentPackets", sentPackets);
        status.put("receivedPackets", receivedPackets);
        status.put("inTransitPackets", sentPackets - receivedPackets);
        status.put("deliveryRate", sentPackets > 0 ? 
            String.format("%.2f%%", receivedPackets * 100.0 / sentPackets) : "-");
        
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
        status.put("retransmittedPackets", retransmittedPackets);
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
        retransmittedPackets = 0;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void setPacketsPerSecond(int rate) {
        this.packetsPerSecond = Math.max(1, Math.min(100, rate)); // Limit 1-100 pps
    }
    
    // Method to toggle MPTCP mode
    public void setUseMptcp(boolean useMptcp) {
        if (!isRunning) {
            this.useMptcp = useMptcp;
            System.out.println("📡 Protocol mode set to: " + (useMptcp ? "MPTCP" : "TCP"));
        } else {
            System.err.println("⚠️ Cannot change protocol while transmission is running");
        }
    }
    
    public boolean isUsingMptcp() {
        return useMptcp;
    }
}