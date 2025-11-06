# 🚀 Quick Start Guide - Video Streaming Test

## Prerequisites Checklist

- [ ] FFmpeg installed on Windows (`ffmpeg -version`)
- [ ] Node.js and npm installed
- [ ] Maven installed
- [ ] AWS Lightsail firewall: Port 6060 opened (TCP)
- [ ] AWS Lightsail firewall: Port 80 opened (HTTP)

---

## 📋 Step-by-Step Startup Commands

### 1️⃣ **AWS Server (SSH via PuTTY)**

```bash
# SSH into your AWS server
ssh admin@54.169.114.206
# OR
ssh ubuntu@54.169.114.206

# Start FFmpeg video receiver (background mode)
nohup ~/start_video_server.sh > ~/ffmpeg.log 2>&1 &

# Verify FFmpeg is listening on port 6060
sudo netstat -tulpn | grep 6060

# Expected output:
# tcp        0      0 0.0.0.0:6060            0.0.0.0:*               LISTEN      xxxxx/ffmpeg

# Check FFmpeg logs (optional)
tail -f ~/ffmpeg.log

# Press Ctrl+C to stop viewing logs (FFmpeg keeps running)
```

**Troubleshooting Commands:**
```bash
# Check if FFmpeg is running
ps aux | grep ffmpeg

# Stop FFmpeg
pkill -f ffmpeg

# Restart FFmpeg
nohup ~/start_video_server.sh > ~/ffmpeg.log 2>&1 &

# Check Nginx is running
sudo systemctl status nginx

# Restart Nginx if needed
sudo systemctl restart nginx
```

---

### 2️⃣ **Backend (Windows PowerShell)**

**Open PowerShell Terminal #1:**

```powershell
# Navigate to backend directory
cd C:\Users\itsam\OneDrive\Documents\GitHub\Multi-RAT-Web-Application\backend

# Clean and build (first time or after code changes)
mvn clean install

# Start Spring Boot backend
mvn spring-boot:run

# Wait for: "✅ Backend is ready!"
```

**Expected Output:**
```
  ____             _                  _
 | __ )  __ _  ___| | _____ _ __   __| |
 |  _ \ / _` |/ __| |/ / _ \ '_ \ / _` |
 | |_) | (_| | (__|   <  __/ | | | (_| |
 |____/ \__,_|\___|_|\_\___|_| |_|\__,_|

✅ Backend is ready!
Started BackendApplication in X.XXX seconds
```

**Keep this terminal running!**

---

### 3️⃣ **Frontend (Windows PowerShell)**

**Open PowerShell Terminal #2:**

```powershell
# Navigate to frontend directory
cd C:\Users\itsam\OneDrive\Documents\GitHub\Multi-RAT-Web-Application\frontend

# Install dependencies (first time only)
npm install

# Start React development server
npm start

# Browser should auto-open to: http://localhost:3000
```

**Expected Output:**
```
Compiled successfully!

You can now view frontend in the browser.

  Local:            http://localhost:3000
  On Your Network:  http://192.168.x.x:3000

webpack compiled with 0 errors
```

**Keep this terminal running!**

---

## 🎥 Testing the Video Stream

### **Step 1: Open the Website**
- Browser should auto-open to: `http://localhost:3000`
- Or manually open: `http://localhost:3000`

### **Step 2: Navigate to Video Streaming Page**
- Click **"Video Streaming"** in the navigation bar

### **Step 3: Start Streaming**
1. Click **"🚀 Start AWS Stream"** button
2. Wait 5-10 seconds
3. Video should appear in the player

### **What You Should See:**

**✅ On Backend Terminal (Windows):**
```
🎥 Starting video stream to AWS: 54.169.114.206:6060
✅ Video streaming started successfully
[FFmpeg] Input #0, dshow, from 'video=Integrated Camera':
[FFmpeg] Stream #0:0: Video: rawvideo, yuyv422, 1280x720, 30 fps
[FFmpeg] Output #0, mpegts, to 'tcp://54.169.114.206:6060':
[FFmpeg] frame=  120 fps= 30 q=28.0 size=    2048kB time=00:00:04.00
```

**✅ On AWS Terminal (tail -f ~/ffmpeg.log):**
```
Input #0, mpegts, from 'tcp://0.0.0.0:6060':
  Duration: N/A, start: ...
  Stream #0:0: Video: h264 (High), yuv420p, 1280x720, 30 fps
Output #0, hls, to '/var/www/html/stream/playlist.m3u8':
  Stream #0:0: Video: h264, 1280x720, 30 fps
frame=  120 fps= 30 q=28.0 size=    1024kB time=00:00:04.00
```

**✅ On Website:**
- Video player shows your webcam feed
- 🔴 LIVE badge visible
- Real-time metrics updating

---

## 🛑 Stopping Everything

### **Stop Frontend:**
- Go to frontend terminal
- Press `Ctrl + C`

### **Stop Backend:**
- Go to backend terminal
- Press `Ctrl + C`

### **Stop AWS FFmpeg:**
```bash
# SSH into AWS
ssh admin@54.169.114.206

# Stop FFmpeg
pkill -f ffmpeg

# Verify it's stopped
ps aux | grep ffmpeg
```

---

## 🔧 Common Issues & Fixes

### ❌ "Connection to tcp://54.169.114.206:6060 failed"

**Cause:** FFmpeg not running on AWS or firewall blocking port 6060

**Fix:**
```bash
# On AWS server
sudo netstat -tulpn | grep 6060

# If nothing shows, restart FFmpeg
nohup ~/start_video_server.sh > ~/ffmpeg.log 2>&1 &

# Check AWS Lightsail firewall - ensure port 6060 is open
```

**Test from Windows:**
```powershell
Test-NetConnection -ComputerName 54.169.114.206 -Port 6060
# Should show: TcpTestSucceeded : True
```

---

### ❌ "Video buffering but not playing"

**Cause:** HLS playlist not being created or CORS issue

**Fix:**
```bash
# On AWS server - check if HLS files are being created
ls -lh /var/www/html/stream/

# Should see: playlist.m3u8 and segment*.ts files

# Test HLS URL directly
curl http://54.169.114.206/stream/playlist.m3u8

# Should return playlist content, not 404
```

---

### ❌ "'ffmpeg' is not recognized" (Windows)

**Cause:** FFmpeg not installed or not in PATH

**Fix:**
```powershell
# Test if FFmpeg is installed
ffmpeg -version

# If not found, download from: https://www.gyan.dev/ffmpeg/builds/
# Extract to C:\ffmpeg
# Add to PATH: C:\ffmpeg\bin
```

---

### ❌ "real-time buffer too full! frame dropped!"

**Cause:** Camera producing frames faster than encoding

**Fix:** Reduce camera resolution or frame rate (optional - doesn't prevent streaming)

---

### ❌ Backend won't start - port 8080 in use

**Fix:**
```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill the process (replace XXXX with PID)
taskkill /PID XXXX /F
```

---

### ❌ Frontend won't start - port 3000 in use

**Fix:**
```powershell
# Find process using port 3000
netstat -ano | findstr :3000

# Kill the process (replace XXXX with PID)
taskkill /PID XXXX /F
```

---

## 📊 Architecture Overview

```
┌─────────────────┐
│  Your Webcam    │
└────────┬────────┘
         │ FFmpeg captures
         ▼
┌─────────────────────────┐
│  Spring Boot Backend    │
│  localhost:8080         │
│  (VideoStreamingService)│
└────────┬────────────────┘
         │ TCP stream (MPEGTS)
         │ Port 6060
         ▼
┌─────────────────────────┐
│  AWS Lightsail Server   │
│  54.169.114.206:6060    │
│  (FFmpeg receiver)      │
└────────┬────────────────┘
         │ Converts to HLS
         │ Saves to /var/www/html/stream/
         ▼
┌─────────────────────────┐
│  Nginx Web Server       │
│  http://54.169.114.206  │
│  Serves HLS playlist    │
└────────┬────────────────┘
         │ HTTP requests
         │ playlist.m3u8 + segments
         ▼
┌─────────────────────────┐
│  React Frontend         │
│  localhost:3000         │
│  (HLS.js player)        │
└─────────────────────────┘
```

---

## 📁 Important Files

### **Backend:**
- `backend/src/main/java/com/example/demo/service/VideoStreamingService.java` - Camera streaming logic
- `backend/src/main/java/com/example/demo/controller/StreamingController.java` - API endpoints

### **Frontend:**
- `frontend/src/pages/StreamingPage.js` - Video streaming UI
- `frontend/public/index.html` - Loads HLS.js library

### **AWS:**
- `~/start_video_server.sh` - FFmpeg receiver script
- `/etc/nginx/sites-available/default` - Nginx config
- `/var/www/html/stream/` - HLS output directory
- `~/ffmpeg.log` - FFmpeg logs

---

## 🎯 Next Steps (After Working)

1. ✅ Basic video streaming works
2. Add latency measurement (compare timestamps)
3. Optimize for lower latency (reduce HLS segment size)
4. Move to MPTCP-enabled kernel
5. Test with multiple network interfaces
6. Compare different MPTCP schedulers

---

## 📚 Related Documentation

- **AWS_SETUP.md** - Detailed AWS server setup
- **STREAMING_TEST_GUIDE.md** - Troubleshooting guide
- **startup_guide.txt** - Original startup instructions

---

## 💡 Tips

- **Use 2 monitors:** One for terminals, one for browser
- **Keep AWS FFmpeg running:** Use `nohup` or `screen`
- **Check browser console:** F12 → Console tab for HLS errors
- **Monitor AWS logs:** `tail -f ~/ffmpeg.log`
- **Test connectivity:** Use `Test-NetConnection` before streaming

---

**Last Updated:** October 15, 2025
