package com.example.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class IspInfoController {

    @GetMapping("/api/isp-info")
    public Map<String, Object> getIspInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Method 1: Using ipinfo.io API
            URL url = new URL("http://ipinfo.io/json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse the JSON response manually (simple parsing)
            String jsonResponse = response.toString();
            result.put("raw_response", jsonResponse);
            
            // Extract key information
            result.put("ip", extractJsonValue(jsonResponse, "ip"));
            result.put("isp", extractJsonValue(jsonResponse, "org"));
            result.put("city", extractJsonValue(jsonResponse, "city"));
            result.put("region", extractJsonValue(jsonResponse, "region"));
            result.put("country", extractJsonValue(jsonResponse, "country"));
            result.put("timezone", extractJsonValue(jsonResponse, "timezone"));
            
        } catch (Exception e) {
            // Fallback: Try using command line approach
            try {
                result = getIspInfoViaCommand();
            } catch (Exception fallbackError) {
                result.put("error", "Failed to get ISP info: " + e.getMessage());
                result.put("fallback_error", fallbackError.getMessage());
            }
        }
        
        return result;
    }
    
    @GetMapping("/api/isp-info/{ip}")
    public Map<String, Object> getIspInfoForIp(@PathVariable String ip) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get ISP info for specific IP using ipinfo.io
            URL url = new URL("http://ipinfo.io/" + ip + "/json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String jsonResponse = response.toString();
            result.put("ip", ip);
            result.put("isp", extractJsonValue(jsonResponse, "org"));
            result.put("city", extractJsonValue(jsonResponse, "city"));
            result.put("region", extractJsonValue(jsonResponse, "region"));
            result.put("country", extractJsonValue(jsonResponse, "country"));
            
        } catch (Exception e) {
            result.put("error", "Failed to get ISP info for IP: " + ip);
        }
        
        return result;
    }

    @GetMapping("/api/isp-info/command")
    public Map<String, Object> getIspInfoViaCommand() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get public IP first
            Process ipProcess = Runtime.getRuntime().exec("curl -s ifconfig.me");
            BufferedReader ipReader = new BufferedReader(new InputStreamReader(ipProcess.getInputStream()));
            String publicIp = ipReader.readLine();
            ipReader.close();
            ipProcess.waitFor();
            
            if (publicIp != null && !publicIp.trim().isEmpty()) {
                result.put("ip", publicIp.trim());
                
                // Get ISP info using whois
                Process whoisProcess = Runtime.getRuntime().exec("whois " + publicIp.trim());
                BufferedReader whoisReader = new BufferedReader(new InputStreamReader(whoisProcess.getInputStream()));
                StringBuilder whoisOutput = new StringBuilder();
                String line;
                
                while ((line = whoisReader.readLine()) != null) {
                    whoisOutput.append(line).append("\n");
                    
                    // Extract relevant information
                    if (line.toLowerCase().contains("org:") || line.toLowerCase().contains("orgname:")) {
                        result.put("isp", line.split(":")[1].trim());
                    }
                    if (line.toLowerCase().contains("netname:")) {
                        result.put("network_name", line.split(":")[1].trim());
                    }
                    if (line.toLowerCase().contains("country:")) {
                        result.put("country", line.split(":")[1].trim());
                    }
                }
                
                whoisReader.close();
                whoisProcess.waitFor();
                result.put("whois_output", whoisOutput.toString());
            }
            
        } catch (Exception e) {
            result.put("error", "Command execution failed: " + e.getMessage());
        }
        
        return result;
    }
    
    // Simple JSON value extractor (basic implementation)
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return "N/A";
            
            startIndex += searchKey.length();
            
            // Skip whitespace and quotes
            while (startIndex < json.length() && (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '"')) {
                startIndex++;
            }
            
            int endIndex = startIndex;
            boolean inQuotes = json.charAt(startIndex - 1) == '"';
            
            if (inQuotes) {
                // Find closing quote
                while (endIndex < json.length() && json.charAt(endIndex) != '"') {
                    endIndex++;
                }
            } else {
                // Find comma or closing brace
                while (endIndex < json.length() && json.charAt(endIndex) != ',' && json.charAt(endIndex) != '}') {
                    endIndex++;
                }
            }
            
            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return "N/A";
        }
    }
}