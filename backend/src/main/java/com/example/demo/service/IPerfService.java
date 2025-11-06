package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class IPerfService {
    
    private static final String AWS_SERVER = "13.212.221.200";  // Updated to new server!
    private static final int SENSOR_PORT = 5000;
    
    private Process iperfProcess;
    private boolean isRunning = false;
    private Thread outputThread;

    /**
     * Start iperf3 traffic to AWS server
     */
    public synchronized boolean startIPerf(int duration) {
        if (isRunning) {
            System.out.println("⚠️ iperf3 is already running");
            return false;
        }

        try {
            // Detect OS for proper command
            String os = System.getProperty("os.name").toLowerCase();
            
            System.out.println("🚀 Starting iperf3 to AWS: " + AWS_SERVER + ":" + SENSOR_PORT);
            System.out.println("🖥️  Detected OS: " + os);
            
            ProcessBuilder pb;
            
            if (os.contains("win")) {
                // Windows
                pb = new ProcessBuilder(
                    "cmd", "/c",
                    String.format("iperf3 -c %s -p %d -t %d -i 1 -b 10M", 
                        AWS_SERVER, SENSOR_PORT, duration)
                );
            } else {
                // Mac/Linux
                pb = new ProcessBuilder(
                    "iperf3",
                    "-c", AWS_SERVER,
                    "-p", String.valueOf(SENSOR_PORT),
                    "-t", String.valueOf(duration),
                    "-i", "1",
                    "-b", "10M"
                );
            }
            
            pb.redirectErrorStream(true);
            
            iperfProcess = pb.start();
            isRunning = true;
            
            // Start thread to read output
            outputThread = new Thread(() -> readIPerfOutput());
            outputThread.start();
            
            System.out.println("✅ iperf3 started successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start iperf3: " + e.getMessage());
            e.printStackTrace();
            isRunning = false;
            return false;
        }
    }

    /**
     * Stop iperf3 traffic
     */
    public synchronized void stopIPerf() {
        if (iperfProcess != null && iperfProcess.isAlive()) {
            iperfProcess.destroy();
            System.out.println("🛑 Stopped iperf3 to AWS");
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
                // Log iperf3 output
                System.out.println("[iperf3] " + line);
            }
            
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("Error reading iperf3 output: " + e.getMessage());
            }
        } finally {
            isRunning = false;
            System.out.println("✅ iperf3 test completed");
        }
    }

    public boolean isRunning() {
        return isRunning && iperfProcess != null && iperfProcess.isAlive();
    }
    
    public String getServerIp() {
        return AWS_SERVER;
    }
    
    public int getPort() {
        return SENSOR_PORT;
    }
}