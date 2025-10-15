package com.example.demo.controller;

import com.example.demo.model.SensorPacket;
import com.example.demo.service.DummyDataGenerator;
import com.example.demo.service.IPerfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensor")
@CrossOrigin(origins = "*")
public class SensorController {

    private final DummyDataGenerator dataGenerator;
    
    @Autowired
    private IPerfService iperfService;

    public SensorController(DummyDataGenerator dataGenerator) {
        this.dataGenerator = dataGenerator;
    }

    @GetMapping("/packets")
    public ResponseEntity<List<SensorPacket>> getPackets(
            @RequestParam(defaultValue = "10") int count) {
        List<SensorPacket> packets = dataGenerator.generateSensorPackets(count);
        return ResponseEntity.ok(packets);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetSequence() {
        dataGenerator.resetSequence();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Sequence reset to 0");
        response.put("currentSequence", dataGenerator.getCurrentSequence());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("scheduler", "Redundant");
        response.put("port", iperfService.getPort());
        response.put("server", iperfService.getServerIp());
        response.put("currentSequence", dataGenerator.getCurrentSequence());
        response.put("active", true);
        response.put("iperfRunning", iperfService.isRunning());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/iperf/start")
    public ResponseEntity<Map<String, Object>> startIPerf(
            @RequestParam(defaultValue = "60") int duration) {
        
        Map<String, Object> response = new HashMap<>();
        
        boolean started = iperfService.startIPerf(duration);
        
        if (started) {
            response.put("status", "success");
            response.put("message", "iperf3 traffic started to AWS server");
            response.put("server", iperfService.getServerIp());
            response.put("port", iperfService.getPort());
            response.put("duration", duration);
            response.put("scheduler", "Redundant");
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
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "iperf3 traffic stopped");
        
        return ResponseEntity.ok(response);
    }
}