package com.example.demo.model;

public class TransferData {
    private int time;
    private int total;
    private int m1;
    private int m2;
    private double latencyM1;
    private double latencyM2;
    private double throughputM1;
    private double throughputM2;

    public TransferData(int time, int total, int m1, int m2,
                         double latencyM1, double latencyM2,
                         double throughputM1, double throughputM2) {
        this.time = time;
        this.total = total;
        this.m1 = m1;
        this.m2 = m2;
        this.latencyM1 = latencyM1;
        this.latencyM2 = latencyM2;
        this.throughputM1 = throughputM1;
        this.throughputM2 = throughputM2;
    }

    public int getTime() { return time; }
    public int getTotal() { return total; }
    public int getM1() { return m1; }
    public int getM2() { return m2; }
    public double getLatencyM1() { return latencyM1; }
    public double getLatencyM2() { return latencyM2; }
    public double getThroughputM1() { return throughputM1; }
    public double getThroughputM2() { return throughputM2; }
}