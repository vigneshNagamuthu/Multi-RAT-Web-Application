# 💻 Client Setup Guide - Multi-RAT Web Application

This guide covers setting up the **client-side application** (your laptop/development machine) that runs the frontend, backend, and streams video/sensor data to AWS.

---

## 📋 Prerequisites

### System Requirements

- **Operating System:** Linux (Ubuntu/Debian recommended), macOS, or Windows
- **Java:** Version 17 or higher
- **Node.js:** Version 14 or higher
- **RAM:** 4GB minimum (8GB recommended)
- **Camera:** Webcam or integrated camera
- **Network:** Internet connection to AWS server

---

## 🔧 Installation

### 1. Install System Dependencies

#### **Ubuntu/Debian Linux:**

```bash
# Update package list
sudo apt update

# Install all required packages
sudo apt install -y \
    openjdk-17-jdk \
    maven \
    nodejs \
    npm \
    ffmpeg \
    v4l-utils \
    socat \
    mptcpize

# Verify installations
java -version    # Should be 17+
mvn -version
node -v          # Should be 14+
npm -v
ffmpeg -version
socat -V
mptcpize --version
```

#### **macOS:**

```bash
# Install Homebrew if not already installed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install dependencies
brew install openjdk@17 maven node ffmpeg

# Link Java
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

#### **Windows:**

1. **Java 17:** Download from [Adoptium](https://adoptium.net/)
2. **Maven:** Download from [Apache Maven](https://maven.apache.org/download.cgi)
3. **Node.js:** Download from [nodejs.org](https://nodejs.org/)
4. **FFmpeg:** Download from [ffmpeg.org](https://ffmpeg.org/download.html)

---

### 2. Enable MPTCP (Linux Only)

MPTCP (Multipath TCP) is required for all features. This enables simultaneous use of multiple network interfaces.

```bash
# Check if MPTCP is available
sysctl net.mptcp.enabled

# Enable MPTCP temporarily
sudo sysctl -w net.mptcp.enabled=1

# Make permanent (survives reboot)
echo "net.mptcp.enabled=1" | sudo tee -a /etc/sysctl.conf

# Verify enabled
sysctl net.mptcp.enabled
# Should output: net.mptcp.enabled = 1

# Check available schedulers
cat /proc/sys/net/mptcp/available_schedulers

# Set custom scheduler (optional)
sudo sysctl -w net.mptcp.scheduler=clientportsched
```

**What each tool does:**
- **mptcpize:** Wraps applications to use MPTCP instead of regular TCP
- **socat:** Creates TCP proxy with custom source ports (needed for port-based MPTCP scheduler routing)

---

### 3. Configure Camera Permissions (Linux)

```bash
# Add your user to the video group
sudo usermod -a -G video $USER

# Logout and login for changes to take effect
# Or run: newgrp video

# Verify membership
groups
# Should include 'video'

# List available cameras
ls -la /dev/video*
v4l2-ctl --list-devices
```

---

## 🚀 Project Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Multi-RAT-Web-Application
```

### 2. Run Automated Setup

```bash
# Make scripts executable
chmod +x setup.sh start.sh

# Run setup (installs all dependencies)
./setup.sh
```

The setup script will:
- ✅ Check all system prerequisites
- ✅ Install Maven dependencies (backend)
- ✅ Install npm dependencies (frontend)
- ✅ Verify MPTCP is enabled
- ✅ Build the backend JAR file

**Expected output:**
```
✅ All critical system prerequisites found!
✅ Backend dependencies installed successfully
✅ Frontend dependencies installed successfully
✅ Setup Complete!
```

---

## 🎮 Running the Application

### Option 1: Automated Start (Recommended)

```bash
./start.sh
```

This will:
- Start the backend on **http://localhost:8080**
- Start the frontend on **http://localhost:3000**
- Display logs and status
- Check AWS relay connectivity

**Expected output:**
```
✅ Multi-RAT Web Application is Running!

🌐 Access the application:
   Frontend: http://localhost:3000
   Backend:  http://localhost:8080

📊 Available pages:
   Home:      http://localhost:3000/
   Streaming: http://localhost:3000/streaming
   Sensor:    http://localhost:3000/sensor
```

**To stop:** Press `Ctrl+C`

---

### Option 2: Manual Start

If you prefer to run services separately:

#### Terminal 1 - Backend:
```bash
cd backend
mvn spring-boot:run
```

#### Terminal 2 - Frontend:
```bash
cd frontend
npm start
```

---

## 🎥 Using the Application

### 1. Video Streaming

1. Open browser: **http://localhost:3000**
2. Navigate to: **Streaming** page
3. Click: **🚀 Start AWS Stream**
4. Wait 5-10 seconds for video to appear

**What happens:**
- FFmpeg captures your camera
- Video is encoded to H.264
- Streamed via MPTCP to AWS:6060 with **sourceport=6060** (for scheduler routing)
- AWS converts to HLS format
- Frontend plays via HLS.js

**Metrics displayed:**
- Live FPS (frames per second)
- Bitrate (Mbps)
- Latency (ms)
- Dropped frames

---

### 2. Sensor Packet Testing

1. Navigate to: **Sensor** page
2. Configure: **Packets per second** (1-100)
3. Click: **▶️ Start Transmission**
4. Monitor: Real-time statistics

**What happens:**
- Backend sends numbered packets with timestamps
- Uses socat proxy with **sourceport=5000** (for scheduler routing)
- AWS echoes packets back
- Latency is calculated per packet
- Statistics are displayed in real-time

**Metrics displayed:**
- Packets sent/received
- Packet loss rate
- Average/min/max latency
- Delayed packets (>1000ms)

---

## 🔧 Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# AWS server
aws.server.ip=13.212.221.200
aws.video.port=6060
aws.sensor.port=5000
```

### MPTCP Port-Based Routing

The application uses **custom source ports** for MPTCP scheduler routing:

- **Port 5000:** Sensor data → Uses socat proxy with `sourceport=5000`
- **Port 6060:** Video stream → Uses socat proxy with `sourceport=6060`

Your custom MPTCP scheduler can detect these ports and route traffic accordingly.

**How it works:**
```
Java/FFmpeg → localhost:5001/6062 (local socat proxy)
              ↓
              mptcpize + socat with sourceport=5000/6060
              ↓
              AWS:5000/6060 (with correct source port for routing)
```

---

## 🔍 Monitoring & Debugging

### Check MPTCP Connections

```bash
# Monitor MPTCP connections
watch -n 1 'ss -M | grep -E "5000|6060"'

# Should show:
# tcp   ESTAB  0  0  <local-ip>:5000  13.212.221.200:5000
#       mptcp subflows:1 ...
# tcp   ESTAB  0  0  <local-ip>:6060  13.212.221.200:6060
#       mptcp subflows:1 ...
```

### View Logs

```bash
# Backend logs (if using start.sh)
tail -f backend.log

# Frontend logs
tail -f frontend.log

# Follow both simultaneously
tail -f backend.log frontend.log
```

### Check Services

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Frontend running
curl http://localhost:3000

# AWS relay health
curl http://13.212.221.200:6061/health
```

### Check Camera

```bash
# List all video devices
ls -la /dev/video*

# Get device details
v4l2-ctl --list-devices

# Test camera with FFmpeg
ffmpeg -f v4l2 -list_formats all -i /dev/video0

# Test camera capture
ffmpeg -f v4l2 -i /dev/video0 -frames:v 1 test.jpg
```

---

## 🐛 Troubleshooting

### Camera Not Detected

**Error:** `No valid video capture device found!`

**Fix:**
```bash
# Check permissions
groups  # Should include 'video'

# Add to video group
sudo usermod -a -G video $USER
# Logout and login

# List devices
ls -la /dev/video*
v4l2-ctl --list-devices
```

---

### Port Already in Use

**Error:** `Port 8080 already in use`

**Fix:**
```bash
# Find process using port
lsof -i :8080

# Kill the process
kill -9 <PID>
```

---

### MPTCP Not Working

**Error:** No "mptcp" in `ss -M` output

**Fix:**
```bash
# 1. Enable MPTCP
sudo sysctl -w net.mptcp.enabled=1

# 2. Verify tools installed
which socat
which mptcpize

# 3. Rebuild backend
cd backend
mvn clean install

# 4. Restart application
```

---

### Sensor Packets Not Restarting

**Error:** Cannot start transmission after stopping

**Fix:**
```bash
# Kill lingering socat processes
pkill -f 'socat.*5001'
fuser -k 5000/tcp

# Or restart backend
```

---

### Video Streaming Connection Failed

**Error:** `Connection to tcp://13.212.221.200:6060 failed`

**Fix:**
```bash
# 1. Check AWS relay is running
curl http://13.212.221.200:6061/health

# 2. Check network connectivity
nc -zv 13.212.221.200 6060

# 3. Check firewall (AWS Security Groups)
# Ensure port 6060 is open for TCP inbound
```

---

## 📊 Port Reference

| Port | Service | Direction | Protocol |
|------|---------|-----------|----------|
| 8080 | Backend API | Local | HTTP |
| 3000 | Frontend | Local | HTTP |
| 5001 | Sensor MPTCP Proxy | Local | TCP |
| 6062 | Video MPTCP Proxy | Local | TCP |
| 5000 | Sensor Listener | AWS | MPTCP (sourceport=5000) |
| 6060 | Video Relay Input | AWS | MPTCP (sourceport=6060) |
| 6061 | HLS Output | AWS | HTTP |

---

## 🧪 Testing MPTCP

### Test with iperf3

```bash
# Install iperf3
sudo apt install iperf3

# Test MPTCP connection to AWS
mptcpize run iperf3 -c 13.212.221.200 -p 5000 --cport 5000 -t 10

# Monitor MPTCP during test
watch -n 1 'ss -M | grep 5000'
```

### Verify Source Ports

```bash
# Start sensor transmission
# Then check source port:
ss -M | grep 5000
# Should show sourceport = 5000

# Start video streaming
# Then check source port:
ss -M | grep 6060
# Should show sourceport = 6060
```

---

## 🎯 Next Steps

After successful setup:

1. ✅ Test video streaming with different camera settings
2. ✅ Test sensor packet transmission at various rates
3. ✅ Monitor MPTCP metrics with `ss -M`
4. ✅ Try different MPTCP schedulers
5. ✅ Enable multiple network interfaces (WiFi + Ethernet)
6. ✅ Compare performance with/without MPTCP

---

## 📚 Related Documentation

- **AWS_SERVER_SETUP.md** - AWS server configuration
- **QUICK_START.md** - Combined quick start guide
- **README.md** - Project overview

---

**Last Updated:** November 14, 2025  
**Supported OS:** Linux (Ubuntu 20.04+), macOS, Windows  
**MPTCP Required:** Yes (Linux only)
