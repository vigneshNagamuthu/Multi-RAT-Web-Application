package com.example.demo.controller;

import com.example.demo.model.TransferData;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api")
public class AnalysisController {


    @PostMapping("/analysis/upload")
    public @ResponseBody java.util.Map<String, Object> runIperfUpload(@RequestBody(required = false) Map<String, Object> body) throws Exception {
        if (body == null || body.get("ip") == null || body.get("ip").toString().isBlank()) {
            throw new org.springframework.http.converter.HttpMessageNotReadableException("No IP address provided in request body");
        }
        String ip = body.get("ip").toString();
        String scheduler = body.get("scheduler") != null ? body.get("scheduler").toString() : "default";
        String protocol = body.get("protocol") != null ? body.get("protocol").toString() : "TCP";
        System.out.println("[IPERF] Upload test triggered for IP: " + ip + ", Scheduler: " + scheduler + ", Protocol: " + protocol);
        return runIperfCommand(false, ip, scheduler, protocol);
    }

    @PostMapping("/analysis/download")
    public @ResponseBody java.util.Map<String, Object> runIperfDownload(@RequestBody(required = false) Map<String, Object> body) throws Exception {
        if (body == null || body.get("ip") == null || body.get("ip").toString().isBlank()) {
            throw new org.springframework.http.converter.HttpMessageNotReadableException("No IP address provided in request body");
        }
        String ip = body.get("ip").toString();
        String scheduler = body.get("scheduler") != null ? body.get("scheduler").toString() : "default";
        String protocol = body.get("protocol") != null ? body.get("protocol").toString() : "TCP";
        System.out.println("[IPERF] Download test triggered for IP: " + ip + ", Scheduler: " + scheduler + ", Protocol: " + protocol);
        return runIperfCommand(true, ip, scheduler, protocol);
    }

    private java.util.Map<String, Object> runIperfCommand(boolean reverse, String ip, String scheduler, String protocol) throws Exception {
        // Set the scheduler first
        java.util.List<String> setSchedulerCmd = java.util.Arrays.asList("sudo", "sysctl", "-w", "net.mptcp.scheduler=" + scheduler);
        System.out.println("[MPTCP] Setting scheduler: " + String.join(" ", setSchedulerCmd));
        ProcessBuilder setSchedulerPb = new ProcessBuilder(setSchedulerCmd);
        setSchedulerPb.redirectErrorStream(true);
        Process setSchedulerProcess = setSchedulerPb.start();
        setSchedulerProcess.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);

        // Prepare to sample /proc/net/dev
        java.util.List<String> interfaces = getActiveInterfaces();
        int sampleIntervalMs = 1000;
        int testDurationSec = 10;
        java.util.List<java.util.Map<String, Object>> samples = new java.util.ArrayList<>();
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<?> samplingTask = executor.submit(() -> {
            try {
                java.util.Map<String, Long> prevRx = new java.util.HashMap<>();
                java.util.Map<String, Long> prevTx = new java.util.HashMap<>();
                for (String iface : interfaces) {
                    long[] rxTx = getInterfaceBytes(iface);
                    prevRx.put(iface, rxTx[0]);
                    prevTx.put(iface, rxTx[1]);
                }
                for (int t = 0; t < testDurationSec; t++) {
                    Thread.sleep(sampleIntervalMs);
                    java.util.Map<String, Object> sample = new java.util.HashMap<>();
                    sample.put("second", t+1);
                    double total = 0;
                    StringBuilder logLine = new StringBuilder();
                    logLine.append("[THROUGHPUT] Second ").append(t+1).append(": ");
                    for (String iface : interfaces) {
                        long[] rxTx = getInterfaceBytes(iface);
                        long rxDiff = rxTx[0] - prevRx.get(iface);
                        long txDiff = rxTx[1] - prevTx.get(iface);
                        prevRx.put(iface, rxTx[0]);
                        prevTx.put(iface, rxTx[1]);
                        // Throughput in Mbps (using RX+TX)
                        double mbps = (rxDiff + txDiff) * 8.0 / 1_000_000.0;
                        sample.put(iface, mbps);
                        total += mbps;
                        logLine.append(iface).append("=").append(String.format("%.2f", mbps)).append(" Mbps; ");
                    }
                    sample.put("total", total);
                    logLine.append("total=").append(String.format("%.2f", total)).append(" Mbps");
                    System.out.println(logLine.toString());
                    samples.add(sample);
                }
            } catch (Exception e) {
                // ignore
            }
        });

        // Build iperf3 command based on protocol
        java.util.List<String> command;
        if ("MPTCP".equalsIgnoreCase(protocol)) {
            command = new java.util.ArrayList<>(java.util.Arrays.asList("mptcpize", "run", "iperf3", "-c", ip, "-t", String.valueOf(testDurationSec)));
        } else {
            command = new java.util.ArrayList<>(java.util.Arrays.asList("iperf3", "-c", ip, "-t", String.valueOf(testDurationSec)));
        }
        if (reverse) command.add("-R");
        System.out.println("[IPERF] Running command: " + String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        boolean finished = process.waitFor(testDurationSec + 2, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            System.out.println("[IPERF] Process timed out and was killed.");
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("error", "timeout");
            result.put("output", output.toString());
            result.put("exitCode", -1);
            result.put("samples", samples);
            result.put("interfaces", interfaces);
            return result;
        }
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        samplingTask.get(); // Wait for sampling to finish
        executor.shutdown();
        int exitCode = process.exitValue();
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("output", output.toString());
        result.put("exitCode", exitCode);
        result.put("samples", samples);
        result.put("interfaces", interfaces);
        return result;
    }

    // Helper to get RX and TX bytes for an interface from /proc/net/dev
    private long[] getInterfaceBytes(String iface) {
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("/proc/net/dev"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(iface + ":")) {
                    String[] parts = line.split(":")[1].trim().split("\\s+");
                    long rx = Long.parseLong(parts[0]);
                    long tx = Long.parseLong(parts[8]);
                    return new long[]{rx, tx};
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return new long[]{0, 0};
    }

    // Helper to get all active non-loopback interfaces from /proc/net/dev
    private java.util.List<String> getActiveInterfaces() {
        java.util.List<String> ifaces = new java.util.ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("/proc/net/dev"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.contains(":")) {
                    String iface = line.split(":")[0].trim();
                    if (!iface.equals("lo")) {
                        ifaces.add(iface);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return ifaces;
    }
}
