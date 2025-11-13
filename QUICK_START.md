# 🚀 Quick Start Guide - Multi-RAT Web Application

## 📋 Prerequisites

### System Requirements (Must Install First)

#### **Linux/Mac Users:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-17-jdk maven nodejs npm ffmpeg v4l-utils

# Check installations
java -version    # Should be 17+
mvn -version
node -v          # Should be 14+
npm -v
ffmpeg -version
```

#### **Optional: MPTCP Support**
```bash
# Install mptcpize for multipath TCP
sudo apt install mptcpize

# Or install from source:
# https://github.com/multipath-tcp/mptcpd
```

#### **Camera Permissions (Linux):**
```bash
# Add your user to video group
sudo usermod -a -G video $USER

# Logout and login for changes to take effect
# Verify with: groups
```

### AWS Server Requirements
- [ ] AWS server running at 13.212.221.200
- [ ] Port 6060 opened (Video input)
- [ ] Port 6061 opened (HLS output)
- [ ] `relay.js` running with PM2

---

## 🚀 Quick Setup (Automated)

### **First Time Setup:**

```bash
# 1. Clone the repository
git clone <repository-url>
cd Multi-RAT-Web-Application

# 2. Run setup script (installs all dependencies)
./setup.sh

# 3. Start the application
./start.sh
```

**That's it!** The application will start automatically:
- Backend: http://localhost:8080
- Frontend: http://localhost:3000

---

## 🛠️ Manual Setup (Alternative)

If you prefer to run commands manually:

---

## 📋 Manual Setup Commands

### **Backend Setup:**
```bash
cd backend
mvn clean install
mvn spring-boot:run
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

### **Frontend Setup (in separate terminal):**
```bash
cd frontend
npm install
npm start
```

**Expected Output:**
```
Compiled successfully!

You can now view frontend in the browser.

  Local:            http://localhost:3000
  On Your Network:  http://192.168.x.x:3000

webpack compiled with 0 errors
```

---

## 🖥️ AWS Server Setup

### **Initial Server Configuration:**

```bash
# SSH into AWS server
ssh admin@13.212.221.200

# Update system
sudo apt update && sudo apt upgrade -y
```

### **1. Enable MPTCP (Multipath TCP):**

```bash
# Check if MPTCP is available
sysctl net.mptcp.enabled

# Enable MPTCP
sudo sysctl -w net.mptcp.enabled=1

# Make it permanent (survives reboot)
echo "net.mptcp.enabled=1" | sudo tee -a /etc/sysctl.conf

# Set MPTCP scheduler (optional, default is usually fine)
sudo sysctl -w net.mptcp.scheduler=default

# Verify MPTCP is enabled
sysctl net.mptcp.enabled
# Should output: net.mptcp.enabled = 1
```

### **2. Install Required Dependencies:**

```bash
# Install Node.js and npm
sudo apt install -y nodejs npm

# Install FFmpeg
sudo apt install -y ffmpeg

# Install PM2 (process manager)
sudo npm install -g pm2

# Install HLS dependencies
cd ~
mkdir -p stream-relay sensor-listener
```

### **3. Setup Project Structure:**

```bash
# Expected directory structure:
# ~/
# ├── sensor-listener/
# │   ├── sensor-listener.js
# │   └── sensor.log
# └── stream-relay/
#     ├── relay.js
#     ├── package.json
#     ├── hls/            (created automatically)
#     └── stream.log

# Create directories
mkdir -p ~/sensor-listener
mkdir -p ~/stream-relay/hls

# Upload your files (from your local machine):
# scp relay.js admin@13.212.221.200:~/stream-relay/
# scp sensor-listener.js admin@13.212.221.200:~/sensor-listener/
```

### **4. Install Node.js Dependencies for Relay:**

```bash
cd ~/stream-relay

# If package.json doesn't exist, create it:
cat > package.json << 'EOF'
{
  "name": "stream-relay",
  "version": "1.0.0",
  "description": "HLS video relay server",
  "main": "relay.js",
  "dependencies": {},
  "scripts": {
    "start": "node relay.js"
  }
}
EOF

# Install dependencies (if any)
npm install
```

### **5. Start Services with PM2:**

```bash
# Start video relay server
cd ~/stream-relay
pm2 start relay.js --name stream-relay --log stream.log

# Start sensor listener
cd ~/sensor-listener
pm2 start sensor-listener.js --name sensor-listener --log sensor.log

# Check status
pm2 status

# Should show:
# ┌─────┬──────────────────┬─────────┬─────────┬──────────┐
# │ id  │ name             │ status  │ restart │ uptime   │
# ├─────┼──────────────────┼─────────┼─────────┼──────────┤
# │ 0   │ stream-relay     │ online  │ 0       │ 5s       │
# │ 1   │ sensor-listener  │ online  │ 0       │ 3s       │
# └─────┴──────────────────┴─────────┴─────────┴──────────┘
```

### **6. Configure PM2 Auto-start on Reboot:**

This ensures your services automatically restart if the AWS server reboots.

```bash
# Step 1: Generate the startup script
pm2 startup

# This will output a command like:
# [PM2] You have to run this command as root. Execute the following command:
# sudo env PATH=$PATH:/usr/bin /usr/lib/node_modules/pm2/bin/pm2 startup systemd -u admin --hp /home/admin

# Step 2: Copy and run the command it gives you (example):
sudo env PATH=$PATH:/usr/bin /usr/lib/node_modules/pm2/bin/pm2 startup systemd -u admin --hp /home/admin

# Step 3: Save the current PM2 process list (IMPORTANT!)
pm2 save

# This creates a dump file at ~/.pm2/dump.pm2 with all your running processes

# Step 4: Verify startup is enabled
sudo systemctl status pm2-admin
# Should show: Active: active (running)

# Step 5: Test by rebooting (optional)
sudo reboot

# After reboot, SSH back in and check:
pm2 list
# Your services should be running automatically!
```

**What this does:**
- `pm2 startup` - Creates a systemd service that starts PM2 on boot
- `pm2 save` - Saves your current process list (stream-relay, sensor-listener) to be restored on boot
- After reboot, PM2 automatically starts and resurrects all saved processes

**Important Notes:**
- Run `pm2 save` every time you add/remove/modify services
- If you delete a service with `pm2 delete`, run `pm2 save` again to update the saved list
- Check startup status: `pm2 startup` (will show if already configured)

### **7. Configure AWS Firewall (Security Groups):**

```bash
# Ensure these ports are open in AWS Lightsail/EC2 console:
# - Port 6060 (TCP) - Video input from client
# - Port 6061 (TCP) - HLS output (HTTP)
# - Port 5000 (TCP) - Sensor packet testing
# - Port 22 (TCP) - SSH access
```

### **8. Verify Services are Running:**

```bash
# Check if ports are listening
sudo netstat -tulpn | grep -E '6060|6061|5000'

# Expected output:
# tcp        0      0 0.0.0.0:6060      0.0.0.0:*      LISTEN      xxxxx/node
# tcp        0      0 0.0.0.0:6061      0.0.0.0:*      LISTEN      xxxxx/node
# tcp        0      0 0.0.0.0:5000      0.0.0.0:*      LISTEN      xxxxx/node

# Test relay health endpoint
curl http://localhost:6061/health
# Should return: {"status":"healthy"}

# View logs
pm2 logs stream-relay
pm2 logs sensor-listener
```

### **9. Test from External Machine:**

```bash
# From your laptop (replace with your AWS IP):
curl http://13.212.221.200:6061/health

# Test video port connectivity
nc -zv 13.212.221.200 6060

# Test sensor port connectivity
nc -zv 13.212.221.200 5000
```

### **Useful PM2 Commands:**

```bash
# View all processes
pm2 list

# View logs
pm2 logs                    # All logs
pm2 logs stream-relay       # Specific service
pm2 logs --lines 100        # Last 100 lines

# Restart services
pm2 restart stream-relay
pm2 restart sensor-listener
pm2 restart all

# Stop services
pm2 stop stream-relay
pm2 stop sensor-listener
pm2 stop all

# Delete services
pm2 delete stream-relay
pm2 delete sensor-listener

# Monitor resources
pm2 monit

# Flush logs
pm2 flush
```

---

## 🎥 Using the Application

### **1. Video Streaming Test**

1. **Open browser:** http://localhost:3000
2. **Navigate to:** "Streaming" page
3. **Click:** "🚀 Start AWS Stream"
4. **Wait:** 5-10 seconds for video to appear

**Expected Backend Output:**
```
🎥 Starting video stream to AWS: 13.212.221.200:6060
🔍 Scanning for video capture devices...
📹 Checking /dev/video0...
   ⏭️ Skipped /dev/video0 (metadata)
📹 Checking /dev/video1...
   ✓ Found valid format: mjpeg
✅ Found valid capture device: /dev/video1
📹 Using video device: /dev/video1
🔍 Camera supports: mjpeg at 18 FPS
📹 Best format: mjpeg at 18 FPS
✅ Video streaming started successfully
[Metrics] FPS: 18.0 | Bitrate: 1850.2 kbps | Dropped: 0
```

**Expected Frontend:**
- Video player shows your webcam feed
- 🔴 LIVE badge visible
- Real-time metrics updating (FPS, bitrate, latency)

### **2. Sensor Packet Testing**

1. **Navigate to:** "Sensor" page
2. **Configure:** Packets per second (1-100)
3. **Click:** "Start Transmission"
4. **Monitor:** Real-time packet statistics

**Metrics Displayed:**
- Packets sent/received/lost
- Success rate percentage
- Average latency
- Live packet feed

---

## 🛑 Stopping the Application

### **If using start.sh:**
```bash
# Press Ctrl+C in the terminal running start.sh
# Both backend and frontend will stop automatically
```

### **If running manually:**
```bash
# Stop backend: Press Ctrl+C in backend terminal
# Stop frontend: Press Ctrl+C in frontend terminal
```

### **Stop AWS Relay:**
```bash
# SSH into AWS
ssh ubuntu@13.212.221.200

# Stop PM2 process
pm2 stop video-relay

# Or stop all PM2 processes
pm2 stop all
```

---

## 🔧 Troubleshooting

### ❌ **"No valid video capture device found!"**

**Cause:** Camera not detected or permission issues

**Fix:**
```bash
# Check camera devices
ls -la /dev/video*

# Check permissions
groups  # Should include 'video'

# Add to video group if missing
sudo usermod -a -G video $USER
# Logout and login

# Test camera manually
ffmpeg -f v4l2 -list_formats all -i /dev/video1
```

---

### ❌ **"Connection to tcp://13.212.221.200:6060 failed"**

**Cause:** AWS relay not running or firewall blocking

**Fix:**
```bash
# Check AWS relay status
ssh ubuntu@13.212.221.200
pm2 status

# Restart relay if needed
pm2 restart video-relay

# Check logs
pm2 logs video-relay

# Test connectivity
nc -zv 13.212.221.200 6060
```

---

### ❌ **"FFmpeg not found" or "mptcpize not found"**

**Cause:** System dependencies not installed

**Fix:**
```bash
# Install missing dependencies
sudo apt update
sudo apt install ffmpeg mptcpize

# Verify installation
ffmpeg -version
mptcpize --version
```

---

### ❌ **Backend won't start - port 8080 in use**

**Fix:**
```bash
# Linux/Mac: Find process using port 8080
lsof -i :8080

# Kill the process (replace XXXX with PID)
kill -9 XXXX

# Or use different port in application.properties
```

---

### ❌ **Frontend won't start - port 3000 in use**

**Fix:**
```bash
# Linux/Mac: Find process using port 3000
lsof -i :3000

# Kill the process
kill -9 XXXX

# Or let React use a different port (it will prompt)
```

---

### ❌ **Video buffering but not playing**

**Cause:** HLS files not being created on AWS

**Fix:**
```bash
# SSH into AWS
ssh ubuntu@13.212.221.200

# Check if relay is creating files
pm2 logs video-relay

# Test HLS endpoint
curl http://13.212.221.200:6061/stream.m3u8

# Should return playlist content, not 404
```

---

### ❌ **"Module not found" errors in frontend**

**Cause:** npm dependencies not installed

**Fix:**
```bash
cd frontend
npm install
npm start
```

---

### ❌ **Maven build fails**

**Cause:** Wrong Java version or corrupted dependencies

**Fix:**
```bash
# Check Java version
java -version  # Should be 17+

# Clean Maven cache
cd backend
mvn clean
rm -rf ~/.m2/repository

# Rebuild
mvn clean install
```

---

## 📊 Architecture Overview

```
┌─────────────────┐
│  Your Camera    │  (Linux: /dev/video1)
└────────┬────────┘
         │ FFmpeg captures (MPTCP)
         ▼
┌─────────────────────────┐
│  Spring Boot Backend    │
│  localhost:8080         │
│  VideoStreamingService  │
└────────┬────────────────┘
         │ TCP stream (MPEG-TS + H.264)
         │ Port 6060 (MPTCP)
         ▼
┌─────────────────────────┐
│  AWS Server             │
│  13.212.221.200:6060    │
│  relay.js (Node.js)     │
└────────┬────────────────┘
         │ Converts to HLS
         │ (FFmpeg: MPEG-TS → HLS)
         ▼
┌─────────────────────────┐
│  HTTP Server            │
│  13.212.221.200:6061    │
│  Serves stream.m3u8     │
└────────┬────────────────┘
         │ HTTP requests
         │ playlist.m3u8 + .ts segments
         ▼
┌─────────────────────────┐
│  React Frontend         │
│  localhost:3000         │
│  HLS.js player          │
└─────────────────────────┘
```

**Key Technologies:**
- **MPTCP:** Multipath TCP for reliability
- **FFmpeg:** Video capture and encoding
- **H.264:** Video compression codec
- **HLS:** HTTP Live Streaming protocol
- **WebSocket:** Real-time metrics updates

---

## 📁 Project Structure

```
Multi-RAT-Web-Application/
├── setup.sh                    # Install dependencies
├── start.sh                    # Launch application
├── QUICK_START.md             # This file
│
├── backend/                    # Java Spring Boot
│   ├── pom.xml                # Maven dependencies
│   └── src/main/java/com/example/demo/
│       ├── controller/
│       │   ├── StreamingController.java    # Video API
│       │   └── SensorController.java       # Sensor API
│       ├── service/
│       │   ├── VideoStreamingService.java  # FFmpeg control
│       │   └── TcpPacketSenderService.java # Packet sender
│       └── websocket/
│           └── StreamingWebSocketHandler.java
│
├── frontend/                   # React Application
│   ├── package.json           # npm dependencies
│   ├── src/
│   │   ├── pages/
│   │   │   ├── HomePage.js
│   │   │   ├── StreamingPage.js    # Video player
│   │   │   └── SensorPage.js       # Packet testing
│   │   ├── components/
│   │   │   ├── VideoPlayer.js      # HLS.js integration
│   │   │   └── MetricsDisplay.js   # Real-time stats
│   │   └── hooks/
│   │       └── useStreamingWebSocket.js
│
└── relay.js                    # AWS relay server (Node.js)
```

---

## 📝 Useful Commands

### **View Logs:**
```bash
# Backend logs (if using start.sh)
tail -f backend.log

# Frontend logs
tail -f frontend.log

# AWS relay logs
ssh ubuntu@13.212.221.200
pm2 logs video-relay
```

### **Check Status:**
```bash
# Backend health
curl http://localhost:8080/actuator/health

# AWS relay health
curl http://13.212.221.200:6061/health

# Camera devices
ls -la /dev/video*
v4l2-ctl --list-devices

# Network connectivity
nc -zv 13.212.221.200 6060
nc -zv 13.212.221.200 6061
```

### **MPTCP Testing:**
```bash
# Check current MPTCP scheduler
sysctl net.mptcp.scheduler

# Change scheduler (root required)
sudo sysctl -w net.mptcp.scheduler=lrtt      # Low RTT First
sudo sysctl -w net.mptcp.scheduler=newLRTT   # New LRTT
sudo sysctl -w net.mptcp.scheduler=clientportsched  # Client Port
```

---

## 💡 Tips for New Contributors

1. **Run `./setup.sh` first** - Installs all dependencies automatically
2. **Use `./start.sh`** - Launches both frontend and backend
3. **Check logs** - Use `tail -f backend.log` and `tail -f frontend.log`
4. **Camera issues?** - Make sure you're in the `video` group
5. **AWS relay down?** - Check with `curl http://13.212.221.200:6061/health`
6. **Browser console** - F12 → Console for frontend errors
7. **Port conflicts?** - Use `lsof -i :8080` and `lsof -i :3000`

---

## 🎯 Next Steps

After getting the application running:

1. **Test video streaming** - Streaming page
2. **Test packet transmission** - Sensor page
3. **Monitor metrics** - Real-time FPS, bitrate, latency
4. **Compare MPTCP schedulers** - Test different schedulers
5. **Multi-interface testing** - Enable multiple network connections

---

## 📚 Additional Documentation

- **README.md** - Project overview
- **relay.js** - AWS server implementation
- **Backend API docs** - Check controller files

---

## 🤝 Contributing

When setting up for development:

1. Fork the repository
2. Clone your fork
3. Run `./setup.sh`
4. Make your changes
5. Test with `./start.sh`
6. Submit a pull request

---

**Last Updated:** November 13, 2025
**AWS Server:** 13.212.221.200
