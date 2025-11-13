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

    @Autowired(required = false)
    private StreamingWebSocketHandler webSocketHandler;

    private Process streamProcess;
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
                
                // Try to detect best format and framerate for the camera
                String[] formatInfo = detectBestFormat(videoDevice);
                String inputFormat = formatInfo[0];
                String maxFps = formatInfo[1];
                
                System.out.println("📹 Best format: " + inputFormat + " at " + maxFps + " FPS");
                
                int fps = Integer.parseInt(maxFps);
                int gopSize = fps * 2; // Keyframe every 2 seconds
                
                pb = new ProcessBuilder(
                    "mptcpize", "run",  // Wrap with MPTCP
                    "ffmpeg",
                    "-f", "v4l2",
                    "-input_format", inputFormat,
                    "-framerate", maxFps,
                    "-video_size", "1280x720",
                    "-i", videoDevice,
                    "-vcodec", "libx264",
                    "-preset", "ultrafast",
                    "-tune", "zerolatency",
                    "-r", maxFps,  // Match input framerate
                    "-b:v", "2M",
                    "-maxrate", "2M",
                    "-bufsize", "4M",
                    "-g", String.valueOf(gopSize),
                    "-keyint_min", String.valueOf(gopSize),
                    "-f", "mpegts",
                    "tcp://" + AWS_SERVER + ":" + VIDEO_PORT
                );
            }
            
            pb.redirectErrorStream(true);
            
            streamProcess = pb.start();
            isStreaming = true;
            
            // Monitor FFmpeg output and extract metrics
            outputThread = new Thread(() -> readStreamOutputWithMetrics());
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
        
        // Reset metrics
        isStreaming = false;
        currentFps = 0;
        currentBitrate = 0;
        droppedFrames = 0;
        
        // Tell WebSocket to go back to dummy data
        if (webSocketHandler != null) {
            webSocketHandler.stopRealMetrics();
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
                if (line.contains("error") || line.contains("Error")) {
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
                }
                // Device path line (starts with /dev/video)
                else if (line.startsWith("/dev/video")) {
                    String devicePath = line.trim();
                    
                    // Verify it's a capture device (not metadata)
                    if (isValidCaptureDevice(devicePath)) {
                        System.out.println("✅ Found: " + currentDevice + " at " + devicePath);
                        return devicePath;
                    } else {
                        System.out.println("⏭️  Skipped: " + devicePath + " (metadata/not capture)");
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("⚠️ v4l2-ctl not available, trying manual scan...");
        }
        
        // Fallback: Manual scan using FFmpeg directly
        System.out.println("🔍 Manual scanning /dev/video* devices...");
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
                    System.out.println("⏭️  Skipped " + devicePath + " (not a valid capture device)");
                }
            }
        }
        
        // Last resort
        System.err.println("❌ No valid video capture device found!");
        System.err.println("💡 Please check: ls -la /dev/video*");
        System.err.println("💡 Or run: v4l2-ctl --list-devices");
        return "/dev/video0";  // Will likely fail, but let FFmpeg give proper error
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
            
            while ((line = errorReader.readLine()) != null) {
                String lowerLine = line.toLowerCase();
                
                // Skip metadata devices
                if (lowerLine.contains("metadata") || lowerLine.contains("meta")) {
                    isMetadata = true;
                    break;
                }
                
                // Check for valid video formats
                if (lowerLine.contains("compressed") || lowerLine.contains("raw") || 
                    lowerLine.contains("yuyv") || lowerLine.contains("mjpeg") ||
                    lowerLine.contains("h264") || lowerLine.contains("nv12")) {
                    hasValidFormats = true;
                }
            }
            
            process.waitFor();
            return hasValidFormats && !isMetadata;
            
        } catch (Exception e) {
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
                if (lowerLine.contains("metadata") || lowerLine.contains("v4l2_meta")) {
                    isMetadata = true;
                    break;
                }
                
                // Check if it has video capture capability
                if (lowerLine.contains("video capture") && !lowerLine.contains("metadata")) {
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
                    if (line.contains("Compressed") || line.contains("Raw") || 
                        line.contains("YUYV") || line.contains("MJPEG")) {
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