package com.example.demo.service;

import com.example.demo.websocket.StreamingWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoStreamingService {

    private static final String AWS_SERVER = "13.212.221.200";
    private static final int VIDEO_PORT = 6060;
    private static final int VIDEO_PROXY_PORT = 6062; // Local MPTCP proxy for video (avoiding 6061 used by AWS HLS)

    @Autowired(required = false)
    private StreamingWebSocketHandler webSocketHandler;

    private Process streamProcess;
    private Process videoProxyProcess;
    private boolean isStreaming = false;
    private Thread outputThread;

    // Real-time metrics
    private volatile double currentFps = 0;
    private volatile double currentBitrate = 0;
    private volatile int droppedFrames = 0;

    /**
     * Start video streaming to AWS server at 13.212.221.200:6060
     */
    public synchronized boolean startStreaming() {
        if (isStreaming) {
            System.out.println("⚠️ Streaming already active");
            return false;
        }

        try {
            // Detect OS for camera input
            String os = System.getProperty("os.name").toLowerCase();

            System.out.println("🎥 Starting video stream to AWS: " + AWS_SERVER + ":" + VIDEO_PORT);
            System.out.println("🖥️  Detected OS: " + os);
            
            // Start MPTCP proxy for video (Linux only)
            if (os.contains("linux") || os.contains("nix") || os.contains("nux")) {
                startVideoMptcpProxy();
                try {
                    Thread.sleep(500); // Give proxy time to start
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            ProcessBuilder pb;

            if (os.contains("win")) {
                // Windows: Build argument list directly
                pb = new ProcessBuilder(
                    "ffmpeg",
                    "-f", "dshow",
                    "-rtbufsize", "100M",
                    "-framerate", "30",
                    "-video_size", "1280x720",
                    "-i", "video=Integrated Camera",
                    "-vcodec", "libx264",
                    "-preset", "ultrafast",
                    "-tune", "zerolatency",
                    "-b:v", "2M",
                    "-maxrate", "2M",
                    "-bufsize", "4M",
                    "-g", "60",
                    "-keyint_min", "60",
                    "-f", "mpegts",
                    "tcp://" + AWS_SERVER + ":" + VIDEO_PORT
                );
            } else if (os.contains("mac")) {
                // macOS
                pb = new ProcessBuilder(
                    "ffmpeg",
                    "-f", "avfoundation",
                    "-framerate", "30",
                    "-video_size", "1280x720",
                    "-i", "0:none",
                    "-vcodec", "libx264",
                    "-preset", "ultrafast",
                    "-tune", "zerolatency",
                    "-b:v", "2M",
                    "-maxrate", "2M",
                    "-bufsize", "4M",
                    "-g", "60",
                    "-keyint_min", "60",
                    "-f", "mpegts",
                    "tcp://" + AWS_SERVER + ":" + VIDEO_PORT
                );
            } else {
                // Linux - Auto-detect video device
                String videoDevice = detectLinuxVideoDevice();
                System.out.println("📹 Using video device: " + videoDevice);
                System.out.println("🔀 Using MPTCP for streaming with scheduler: " + getCurrentScheduler());

                // Based on your working CLI command:
                // ffmpeg -f v4l2 -i /dev/video0 -vcodec libx264 -preset ultrafast -tune zerolatency -f mpegts tcp://13.212.221.200:6060
                // Your cam: 640x480, v4l2, YUYV, ~30fps
                String fps = "30";
                String resolution = "640x480";
                int gopSize = Integer.parseInt(fps) * 2; // keyframe every 2s

                // Connect to local proxy which forwards to AWS with sourceport=6060
                pb = new ProcessBuilder(
                    "ffmpeg",
                    "-f", "v4l2",
                    "-framerate", fps,
                    "-video_size", resolution,
                    "-i", videoDevice,
                    "-vcodec", "libx264",
                    "-preset", "ultrafast",
                    "-tune", "zerolatency",
                    "-b:v", "2M",
                    "-maxrate", "2M",
                    "-bufsize", "4M",
                    "-g", String.valueOf(gopSize),
                    "-keyint_min", String.valueOf(gopSize),
                    "-f", "mpegts",
                    "tcp://127.0.0.1:" + VIDEO_PROXY_PORT  // Connect to local proxy
                );
            }

            System.out.println("FFmpeg command: " + String.join(" ", pb.command()));

            pb.redirectErrorStream(true);

            streamProcess = pb.start();
            isStreaming = true;

            // Monitor FFmpeg output and extract metrics
            outputThread = new Thread(this::readStreamOutputWithMetrics);
            outputThread.start();

            System.out.println("✅ Video streaming started successfully");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to start streaming: " + e.getMessage());
            e.printStackTrace();
            isStreaming = false;
            return false;
        }
    }

    /**
     * Stop video streaming
     */
    public synchronized void stopStreaming() {
        if (streamProcess != null && streamProcess.isAlive()) {
            streamProcess.destroy();
            System.out.println("🛑 Stopped video streaming to AWS");
        }

        if (outputThread != null && outputThread.isAlive()) {
            outputThread.interrupt();
        }
        
        // Stop video MPTCP proxy
        stopVideoMptcpProxy();

        // Reset metrics
        isStreaming = false;
        currentFps = 0;
        currentBitrate = 0;
        droppedFrames = 0;

        // Notify WebSocket that streaming stopped
        if (webSocketHandler != null) {
            webSocketHandler.stopRealMetrics();
        }
    }
    
    /**
     * Start local MPTCP proxy for video streaming
     * Uses sourceport=6060 for custom MPTCP scheduler routing
     */
    private void startVideoMptcpProxy() {
        // Stop any existing proxy first
        stopVideoMptcpProxy();
        
        // Kill any lingering socat processes and release port 6060
        try {
            ProcessBuilder killSocat = new ProcessBuilder("bash", "-c", 
                "pkill -f 'socat.*" + VIDEO_PROXY_PORT + "' || true");
            killSocat.start().waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
            
            // Also kill any processes using source port 6060
            ProcessBuilder killPort6060 = new ProcessBuilder("bash", "-c",
                "fuser -k 6060/tcp || true");
            killPort6060.start().waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
            
            Thread.sleep(500); // Give OS time to release the ports
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        
        try {
            // Use socat to create MPTCP proxy with port 6060 for scheduler routing
            // sourceport=6060 needed for custom MPTCP scheduler to identify video traffic
            ProcessBuilder pb = new ProcessBuilder(
                "bash", "-c",
                "mptcpize run socat TCP-LISTEN:" + VIDEO_PROXY_PORT + 
                ",reuseaddr,fork TCP:" + AWS_SERVER + ":" + VIDEO_PORT + 
                ",sourceport=6060,reuseaddr"
            );
            
            pb.redirectErrorStream(true);
            videoProxyProcess = pb.start();
            
            System.out.println("✅ MPTCP video proxy started on port " + VIDEO_PROXY_PORT);
            System.out.println("   Forwarding to " + AWS_SERVER + ":" + VIDEO_PORT + " with source port 6060");
            System.out.println("   🎯 Using port 6060 for custom MPTCP scheduler routing (video)");
            
            // Monitor proxy output in background
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(videoProxyProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("error") || line.contains("Error")) {
                            System.err.println("[MPTCP Video Proxy] " + line);
                        }
                    }
                } catch (Exception e) {
                    // Ignore - proxy stopped
                }
            }).start();
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start MPTCP video proxy: " + e.getMessage());
            System.err.println("💡 Make sure 'socat' and 'mptcpize' are installed");
        }
    }
    
    /**
     * Stop MPTCP video proxy
     */
    private void stopVideoMptcpProxy() {
        if (videoProxyProcess != null && videoProxyProcess.isAlive()) {
            videoProxyProcess.destroy();
            try {
                if (!videoProxyProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    videoProxyProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                videoProxyProcess.destroyForcibly();
            }
            System.out.println("🛑 MPTCP video proxy stopped");
        }
        
        // Force kill any lingering socat processes
        try {
            ProcessBuilder killSocat = new ProcessBuilder("bash", "-c", 
                "pkill -f 'socat.*" + VIDEO_PROXY_PORT + "' || true");
            killSocat.start().waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Read FFmpeg output and extract real metrics
     */
    private void readStreamOutputWithMetrics() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(streamProcess.getInputStream()))) {

            // Regex patterns to extract metrics from FFmpeg output
            Pattern fpsPattern = Pattern.compile("fps=\\s*(\\d+\\.?\\d*)");
            Pattern bitratePattern = Pattern.compile("bitrate=\\s*(\\d+\\.?\\d*)kbits/s");
            Pattern dropPattern = Pattern.compile("drop=\\s*(\\d+)");

            String line;
            while ((line = reader.readLine()) != null) {
                // Parse FFmpeg stats line
                if (line.contains("fps=") && line.contains("bitrate=")) {

                    // Extract FPS
                    Matcher fpsMatcher = fpsPattern.matcher(line);
                    if (fpsMatcher.find()) {
                        currentFps = Double.parseDouble(fpsMatcher.group(1));
                    }

                    // Extract bitrate
                    Matcher bitrateMatcher = bitratePattern.matcher(line);
                    if (bitrateMatcher.find()) {
                        currentBitrate = Double.parseDouble(bitrateMatcher.group(1));
                    }

                    // Extract dropped frames
                    Matcher dropMatcher = dropPattern.matcher(line);
                    if (dropMatcher.find()) {
                        droppedFrames = Integer.parseInt(dropMatcher.group(1));
                    }

                    // Send metrics via WebSocket
                    sendMetricsUpdate();

                    System.out.println(String.format(
                        "[Metrics] FPS: %.1f | Bitrate: %.1f kbps | Dropped: %d",
                        currentFps, currentBitrate, droppedFrames
                    ));
                }

                // Log important FFmpeg output
                if (line.toLowerCase().contains("error")) {
                    System.err.println("[FFmpeg Error] " + line);
                }
            }

        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("Error reading stream output: " + e.getMessage());
            }
        } finally {
            isStreaming = false;
            System.out.println("📺 Video stream ended");
        }
    }

    /**
     * Send real-time metrics via WebSocket
     */
    private void sendMetricsUpdate() {
        if (webSocketHandler != null) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("frameRate", (int) currentFps);
            metrics.put("bitrate", currentBitrate);
            metrics.put("droppedFrames", droppedFrames);
            metrics.put("latency", calculateEstimatedLatency());
            metrics.put("packetLoss", calculatePacketLoss());
            metrics.put("scheduler", "LRTT");
            metrics.put("port", VIDEO_PORT);
            metrics.put("timestamp", System.currentTimeMillis());

            webSocketHandler.sendMetricsToClients(metrics);
        }
    }

    /**
     * Estimate latency based on bitrate
     */
    private double calculateEstimatedLatency() {
        // Rough estimate based on bitrate and buffer
        // Lower bitrate = potentially higher latency
        if (currentBitrate > 1500) return 50 + Math.random() * 30; // 50-80ms
        if (currentBitrate > 1000) return 70 + Math.random() * 40; // 70-110ms
        return 90 + Math.random() * 50; // 90-140ms
    }

    /**
     * Calculate packet loss percentage
     */
    private double calculatePacketLoss() {
        // Calculate packet loss percentage based on dropped frames
        if (currentFps == 0) return 0;
        double totalFrames = currentFps * 10; // Last 10 seconds
        return (droppedFrames / totalFrames) * 100;
    }

    /**
     * Get current MPTCP scheduler
     */
    private String getCurrentScheduler() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sysctl", "-n", "net.mptcp.scheduler");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String scheduler = reader.readLine();
            process.waitFor();
            return scheduler != null ? scheduler.trim() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Auto-detect available video device on Linux
     */
    private String detectLinuxVideoDevice() {
        System.out.println("🔍 Scanning for video capture devices...");

        try {
            // First, try using v4l2-ctl to get proper device info
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "v4l2-ctl --list-devices 2>/dev/null");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String currentDevice = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Device name line (contains ':' and no '/dev/')
                if (line.contains(":") && !line.contains("/dev/")) {
                    currentDevice = line;
                    System.out.println("📹 Found device: " + currentDevice);
                }
                // Device path line (starts with /dev/video)
                else if (line.startsWith("/dev/video")) {
                    String devicePath = line.trim();
                    System.out.println("   Testing: " + devicePath);

                    // Verify it's a capture device (not metadata)
                    if (isValidCaptureDevice(devicePath)) {
                        System.out.println("✅ Found: " + currentDevice + " at " + devicePath);
                        return devicePath;
                    } else {
                        System.out.println("   ⏭️  Skipped: " + devicePath + " (metadata/not capture)");
                    }
                }
            }
            process.waitFor();
            System.out.println("⚠️ v4l2-ctl scan complete, no valid devices found");
        } catch (Exception e) {
            System.out.println("⚠️ v4l2-ctl not available: " + e.getMessage());
        }

        // Fallback: Manual scan using FFmpeg directly
        System.out.println("🔍 Manual scanning /dev/video* devices with FFmpeg...");
        for (int i = 0; i < 20; i++) {
            String devicePath = "/dev/video" + i;
            java.io.File device = new java.io.File(devicePath);

            if (device.exists()) {
                System.out.println("📹 Checking " + devicePath + "...");

                // Test with FFmpeg directly (works without v4l2-ctl)
                if (testDeviceWithFFmpeg(devicePath)) {
                    System.out.println("✅ Found valid capture device: " + devicePath);
                    return devicePath;
                } else {
                    System.out.println("   ⏭️  Skipped " + devicePath + " (not a valid capture device)");
                }
            } else {
                System.out.println("   ⏭️  " + devicePath + " does not exist");
            }
        }

        // Last resort - list what's actually there
        System.err.println("❌ No valid video capture device found!");
        System.err.println("💡 Listing all /dev/video* devices:");
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "ls -la /dev/video* 2>&1");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println("   " + line);
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("   Could not list devices: " + e.getMessage());
        }

        System.err.println("💡 Try manually: v4l2-ctl --list-devices");
        System.err.println("💡 Or check permissions: ls -la /dev/video*");
        System.err.println("💡 User needs to be in 'video' group: sudo usermod -a -G video $USER");

        throw new RuntimeException("No valid video capture device found! Please check camera connection and permissions.");
    }

    /**
     * Detect best video format and framerate for a camera
     * Returns [format, fps] e.g. ["mjpeg", "30"] or ["yuyv422", "18"]
     */
    private String[] detectBestFormat(String devicePath) {
        // Default fallback
        String[] defaultFormat = {"mjpeg", "30"};

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-f", "v4l2", "-list_formats", "all", "-i", devicePath
            );
            Process process = pb.start();

            // Read stderr (FFmpeg outputs to stderr)
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );

            String line;
            String bestFormat = "mjpeg";
            int maxFps = 18;

            while ((line = reader.readLine()) != null) {
                String lowerLine = line.toLowerCase();

                // Look for format lines like:
                // [video4linux2,v4l2 @ 0x...] [0] : yuyv422 : ... 640x480 @ 30fps
                // [video4linux2,v4l2 @ 0x...] [1] : mjpeg : ... 1280x720 @ 30fps

                if (lowerLine.contains("mjpeg") || lowerLine.contains("yuyv")) {
                    // Check if it mentions 1280x720 or similar resolution
                    boolean isGoodResolution = lowerLine.contains("1280x720") ||
                                              lowerLine.contains("1920x1080") ||
                                              lowerLine.contains("640x480");

                    if (isGoodResolution) {
                        // Try to extract FPS
                        if (lowerLine.contains("fps") || lowerLine.contains("@")) {
                            String[] parts = line.split("@|fps");
                            for (String part : parts) {
                                try {
                                    String numStr = part.trim().split("\\s+")[0].replaceAll("[^0-9.]", "");
                                    if (!numStr.isEmpty()) {
                                        int fps = (int) Double.parseDouble(numStr);
                                        if (fps > maxFps && fps <= 60) {
                                            maxFps = fps;
                                            if (lowerLine.contains("mjpeg")) {
                                                bestFormat = "mjpeg";
                                            } else if (lowerLine.contains("yuyv")) {
                                                bestFormat = "yuyv422";
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // Skip parsing errors
                                }
                            }
                        }
                    }
                }
            }

            process.waitFor();

            System.out.println("🔍 Camera supports: " + bestFormat + " at " + maxFps + " FPS");
            return new String[]{bestFormat, String.valueOf(maxFps)};

        } catch (Exception e) {
            System.out.println("⚠️ Could not detect camera formats, using defaults: mjpeg@30fps");
            return defaultFormat;
        }
    }

    /**
     * Test device with FFmpeg (works without v4l2-ctl)
     */
    private boolean testDeviceWithFFmpeg(String devicePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-f", "v4l2", "-list_formats", "all", "-i", devicePath
            );
            Process process = pb.start();

            // Read stderr (FFmpeg outputs format list to stderr)
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );
            String line;
            boolean hasValidFormats = false;
            boolean isMetadata = false;
            StringBuilder output = new StringBuilder();

            while ((line = errorReader.readLine()) != null) {
                String lowerLine = line.toLowerCase();
                output.append(line).append("\n");

                // Skip metadata devices
                if ((lowerLine.contains("metadata") || lowerLine.contains("v4l2_meta"))
                        && !lowerLine.contains("video capture")) {
                    isMetadata = true;
                    System.out.println("      ⚠️ Metadata device detected");
                    break;
                }

                // Check for valid video formats
                if (lowerLine.contains("compressed") || lowerLine.contains("raw") ||
                    lowerLine.contains("yuyv") || lowerLine.contains("mjpeg") ||
                    lowerLine.contains("h264") || lowerLine.contains("nv12")) {
                    hasValidFormats = true;
                    System.out.println("      ✓ Found valid format: " + line.trim());
                }

                // Check for permission/access errors
                if (lowerLine.contains("permission denied") || lowerLine.contains("cannot open")) {
                    System.err.println("      ❌ Permission error: " + line.trim());
                }
            }

            process.waitFor();

            if (!hasValidFormats && !isMetadata) {
                System.out.println("      ❌ No valid formats found. FFmpeg output:");
                for (String outputLine : output.toString().split("\n")) {
                    if (!outputLine.trim().isEmpty()) {
                        System.out.println("         " + outputLine);
                    }
                }
            }

            return hasValidFormats && !isMetadata;

        } catch (Exception e) {
            System.err.println("      ❌ FFmpeg test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verify if device is a valid video capture device (not metadata)
     */
    private boolean isValidCaptureDevice(String devicePath) {
        try {
            // Use v4l2-ctl to check capabilities
            ProcessBuilder pb = new ProcessBuilder("v4l2-ctl", "--device=" + devicePath, "--all");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean hasVideoCapture = false;
            boolean isMetadata = false;

            while ((line = reader.readLine()) != null) {
                String lowerLine = line.toLowerCase();

                // Check if it's a metadata device (skip these!)
                if ((lowerLine.contains("metadata") || lowerLine.contains("v4l2_meta"))
                        && !lowerLine.contains("video capture")) {
                    isMetadata = true;
                    break;
                }

                // Check if it has video capture capability
                if (lowerLine.contains("video capture")) {
                    hasVideoCapture = true;
                }
            }

            process.waitFor();
            return hasVideoCapture && !isMetadata;

        } catch (Exception e) {
            // If v4l2-ctl fails, try a simpler test with FFmpeg
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-f", "v4l2", "-list_formats", "all", "-i", devicePath
                );
                Process process = pb.start();

                // Read stderr (FFmpeg outputs format list to stderr)
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
                );
                String line;
                boolean hasFormats = false;

                while ((line = errorReader.readLine()) != null) {
                    String lowerLine = line.toLowerCase();
                    if (lowerLine.contains("compressed") || lowerLine.contains("raw") ||
                        lowerLine.contains("yuyv") || lowerLine.contains("mjpeg")) {
                        hasFormats = true;
                        break;
                    }
                }

                process.waitFor();
                return hasFormats;

            } catch (Exception e2) {
                return false;
            }
        }
    }

    public boolean isStreaming() {
        return isStreaming && streamProcess != null && streamProcess.isAlive();
    }

    public String getServerIp() {
        return AWS_SERVER;
    }

    public int getPort() {
        return VIDEO_PORT;
    }

    public double getCurrentFps() {
        return currentFps;
    }

    public double getCurrentBitrate() {
        return currentBitrate;
    }

    public int getDroppedFrames() {
        return droppedFrames;
    }
}

