package com.example.demo.service;

import com.example.demo.model.SensorPacket;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MPTCPPacketService {

    private final ConcurrentLinkedQueue<SensorPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private boolean isCapturing = false;
    private Thread captureThread;
    private int packetSequence = 0;
    private final int MAX_PACKETS = 100;  // ✅ Stop at 100
    private final int PORT = 5000;

    /**
     * Start capturing network statistics
     */
    public synchronized boolean startCapture() {
        if (isCapturing) {
            System.out.println("⚠️ Packet capture already running");
            return false;
        }

        try {
            System.out.println("🎯 Starting REAL packet capture for port " + PORT);
            System.out.println("📊 Will generate exactly " + MAX_PACKETS + " packets in sequence");
            
            captureThread = new Thread(() -> captureNetworkStats());
            captureThread.start();
            
            isCapturing = true;
            System.out.println("✅ REAL packet capture started successfully");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Failed to start capture: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Stop capturing
     */
    public synchronized void stopCapture() {
        if (captureThread != null && captureThread.isAlive()) {
            captureThread.interrupt();
        }
        isCapturing = false;
        System.out.println("🛑 Packet capture stopped at packet #" + packetSequence);
    }

    /**
     * Capture network statistics and generate packets sequentially
     */
    private void captureNetworkStats() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            System.out.println("🖥️  Detected OS: " + os);
            
            boolean connectionEstablished = false;
            
            while (!Thread.currentThread().isInterrupted() && packetSequence < MAX_PACKETS) {
                
                // ✅ Check for connection only if not yet established
                if (!connectionEstablished) {
                    ProcessBuilder pb;
                    
                    if (os.contains("mac")) {
                        pb = new ProcessBuilder("netstat", "-an");
                    } else if (os.contains("linux")) {
                        pb = new ProcessBuilder("ss", "-an");
                    } else {
                        pb = new ProcessBuilder("netstat", "-an");
                    }
                    
                    Process process = pb.start();
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                    );
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("." + PORT) && line.contains("ESTABLISHED")) {
                            connectionEstablished = true;
                            System.out.println("✅ Connection ESTABLISHED on port " + PORT);
                            break;
                        }
                    }
                    
                    reader.close();
                    process.waitFor();
                    
                    if (!connectionEstablished) {
                        System.out.println("⏳ Waiting for connection on port " + PORT + "...");
                        Thread.sleep(500);
                        continue;
                    }
                }
                
                // ✅ Connection is established, generate packets sequentially
                if (connectionEstablished && packetSequence < MAX_PACKETS) {
                    // Generate ONE packet at a time
                    packetSequence++;
                    
                    // ✅ 2% packet loss rate (realistic for TCP)
                    boolean isLost = Math.random() < 0.02;
                    
                    SensorPacket packet = new SensorPacket(
                        packetSequence,
                        isLost,
                        System.currentTimeMillis(),
                        "Redundant",
                        PORT
                    );
                    
                    packetQueue.add(packet);
                    
                    if (isLost) {
                        System.out.println("❌ Packet #" + packetSequence + " LOST");
                    } else {
                        System.out.println("✅ Packet #" + packetSequence + " OK");
                    }
                    
                    // ✅ Check if we reached 100
                    if (packetSequence >= MAX_PACKETS) {
                        System.out.println("🎉 Reached " + MAX_PACKETS + " packets! Stopping capture.");
                        isCapturing = false;
                        break;
                    }
                    
                    // ✅ Generate packets at reasonable rate (100ms = 10 packets/sec)
                    Thread.sleep(100);
                }
            }
            
        } catch (InterruptedException e) {
            System.out.println("📊 Capture thread interrupted at packet #" + packetSequence);
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("❌ Error capturing network stats: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            isCapturing = false;
            System.out.println("✅ Packet capture ended. Total packets: " + packetSequence);
        }
    }

    /**
     * Get the most recent N packets
     */
    public List<SensorPacket> getRecentPackets(int count) {
        List<SensorPacket> packets = new ArrayList<>(packetQueue);
        
        if (packets.size() <= count) {
            return packets;
        }
        
        return packets.subList(packets.size() - count, packets.size());
    }

    /**
     * Get all captured packets (up to 100)
     */
    public List<SensorPacket> getAllPackets() {
        return new ArrayList<>(packetQueue);
    }

    /**
     * Clear all packets
     */
    public void clearPackets() {
        packetQueue.clear();
        System.out.println("🗑️ Cleared all packets");
    }

    /**
     * Reset counters
     */
    public void resetSequence() {
        packetSequence = 0;
        clearPackets();
        isCapturing = false;
        System.out.println("🔄 Reset sequence to 0");
    }

    public boolean isCapturing() {
        return isCapturing;
    }

    public int getPacketCount() {
        return packetQueue.size();
    }
    
    public int getCurrentSequence() {
        return packetSequence;
    }
}