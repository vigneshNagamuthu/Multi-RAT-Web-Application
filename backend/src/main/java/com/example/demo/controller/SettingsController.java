package com.example.demo.controller;

import com.example.demo.model.IpSettings.Modem;
import com.example.demo.model.ModemStatus;
import com.example.demo.service.IpSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:3000")
public class SettingsController {

    @Autowired
    private IpSettingsService ipSettingsService;

    // Get all modems
    @GetMapping
    public List<Modem> getAllModems() {
        return ipSettingsService.getSettings().getModems();
    }

    // Add a new modem
    @PostMapping("/modems")
    public ResponseEntity<String> addModem(@RequestBody Modem modem) {
        ipSettingsService.addModem(modem);
        System.out.println("➕ Added modem: " + modem.getName());
        return ResponseEntity.ok("Modem added");
    }

    // Delete a modem
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteModem(@PathVariable String id) {
        boolean removed = ipSettingsService.removeModemById(id);
        if (removed) {
            System.out.println("🗑️ Deleted modem: " + id);
            return ResponseEntity.ok("Modem deleted");
        } else {
            return ResponseEntity.badRequest().body("Modem not found");
        }
    }

    // Update modem IP
    @PostMapping("/{id}/ip")
    public ResponseEntity<String> updateIp(@PathVariable String id, @RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        boolean success = ipSettingsService.updateModemIp(id, ip);
        return success ? ResponseEntity.ok("IP updated") : ResponseEntity.badRequest().body("Modem not found");
    }

    // Update modem name
    @PostMapping("/{id}/name")
    public ResponseEntity<String> updateName(@PathVariable String id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        boolean success = ipSettingsService.updateModemName(id, name);
        return success ? ResponseEntity.ok("Name updated") : ResponseEntity.badRequest().body("Modem not found");
    }

    // Update modem power state
    @PostMapping("/{id}/power")
    public ResponseEntity<String> togglePower(@PathVariable String id, @RequestBody ModemStatus status) {
        boolean success = ipSettingsService.updateModemPower(id, status.isOn());
        return success
                ? ResponseEntity.ok("Power updated to " + (status.isOn() ? "ON" : "OFF"))
                : ResponseEntity.badRequest().body("Modem not found");
    }
}
