package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SchedulerController {

    /**
     * Get available MPTCP schedulers from kernel
     */
    @GetMapping("/schedulers")
    public List<String> getAvailableSchedulers() {
        List<String> schedulers = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("cat /proc/sys/net/mptcp/available_schedulers");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("_")) continue;
                schedulers = Arrays.asList(line.trim().split("\\s+"));
                break;
            }
            reader.close();
        } catch (Exception e) {
            schedulers = List.of("default", "lrtt", "redundant", "roundrobin");
        }
        return schedulers;
    }

    /**
     * Get current active scheduler
     */
    @GetMapping("/scheduler/current")
    public ResponseEntity<Map<String, Object>> getCurrentScheduler() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Process process = Runtime.getRuntime().exec("cat /proc/sys/net/mptcp/scheduler");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String scheduler = reader.readLine();
            if (scheduler != null && !scheduler.trim().isEmpty()) {
                response.put("current", scheduler.trim());
            } else {
                response.put("current", "default");
            }
            
            reader.close();
        } catch (Exception e) {
            response.put("current", "default");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get hybrid scheduler configuration (port-based mapping)
     */
    @GetMapping("/scheduler/config")
    public ResponseEntity<Map<String, Object>> getSchedulerConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // Port-based scheduler configuration
        Map<String, String> portMapping = new HashMap<>();
        portMapping.put("6060", "LRTT");
        portMapping.put("5000", "Redundant");
        
        config.put("portMapping", portMapping);
        config.put("hybridMode", true);
        config.put("streamingPort", 6060);
        config.put("sensorPort", 5000);
        config.put("description", "Hybrid scheduler: Port 6060 uses LRTT, Port 5000 uses Redundant");
        
        return ResponseEntity.ok(config);
    }
}