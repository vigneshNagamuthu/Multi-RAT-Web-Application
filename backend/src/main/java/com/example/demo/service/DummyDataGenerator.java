package com.example.demo.service;

import com.example.demo.model.SensorPacket;
import com.example.demo.model.StreamMetrics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DummyDataGenerator {
    private final Random random = new Random();
    private int sensorSequence = 0;

    public StreamMetrics generateStreamMetrics() {
        long timestamp = System.currentTimeMillis();
        double latency = 50 + random.nextDouble() * 50;
        double packetLoss = random.nextDouble() * 5;
        int fps = 25 + random.nextInt(10);
        
        return new StreamMetrics(timestamp, latency, packetLoss, fps, "LRTT", 6060);
    }

    public List<SensorPacket> generateSensorPackets(int count) {
        List<SensorPacket> packets = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            sensorSequence++;
            
            // Simulate 10% packet loss
            boolean isLost = random.nextDouble() < 0.1;
            long timestamp = System.currentTimeMillis();
            
            packets.add(new SensorPacket(sensorSequence, isLost, timestamp, "Redundant", 5000));
        }
        
        return packets;
    }

    public void resetSequence() {
        sensorSequence = 0;
    }

    public int getCurrentSequence() {
        return sensorSequence;
    }
}