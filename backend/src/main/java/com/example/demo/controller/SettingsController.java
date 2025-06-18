package com.example.demo.controller;

import com.example.demo.model.ModemStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:3000")
public class SettingsController {

    // Store current IPs and names
    private String if1 = "192.168.1.1";
    private String if2 = "192.168.2.1";
    private String server = "47.129.143.46";
    private String modem1Name = "Modem 1";
    private String modem2Name = "Modem 2";

    // ====== IP UPDATES ======
    @PostMapping("/{target}")
    public ResponseEntity<String> updateIp(@PathVariable String target, @RequestBody Map<String, String> body) {
        String ip = body.get("ip");

        switch (target.toLowerCase()) {
            case "if1" -> if1 = ip;
            case "if2" -> if2 = ip;
            case "server" -> server = ip;
            default -> {
                return ResponseEntity.badRequest().body("Invalid target");
            }
        }

        System.out.println("✅ Updated " + target + " to " + ip);
        return ResponseEntity.ok(target + " IP updated");
    }

    // ====== GET ALL SETTINGS ======
    @GetMapping
    public Map<String, String> getAllSettings() {
        return Map.of(
            "if1", if1,
            "if2", if2,
            "server", server,
            "modem1Name", modem1Name,
            "modem2Name", modem2Name
        );
    }

    // ====== POWER TOGGLE ======
    @PostMapping("/modem1/power")
    public ResponseEntity<String> toggleModem1(@RequestBody ModemStatus status) {
        System.out.println("⚡ Modem 1 turned " + (status.isOn() ? "ON" : "OFF"));
        return ResponseEntity.ok("Modem 1 is now " + (status.isOn() ? "ON" : "OFF"));
    }

    @PostMapping("/modem2/power")
    public ResponseEntity<String> toggleModem2(@RequestBody ModemStatus status) {
        System.out.println("⚡ Modem 2 turned " + (status.isOn() ? "ON" : "OFF"));
        return ResponseEntity.ok("Modem 2 is now " + (status.isOn() ? "ON" : "OFF"));
    }

    // ====== NAME UPDATES ======
    @PostMapping("/modem1/name")
    public ResponseEntity<String> updateModem1Name(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        modem1Name = name;
        System.out.println("✏️ Modem 1 name updated to: " + name);
        return ResponseEntity.ok("Modem 1 name saved");
    }

    @PostMapping("/modem2/name")
    public ResponseEntity<String> updateModem2Name(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        modem2Name = name;
        System.out.println("✏️ Modem 2 name updated to: " + name);
        return ResponseEntity.ok("Modem 2 name saved");
    }
}
