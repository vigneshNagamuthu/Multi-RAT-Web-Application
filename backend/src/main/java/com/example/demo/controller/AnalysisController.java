package com.example.demo.controller;

import com.example.demo.model.TransferData;
import com.example.demo.service.AnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/analysis")
    public List<TransferData> getAnalysis(@RequestParam String type) {
        System.out.println("🔥 AnalysisController HIT with type = " + type);
        return analysisService.generateFakeData(type);
    }

    @PostMapping("/analysis/upload")
    public @ResponseBody java.util.Map<String, Object> runIperfUpload(@RequestBody(required = false) Map<String, Object> body) throws Exception {
        if (body == null || body.get("ip") == null || body.get("ip").toString().isBlank()) {
            throw new org.springframework.http.converter.HttpMessageNotReadableException("No IP address provided in request body");
        }
        String ip = body.get("ip").toString();
        System.out.println("[IPERF] Upload test triggered for IP: " + ip);
        return runIperfCommand(false, ip);
    }

    @PostMapping("/analysis/download")
    public @ResponseBody java.util.Map<String, Object> runIperfDownload(@RequestBody(required = false) Map<String, Object> body) throws Exception {
        if (body == null || body.get("ip") == null || body.get("ip").toString().isBlank()) {
            throw new org.springframework.http.converter.HttpMessageNotReadableException("No IP address provided in request body");
        }
        String ip = body.get("ip").toString();
        System.out.println("[IPERF] Download test triggered for IP: " + ip);
        return runIperfCommand(true, ip);
    }

    private java.util.Map<String, Object> runIperfCommand(boolean reverse, String ip) throws Exception {
        java.util.List<String> command = new java.util.ArrayList<>(java.util.Arrays.asList("iperf3", "-c", ip, "-t", "10"));
        if (reverse) command.add("-R");
        System.out.println("[IPERF] Running command: " + String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        boolean finished = process.waitFor(12, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            System.out.println("[IPERF] Process timed out and was killed.");
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("error", "timeout");
            result.put("output", output.toString());
            result.put("exitCode", -1);
            return result;
        }
        while ((line = reader.readLine()) != null) {
            System.out.println("[IPERF] " + line);
            output.append(line).append("\n");
        }
        int exitCode = process.exitValue();
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("output", output.toString());
        result.put("exitCode", exitCode);
        return result;
    }
}
