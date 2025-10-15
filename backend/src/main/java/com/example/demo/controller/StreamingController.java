package com.example.demo.controller;

import com.example.demo.model.StreamMetrics;
import com.example.demo.service.DummyDataGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/streaming")
@CrossOrigin(origins = "*")
public class StreamingController {

    private final DummyDataGenerator dataGenerator;

    public StreamingController(DummyDataGenerator dataGenerator) {
        this.dataGenerator = dataGenerator;
    }

    @GetMapping("/metrics")
    public ResponseEntity<StreamMetrics> getMetrics() {
        StreamMetrics metrics = dataGenerator.generateStreamMetrics();
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startStream() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Streaming started on port 6060 (LRTT scheduler)");
        response.put("scheduler", "LRTT");
        response.put("port", "6060");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stopStream() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Streaming stopped");
        return ResponseEntity.ok(response);
    }
}