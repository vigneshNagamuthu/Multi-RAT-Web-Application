package com.example.demo.service;

import com.example.demo.model.IpSettings;
import com.example.demo.model.IpSettings.Modem;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class IpSettingsService {
    public IpSettings getSettings() {
        IpSettings ipSettings = new IpSettings();
        Map<String, Boolean> powerStates = getPowerStates();
        try {
            Process process = Runtime.getRuntime().exec("ifconfig -a");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String currentInterface = null;
            String currentIp = null;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(" ") && line.contains(":")) {
                    if (currentInterface != null && !"lo".equals(currentInterface)) {
                        boolean power = powerStates.getOrDefault(currentInterface, true);
                        ipSettings.getModems().add(new Modem(currentInterface, currentInterface, currentIp != null ? currentIp : "-", power, currentInterface));
                    }
                    currentInterface = line.split(":")[0].trim();
                    currentIp = null;
                }
                if (line.trim().startsWith("inet ")) {
                    String[] parts = line.trim().split(" ");
                    for (int i = 0; i < parts.length; i++) {
                        if ("inet".equals(parts[i]) && i + 1 < parts.length) {
                            String ip = parts[i + 1];
                            if (!"127.0.0.1".equals(ip)) {
                                currentIp = ip;
                            }
                        }
                    }
                }
            }
            // Add the last interface
            if (currentInterface != null && !"lo".equals(currentInterface)) {
                boolean power = powerStates.getOrDefault(currentInterface, true);
                ipSettings.getModems().add(new Modem(currentInterface, currentInterface, currentIp != null ? currentIp : "-", power, currentInterface));
            }
        } catch (Exception e) {
            // ignore, return what we have
        }
        return ipSettings;
    }

    // Store power state in memory (could be improved with persistence)
    private static final Map<String, Boolean> interfacePowerState = new HashMap<>();

    private Map<String, Boolean> getPowerStates() {
        return interfacePowerState;
    }

    public Optional<Modem> getModemById(String id) {
        return getSettings().getModems().stream()
                .filter(m -> m.getId().equalsIgnoreCase(id))
                .findFirst();
    }

    public boolean updateModemPower(String id, boolean on) {
        interfacePowerState.put(id, on);
        return true;
    }

    public boolean updateModemIp(String id, String ip) {
        // Not supported in dynamic mode
        return false;
    }

    public boolean updateModemName(String id, String name) {
        // Not supported in dynamic mode
        return false;
    }

    public boolean removeModemById(String id) {
        // Not supported in dynamic mode
        return false;
    }
}
