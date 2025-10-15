package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class IPerfService {
    
    private static final String AWS_SERVER = "54.169.114.206";
    private static final int SENSOR_PORT = 5000;
    
    private Process iperfProcess;
    private boolean isRunning = false;
    private Thread outputThread;

    public synchronized boolean startIPerf(int duration) {
        if (isRunning) {
            System.out.println("⚠️ iperf3 is already running");
            return false;
        }

        try {
            String command = String.format(
                "iperf3 -c %s -p %d -t %d -i 1 -b 10M",
                AWS_SERVER, SENSOR_PORT, duration
            );
            
            System.out.println("🚀 Starting iperf3 to AWS: " + AWS_SERVER + ":" + SENSOR_PORT);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            
            iperfProcess = pb.start();
            isRunning = true;
            
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