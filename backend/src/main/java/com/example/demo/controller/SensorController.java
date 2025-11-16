package com.example.demo.controller;

import com.example.demo.service.TcpPacketSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sensor")
@CrossOrigin(origins = "*")
public class SensorController {

    @Autowired
    private TcpPacketSenderService tcpPacketSenderService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        // Return TCP packet sender status
        return ResponseEntity.ok(tcpPacketSenderService.getStatus());
    }
    
    // TCP Packet Sender Endpoints
    
    @PostMapping("/tcp/start")
    public ResponseEntity<Map<String, Object>> startTcpSender(
            @RequestParam(defaultValue = "10") int packetsPerSecond) {
        
        Map<String, Object> response = new HashMap<>();
        
        tcpPacketSenderService.setPacketsPerSecond(packetsPerSecond);
        boolean started = tcpPacketSenderService.startSending();
        
        if (started) {
            response.put("status", "success");
            response.put("message", "TCP packet transmission started");
            response.putAll(tcpPacketSenderService.getStatus());
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Failed to start TCP transmission. Check AWS server connection.");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/tcp/stop")
    public ResponseEntity<Map<String, String>> stopTcpSender() {
        tcpPacketSenderService.stopSending();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "TCP packet transmission stopped");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/tcp/status")
    public ResponseEntity<Map<String, Object>> getTcpStatus() {
        return ResponseEntity.ok(tcpPacketSenderService.getStatus());
    }
    
    @PostMapping("/tcp/reset")
    public ResponseEntity<Map<String, String>> resetTcpSender() {
        tcpPacketSenderService.reset();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "TCP sender reset");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/tcp/reconfigure-mptcp")
    public ResponseEntity<Map<String, Object>> reconfigureMptcp() {
        Map<String, Object> result = tcpPacketSenderService.reconfigureMptcp();
        return ResponseEntity.ok(result);
    }
}