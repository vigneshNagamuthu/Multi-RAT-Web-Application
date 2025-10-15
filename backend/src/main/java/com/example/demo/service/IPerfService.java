package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class IPerfService {
    
    private Process iperfProcess;
    private boolean isRunning = false;
    private Thread outputThread;

    /**
     * Start iperf3 client sending traffic to server
     */
    public synchronized boolean startIPerf(String serverIp, int port, int duration) {
        if (isRunning) {
            System.out.println("⚠️ iperf3 is already running");
            return false;
        }

        try {
            // iperf3 command: send traffic for specified duration
            String command = String.format(
                "iperf3 -c %s -p %d -t %d -i 1", 
                serverIp, port, duration
            );
            
            System.out.println("🚀 Starting iperf3: " + command);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            
            iperfProcess = pb.start();
            isRunning = true;
            
            // Read output in separate thread
            outputThread = new Thread(() -> readIPerfOutput());
            outputThread.start();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start iperf3: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Stop iperf3 traffic
     */
    public synchronized void stopIPerf() {
        if (iperfProcess != null && iperfProcess.isAlive()) {
            iperfProcess.destroy();
            System.out.println("🛑 Stopped iperf3");
        }
        
        if (outputThread != null && outputThread.isAlive()) {
            outputThread.interrupt();
        }
        
        isRunning = false;
    }

    /**
     * Read and log iperf3 output
     */
    private void readIPerfOutput() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(iperfProcess.getInputStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[iperf3] " + line);
            }
            
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("Error reading iperf3 output: " + e.getMessage());
            }
        } finally {
            isRunning = false;
            System.out.println("✅ iperf3 process completed");
        }
    }

    public boolean isRunning() {
        return isRunning && iperfProcess != null && iperfProcess.isAlive();
    }
}