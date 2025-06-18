package com.example.demo.controller;

import com.example.demo.model.TransferData;
import com.example.demo.service.AnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/analysis")
    public List<TransferData> getAnalysis(@RequestParam String type) {
        System.out.println("🔥 AnalysisController HIT with type = " + type);
        return analysisService.generateFakeData(type);
    }
}
