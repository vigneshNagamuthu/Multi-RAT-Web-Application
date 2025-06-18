package com.example.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3006")
@RestController
public class NetworkInfoController {

    @GetMapping("/api/network-info")
    public Map<String, String> getNetworkInfo() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        Map<String, String> result = new HashMap<>();
                        result.put("interface", ni.getName());
                        result.put("ip", addr.getHostAddress());
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
        Map<String, String> fallback = new HashMap<>();
        fallback.put("error", "No valid IP found");
        return fallback;
    }
}
