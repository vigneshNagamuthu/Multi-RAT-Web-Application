package com.example.demo.model;

public class SensorPacket {
    private int sequenceNumber;
    private boolean isLost;
    private long timestamp;
    private String scheduler;
    private int port;

    public SensorPacket() {}

    public SensorPacket(int sequenceNumber, boolean isLost, long timestamp, String scheduler, int port) {
        this.sequenceNumber = sequenceNumber;
        this.isLost = isLost;
        this.timestamp = timestamp;
        this.scheduler = scheduler;
        this.port = port;
    }

    // Getters and Setters
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public boolean isLost() {
        return isLost;
    }

    public void setLost(boolean lost) {
        isLost = lost;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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