package com.example.demo.controller;

import com.example.demo.model.StreamMetrics;
import com.example.demo.service.DummyDataGenerator;
import com.example.demo.service.VideoStreamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/streaming")
@CrossOrigin(origins = "*")
public class StreamingController {

    private final DummyDataGenerator dataGenerator;
    
    @Autowired
    private VideoStreamingService videoStreamingService;

    public StreamingController(DummyDataGenerator dataGenerator) {
        this.dataGenerator = dataGenerator;
    }

    @GetMapping("/metrics")
    public ResponseEntity<StreamMetrics> getMetrics() {
        StreamMetrics metrics = dataGenerator.generateStreamMetrics();
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startStream() {
        Map<String, Object> response = new HashMap<>();
        
        boolean started = videoStreamingService.startStreaming();
        
        if (started) {
            response.put("status", "success");
            response.put("message", "Video streaming started to AWS server");
            response.put("server", videoStreamingService.getServerIp());
            response.put("port", videoStreamingService.getPort());
            response.put("scheduler", "LRTT");
        } else {
            response.put("status", "error");
            response.put("message", "Failed to start streaming. Check if FFmpeg is installed and camera is available.");
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stopStream() {
        videoStreamingService.stopStreaming();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Streaming stopped");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("isStreaming", videoStreamingService.isStreaming());
        response.put("server", videoStreamingService.getServerIp());
        response.put("port", videoStreamingService.getPort());
        response.put("scheduler", "LRTT");
        
        return ResponseEntity.ok(response);
    }
}