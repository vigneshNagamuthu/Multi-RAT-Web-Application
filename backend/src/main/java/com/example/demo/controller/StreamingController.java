package com.example.demo.controller;

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
    
    @Autowired
    private VideoStreamingService videoStreamingService;

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
        
        // Include real-time metrics if streaming
        if (videoStreamingService.isStreaming()) {
            response.put("currentFps", videoStreamingService.getCurrentFps());
            response.put("currentBitrate", videoStreamingService.getCurrentBitrate());
            response.put("droppedFrames", videoStreamingService.getDroppedFrames());
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getCurrentMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        if (videoStreamingService.isStreaming()) {
            metrics.put("frameRate", (int) videoStreamingService.getCurrentFps());
            metrics.put("bitrate", videoStreamingService.getCurrentBitrate());
            metrics.put("droppedFrames", videoStreamingService.getDroppedFrames());
            metrics.put("scheduler", "LRTT");
            metrics.put("port", videoStreamingService.getPort());
            metrics.put("status", "streaming");
        } else {
            metrics.put("frameRate", 0);
            metrics.put("bitrate", 0);
            metrics.put("droppedFrames", 0);
            metrics.put("scheduler", "LRTT");
            metrics.put("port", videoStreamingService.getPort());
            metrics.put("status", "stopped");
        }
        
        return ResponseEntity.ok(metrics);
    }
}