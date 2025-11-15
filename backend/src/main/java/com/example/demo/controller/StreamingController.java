package com.example.demo.controller;

import com.example.demo.service.VideoStreamingService;
import com.example.demo.service.VideoStreamingService.VideoDeviceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/streaming")
@CrossOrigin(origins = "*")
public class StreamingController {

    @Autowired
    private VideoStreamingService videoStreamingService;

    // Request body for /start
    public static class StartStreamRequest {
        private String devicePath;

        public String getDevicePath() {
            return devicePath;
        }

        public void setDevicePath(String devicePath) {
            this.devicePath = devicePath;
        }
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startStream(
            @RequestBody(required = false) StartStreamRequest request) {

        String preferredDevice = (request != null ? request.getDevicePath() : null);

        Map<String, Object> response = new HashMap<>();

        boolean started = videoStreamingService.startStreaming(preferredDevice);

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

    @GetMapping("/devices")
    public ResponseEntity<List<VideoDeviceInfo>> getDevices() {
        List<VideoDeviceInfo> devices = videoStreamingService.listVideoDevices();
        return ResponseEntity.ok(devices);
    }
}

