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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;


@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class NetworkInfoController {

    @GetMapping("/api/network-info")
    public Map<String, Object> getNetworkInfo() {
        Map<String, Object> result = new HashMap<>();
        try {
            Process process = Runtime.getRuntime().exec("ip -j address show"); // JSON output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.lines().collect(Collectors.joining());
            
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> interfaces = mapper.readValue(output, List.class);

            for (Map<String, Object> iface : interfaces) {
                String ifName = (String) iface.get("ifname");
                List<Map<String, Object>> addrInfo = (List<Map<String, Object>>) iface.get("addr_info");

                for (Map<String, Object> addr : addrInfo) {
                    String family = (String) addr.get("family");
                    String local = (String) addr.get("local");

                    if ("inet".equals(family)) {
                        switch (ifName) {
                            case "ens33":
                                result.put("client", Map.of("interface", ifName, "ip", local));
                                break;
                            case "dummy0":
                                result.put("modem1", Map.of("interface", ifName, "ip", local));
                                break;
                            case "dummy1":
                                result.put("modem2", Map.of("interface", ifName, "ip", local));
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

}
