package com.example.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class NetworkInfoController {

    @GetMapping("/api/network-info")
    public Map<String, Object> getNetworkInfo() {
        Map<String, Object> result = new HashMap<>();
        try {
            Process process = Runtime.getRuntime().exec("ifconfig -a");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String currentInterface = null;
            String currentIp = null;
            String currentState = "DOWN";

            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(" ") && line.contains(":")) {
                    if (currentInterface != null && !"lo".equals(currentInterface)) {
                        Map<String, String> ifaceInfo = new HashMap<>();
                        ifaceInfo.put("interface", currentInterface);
                        ifaceInfo.put("ip", currentIp != null ? currentIp : "-");
                        ifaceInfo.put("state", currentState);
                        result.put(currentInterface, ifaceInfo);
                    }
                    currentInterface = line.split(":")[0].trim();
                    currentIp = null;
                    currentState = line.contains("UP") ? "UP" : "DOWN";
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
                Map<String, String> ifaceInfo = new HashMap<>();
                ifaceInfo.put("interface", currentInterface);
                ifaceInfo.put("ip", currentIp != null ? currentIp : "-");
                ifaceInfo.put("state", currentState);
                result.put(currentInterface, ifaceInfo);
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }
}