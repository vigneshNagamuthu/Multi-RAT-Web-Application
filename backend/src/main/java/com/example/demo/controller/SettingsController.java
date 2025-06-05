package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:3006") // allow frontend to call
public class SettingsController {

    private String if1 = "192.168.1.1";
    private String if2 = "192.168.2.1";
    private String server = "47.129.143.46";

    @PostMapping("/{target}")
    public void updateIp(@PathVariable String target, @RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        switch (target.toLowerCase()) {
            case "if1" -> if1 = ip;
            case "if2" -> if2 = ip;
            case "server" -> server = ip;
        }
        System.out.println("✅ Updated " + target + " to " + ip);
    }

    @GetMapping
    public Map<String, String> getAllIps() {
        return Map.of("if1", if1, "if2", if2, "server", server);
    }
}
