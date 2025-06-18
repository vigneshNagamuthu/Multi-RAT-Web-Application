package com.example.demo.service;

import com.example.demo.model.TransferData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisService {

    public List<TransferData> generateFakeData(String type) {
        List<TransferData> list = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int m1, m2;
            double latencyM1, latencyM2, throughputM1, throughputM2;

            if ("Round Robin".equalsIgnoreCase(type)) {
                m1 = 10 + i;
                m2 = 15 + i;
                latencyM1 = 80 - i * 1.5;
                latencyM2 = 85 - i * 1.8;
                throughputM1 = 5 + i * 0.4;
                throughputM2 = 4.5 + i * 0.3;
            } else if ("RTT".equalsIgnoreCase(type)) {
                m1 = 12 + i * 2;
                m2 = 10 + i;
                latencyM1 = 95 - i * 1.2;
                latencyM2 = 90 - i * 1.1;
                throughputM1 = 4.8 + i * 0.6;
                throughputM2 = 4.2 + i * 0.5;
            } else {
                // Default fallback if type is unrecognized
                m1 = 10 + i;
                m2 = 10 + i;
                latencyM1 = 100 - i * 2.0;
                latencyM2 = 100 - i * 2.0;
                throughputM1 = 5.0;
                throughputM2 = 5.0;
            }

            int total = m1 + m2;
            list.add(new TransferData(i, total, m1, m2, latencyM1, latencyM2, throughputM1, throughputM2));
        }

        return list;
    }
}
