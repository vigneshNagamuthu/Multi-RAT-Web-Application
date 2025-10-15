package com.example.demo.model;

public class StreamMetrics {
    private long timestamp;
    private double latency;
    private double packetLoss;
    private int fps;
    private String scheduler;
    private int port;

    public StreamMetrics() {}

    public StreamMetrics(long timestamp, double latency, double packetLoss, int fps, String scheduler, int port) {
        this.timestamp = timestamp;
        this.latency = latency;
        this.packetLoss = packetLoss;
        this.fps = fps;
        this.scheduler = scheduler;
        this.port = port;
    }

    // Getters and Setters
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatency() {
        return latency;
    }

    public void setLatency(double latency) {
        this.latency = latency;
    }

    public double getPacketLoss() {
        return packetLoss;
    }

    public void setPacketLoss(double packetLoss) {
        this.packetLoss = packetLoss;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public String getScheduler() {
        return scheduler;
    }

    public void setScheduler(String scheduler) {
        this.scheduler = scheduler;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}