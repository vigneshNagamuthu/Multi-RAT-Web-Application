// TransferData.java
package com.example.demo.model;

public class TransferData {
    private int time;
    private int total;
    private int m1;
    private int m2;

    public TransferData(int time, int total, int m1, int m2) {
        this.time = time;
        this.total = total;
        this.m1 = m1;
        this.m2 = m2;
    }

    public int getTime() { return time; }
    public int getTotal() { return total; }
    public int getM1() { return m1; }
    public int getM2() { return m2; }
}
