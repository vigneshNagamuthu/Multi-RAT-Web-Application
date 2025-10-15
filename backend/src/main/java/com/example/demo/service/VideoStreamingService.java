package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class VideoStreamingService {
    
    private static final String AWS_SERVER = "54.169.114.206";
    private static final int VIDEO_PORT = 6060;
    
    private Process streamProcess;
    private boolean isStreaming = false;
    private Thread outputThread;

    public synchronized boolean startStreaming() {
        if (isStreaming) {
            System.out.println("⚠️ Streaming already active");
            return false;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            String cameraInput;
            
            if (os.contains("mac")) {
                cameraInput = "-f avfoundation -framerate 30 -video_size 1280x720 -i '0:none'";
            } else if (os.contains("linux")) {
                cameraInput = "-f v4l2 -framerate 30 -video_size 1280x720 -i /dev/video0";
            } else {
                cameraInput = "-f dshow -framerate 30 -video_size 1280x720 -i video='Integrated Camera'";
            }
            
            String command = String.format(
                "ffmpeg %s " +
                "-vcodec libx264 -preset ultrafast -tune zerolatency " +
                "-b:v 2M -maxrate 2M -bufsize 4M " +
                "-g 60 -keyint_min 60 " +
                "-f mpegts tcp://%s:%d",
                cameraInput, AWS_SERVER, VIDEO_PORT
            );
            
            System.out.println("🎥 Starting video stream to AWS: " + AWS_SERVER + ":" + VIDEO_PORT);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            
            streamProcess = pb.start();
            isStreaming = true;
            
            outputThread = new Thread(() -> readStreamOutput());
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

    public synchronized void stopStreaming() {
        if (streamProcess != null && streamProcess.isAlive()) {
            streamProcess.destroy();
            System.out.println("🛑 Stopped video streaming to AWS");
        }
        
        if (outputThread != null && outputThread.isAlive()) {
            outputThread.interrupt();
        }
        
        isStreaming = false;
    }

    private void readStreamOutput() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(streamProcess.getInputStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("fps=") || line.contains("bitrate=") || 
                    line.contains("speed=") || line.contains("error")) {
                    System.out.println("[FFmpeg] " + line);
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

    public boolean isStreaming() {
        return isStreaming && streamProcess != null && streamProcess.isAlive();
    }
    
    public String getServerIp() {
        return AWS_SERVER;
    }
    
    public int getPort() {
        return VIDEO_PORT;
    }
}