package com.example.demo.controller;

import com.example.demo.service.IPerfService;
import com.example.demo.service.MPTCPPacketService;
import com.example.demo.websocket.SensorWebSocketHandler;
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
    private IPerfService iperfService;

    @Autowired(required = false)
    private MPTCPPacketService mptcpPacketService;
    
    @Autowired(required = false)
    private SensorWebSocketHandler webSocketHandler;

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetSequence() {
        // ✅ Reset MPTCP service
        if (mptcpPacketService != null) {
            mptcpPacketService.resetSequence();
        }
        
        // ✅ Reset WebSocket handler
        if (webSocketHandler != null) {
            webSocketHandler.resetCounters();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Sequence reset to 0");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("scheduler", "Redundant");
        response.put("port", iperfService.getPort());
        response.put("server", iperfService.getServerIp());
        response.put("active", true);
        response.put("iperfRunning", iperfService.isRunning());
        
        if (mptcpPacketService != null) {
            response.put("mptcpCapturing", mptcpPacketService.isCapturing());
            response.put("currentSequence", mptcpPacketService.getCurrentSequence());
            response.put("packetCount", mptcpPacketService.getPacketCount());
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/iperf/start")
    public ResponseEntity<Map<String, Object>> startIPerf(
            @RequestParam(defaultValue = "60") int duration) {
        
        Map<String, Object> response = new HashMap<>();
        
        boolean started = iperfService.startIPerf(duration);
        
        if (started) {
            // ✅ Auto-start MPTCP packet capture
            if (mptcpPacketService != null) {
                mptcpPacketService.startCapture();
                System.out.println("✅ Auto-started MPTCP packet capture (max 100 packets)");
            }
            
            response.put("status", "success");
            response.put("message", "iperf3 traffic started - will capture 100 packets");
            response.put("server", iperfService.getServerIp());
            response.put("port", iperfService.getPort());
            response.put("duration", duration);
            response.put("scheduler", "Redundant");
            response.put("maxPackets", 100);
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Failed to start iperf3. Make sure iperf3 is installed.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/iperf/stop")
    public ResponseEntity<Map<String, String>> stopIPerf() {
        iperfService.stopIPerf();
        
        // ✅ Stop MPTCP capture
        if (mptcpPacketService != null) {
            mptcpPacketService.stopCapture();
            System.out.println("🛑 Auto-stopped MPTCP packet capture");
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "iperf3 traffic stopped and packet capture disabled");
        
        return ResponseEntity.ok(response);
    }
}