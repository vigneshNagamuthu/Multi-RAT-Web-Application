package com.example.demo.controller;

import com.example.demo.model.TransferData;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    @GetMapping("/analysis")
    public List<TransferData> getAnalysis(@RequestParam String type) {
        System.out.println("🔥 AnalysisController HIT with type = " + type);

        List<TransferData> data = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int m1 = 10 + i;
            int m2 = 15 + i;
            int total = m1 + m2;

            double latencyM1 = 100 - i * 2.5;
            double latencyM2 = 95 - i * 2.0;
            double throughputM1 = 5 + i * 0.5;
            double throughputM2 = 4.5 + i * 0.6;

            TransferData entry = new TransferData(i, total, m1, m2, latencyM1, latencyM2, throughputM1, throughputM2);
            data.add(entry);
        }

        return data;
    }
}
