package com.example.demo.controller;

import com.example.demo.model.IpSettings.Modem;
import com.example.demo.model.ModemStatus;
import com.example.demo.service.IpSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:3000")
public class SettingsController {

    @Autowired
    private IpSettingsService ipSettingsService;

    @GetMapping
    public java.util.List<Modem> getAllModems() {
        return ipSettingsService.getSettings().getModems();
    }

    @PostMapping("/{id}/power")
    public ResponseEntity<Map<String, String>> togglePower(@PathVariable String id, @RequestBody ModemStatus status) {
        Optional<Modem> modemOpt = ipSettingsService.getModemById(id);
        if (modemOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Modem not found"));
        }

        Modem modem = modemOpt.get();
        String iface = modem.getInterfaceName();
        if (iface == null || iface.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing interface name"));
        }

        try {
            String command = "sudo /sbin/ip link set " + iface + (status.isOn() ? " up" : " down");
            Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
            int exit = process.waitFor();

            if (exit != 0) {
                BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorMessage = err.lines().collect(Collectors.joining("\n"));
                return ResponseEntity.status(500).body(Map.of("error", "Interface toggle failed: " + errorMessage));
            }

            ipSettingsService.updateModemPower(id, status.isOn());
            System.out.println("[POWER] " + modem.getId() + " (" + iface + "): " + (status.isOn() ? "ON" : "OFF"));
            return ResponseEntity.ok(Map.of("message", "Interface toggled successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Exception: " + e.getMessage()));
        }
    }
}