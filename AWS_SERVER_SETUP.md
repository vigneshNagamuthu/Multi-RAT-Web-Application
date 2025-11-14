# ☁️ AWS Server Setup Guide - Multi-RAT Web Application

This guide covers setting up the **AWS server** that receives video streams and sensor data from clients, converts video to HLS format, and echoes back sensor packets.

---

## 📋 Prerequisites

### Server Requirements

- **Instance Type:** AWS Lightsail or EC2
- **OS:** Ubuntu 20.04+ or Debian 11+
- **RAM:** 2GB minimum (4GB recommended)
- **Storage:** 20GB minimum
- **Network:** Public IP with open ports

### Required Ports

Configure **Security Groups / Firewall Rules** to allow:

| Port | Protocol | Purpose | Direction |
|------|----------|---------|-----------|
| 22 | TCP | SSH access | Inbound |
| 5000 | TCP | Sensor packet testing | Inbound |
| 6060 | TCP | Video stream input | Inbound |
| 6061 | TCP | HLS output (HTTP) | Inbound |

---

## 🔧 Initial Server Configuration

### 1. Connect to Server

```bash
# SSH into your AWS instance
ssh admin@13.212.221.200

# Or if using a key file:
ssh -i your-key.pem admin@13.212.221.200
```

### 2. Update System

```bash
# Update package list
sudo apt update && sudo apt upgrade -y

# Install basic utilities
sudo apt install -y curl wget git net-tools
```

---

## 📦 Install Dependencies

### 1. Install Node.js and npm

```bash
# Install Node.js (v18 LTS recommended)
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Verify installation
node -v    # Should be v18.x or higher
npm -v
```

### 2. Install FFmpeg

```bash
# Install FFmpeg
sudo apt install -y ffmpeg

# Verify installation
ffmpeg -version
```

### 3. Install MPTCP Tools (REQUIRED)

```bash
# Install mptcpize and socat
sudo apt install -y mptcpize socat

# Verify installation
mptcpize --version
socat -V
```

### 4. Install PM2 (Process Manager)

```bash
# Install PM2 globally
sudo npm install -g pm2

# Verify installation
pm2 --version
```

---

## 🌐 Enable MPTCP

MPTCP must be enabled for the server to accept MPTCP connections from clients.

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

# Set scheduler (optional)
sudo sysctl -w net.mptcp.scheduler=default
```

---

## 📁 Setup Project Structure

### 1. Create Directories

```bash
cd ~

# Create project directories
mkdir -p stream-relay
mkdir -p sensor-listener

# Expected structure:
# ~/
# ├── stream-relay/
# │   ├── relay.js
# │   ├── start-relay.sh
# │   ├── package.json
# │   └── hls/ (created automatically)
# └── sensor-listener/
#     ├── sensor-listener.js
#     └── start-sensor.sh
```

### 2. Create HLS Directory

```bash
# Create directory for HLS segments
mkdir -p ~/stream-relay/hls

# Set permissions
chmod 755 ~/stream-relay/hls
```

---

## 📤 Upload Application Files

### From Your Local Machine:

```bash
# Upload relay.js (video streaming server)
scp relay.js admin@13.212.221.200:~/stream-relay/

# Upload sensor-listener.js (sensor echo server)
scp sensor-listener.js admin@13.212.221.200:~/sensor-listener/

# Or use git:
ssh admin@13.212.221.200
cd ~
git clone <your-repo-url>
cp <repo>/relay.js ~/stream-relay/
cp <repo>/sensor-listener.js ~/sensor-listener/
```

---

## 🚀 Configure Services

### 1. Create Video Relay Wrapper (MPTCP)

```bash
cd ~/stream-relay

# Create package.json
cat > package.json << 'EOF'
{
  "name": "stream-relay",
  "version": "1.0.0",
  "description": "HLS video relay server with MPTCP",
  "main": "relay.js",
  "dependencies": {},
  "scripts": {
    "start": "node relay.js"
  }
}
EOF

# Create MPTCP wrapper script
cat > start-relay.sh << 'EOF'
#!/bin/bash
exec mptcpize run node relay.js
EOF

# Make executable
chmod +x start-relay.sh

# Verify file
cat start-relay.sh
```

### 2. Create Sensor Listener Wrapper (MPTCP)

```bash
cd ~/sensor-listener

# Create MPTCP wrapper script
cat > start-sensor.sh << 'EOF'
#!/bin/bash
exec mptcpize run node sensor-listener.js
EOF

# Make executable
chmod +x start-sensor.sh

# Verify file
cat start-sensor.sh
```

---

## 🎮 Start Services with PM2

### 1. Start Video Relay

```bash
cd ~/stream-relay

# Start with PM2 (using MPTCP wrapper)
pm2 start ./start-relay.sh --name stream-relay --log stream.log

# Check status
pm2 status
```

### 2. Start Sensor Listener

```bash
cd ~/sensor-listener

# Start with PM2 (using MPTCP wrapper)
pm2 start ./start-sensor.sh --name sensor-listener --log sensor.log

# Check status
pm2 status
```

### Expected Output:

```
┌─────┬──────────────────┬─────────┬─────────┬──────────┐
│ id  │ name             │ status  │ restart │ uptime   │
├─────┼──────────────────┼─────────┼─────────┼──────────┤
│ 0   │ stream-relay     │ online  │ 0       │ 5s       │
│ 1   │ sensor-listener  │ online  │ 0       │ 3s       │
└─────┴──────────────────┴─────────┴─────────┴──────────┘
```

---

## 🔄 Configure Auto-Start on Reboot

Ensure services automatically restart after server reboot.

```bash
# Step 1: Generate startup script
pm2 startup

# This outputs a command like:
# sudo env PATH=$PATH:/usr/bin /usr/lib/node_modules/pm2/bin/pm2 startup systemd -u admin --hp /home/admin

# Step 2: Copy and run the command (example):
sudo env PATH=$PATH:/usr/bin /usr/lib/node_modules/pm2/bin/pm2 startup systemd -u admin --hp /home/admin

# Step 3: Save current process list
pm2 save

# Step 4: Verify startup is enabled
sudo systemctl status pm2-admin
# Should show: Active: active (running)
```

**Important:** Run `pm2 save` every time you add/remove/modify services!

---

## ✅ Verify Services

### 1. Check Ports are Listening

```bash
# Check if services are listening
sudo netstat -tulpn | grep -E '5000|6060|6061'

# Expected output:
# tcp  0  0  0.0.0.0:5000  0.0.0.0:*  LISTEN  xxxxx/node
# tcp  0  0  0.0.0.0:6060  0.0.0.0:*  LISTEN  xxxxx/node
# tcp  0  0  0.0.0.0:6061  0.0.0.0:*  LISTEN  xxxxx/node
```

### 2. Verify MPTCP Wrappers

```bash
# Check processes are running with mptcpize
ps aux | grep mptcpize

# Should show:
# mptcpize run node /home/admin/sensor-listener/sensor-listener.js
# mptcpize run node /home/admin/stream-relay/relay.js
```

### 3. Test Health Endpoints

```bash
# Test relay health
curl http://localhost:6061/health
# Should return: {"status":"healthy"}

# Test from external machine:
curl http://13.212.221.200:6061/health
```

### 4. Test Port Connectivity

```bash
# From your laptop:
nc -zv 13.212.221.200 5000  # Sensor port
nc -zv 13.212.221.200 6060  # Video input port
nc -zv 13.212.221.200 6061  # HLS output port
```

---

## 🧪 Test MPTCP

### From Client Machine:

```bash
# Install iperf3 on client
sudo apt install iperf3

# Test MPTCP connection
mptcpize run iperf3 -c 13.212.221.200 -p 5000 --cport 5000 -t 10
```

### On AWS Server (during test):

```bash
# Monitor MPTCP connections
watch -n 1 'ss -M | grep 5000'

# If MPTCP is working, you'll see:
# tcp   ESTAB  0  0  <client-ip>:5000  0.0.0.0:*
#       mptcp subflows:1 add_addr_signal:0 ...
```

---

## 📊 Monitoring & Logs

### View PM2 Logs

```bash
# View all logs
pm2 logs

# View specific service
pm2 logs stream-relay
pm2 logs sensor-listener

# View last 100 lines
pm2 logs --lines 100

# Follow logs in real-time
pm2 logs --lines 0
```

### Monitor Resource Usage

```bash
# PM2 monitoring dashboard
pm2 monit

# System resources
htop  # or: sudo apt install htop
```

### Check MPTCP Connections

```bash
# View all MPTCP connections
ss -M

# Filter by port
ss -M | grep -E '5000|6060'

# Watch in real-time
watch -n 1 'ss -M | grep -E "5000|6060"'
```

---

## 🛠️ PM2 Management Commands

### Basic Commands

```bash
# List all processes
pm2 list

# Start a service
pm2 start stream-relay

# Stop a service
pm2 stop stream-relay

# Restart a service
pm2 restart stream-relay

# Delete a service
pm2 delete stream-relay

# Restart all
pm2 restart all

# Stop all
pm2 stop all
```

### Maintenance Commands

```bash
# Save process list
pm2 save

# Resurrect saved processes
pm2 resurrect

# Flush logs
pm2 flush

# Update PM2
npm install -g pm2
pm2 update
```

### Reset PM2 (if issues occur)

```bash
# Method 1: Delete all and save (recommended)
pm2 delete all
pm2 save --force
pm2 list  # Should show empty

# Method 2: Nuclear option (if duplicates persist)
pm2 kill
rm -rf ~/.pm2
pm2 list  # Should show empty

# Then recreate services:
cd ~/sensor-listener
pm2 start ./start-sensor.sh --name sensor-listener

cd ~/stream-relay
pm2 start ./start-relay.sh --name stream-relay

pm2 save
pm2 list  # Should show only 2 services
```

---

## 🔐 Security Considerations

### 1. Firewall Configuration

```bash
# If using UFW (Ubuntu Firewall)
sudo ufw status

# Allow required ports
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 5000/tcp  # Sensor
sudo ufw allow 6060/tcp  # Video input
sudo ufw allow 6061/tcp  # HLS output

# Enable firewall
sudo ufw enable
```

### 2. Fail2ban (SSH Protection)

```bash
# Install fail2ban
sudo apt install -y fail2ban

# Start service
sudo systemctl start fail2ban
sudo systemctl enable fail2ban
```

### 3. Regular Updates

```bash
# Update system regularly
sudo apt update && sudo apt upgrade -y

# Update Node.js packages
cd ~/stream-relay && npm update
```

---

## 🐛 Troubleshooting

### Services Not Starting

**Check logs:**
```bash
pm2 logs stream-relay --lines 50
pm2 logs sensor-listener --lines 50
```

**Common issues:**
- Port already in use: `lsof -i :6060`
- Missing files: Check `relay.js` and `sensor-listener.js` exist
- Permission errors: Ensure scripts are executable (`chmod +x`)

---

### MPTCP Not Working

**Check if enabled:**
```bash
sysctl net.mptcp.enabled
# Should be 1
```

**Verify wrappers:**
```bash
ps aux | grep mptcpize
# Should show both services
```

**Re-wrap services:**
```bash
pm2 stop all
pm2 delete all

# Recreate wrapper scripts (see Configuration section)
# Restart services
```

---

### Duplicate PM2 Services

**Clean up:**
```bash
pm2 list
pm2 delete <id>  # Delete duplicates by ID

# Or delete all and start fresh
pm2 delete all
pm2 save --force
```

---

### HLS Files Not Creating

**Check directory permissions:**
```bash
ls -la ~/stream-relay/hls/
chmod 755 ~/stream-relay/hls/
```

**Check FFmpeg errors:**
```bash
pm2 logs stream-relay | grep -i error
```

---

## 📈 Performance Optimization

### Increase File Limits

```bash
# Edit limits
sudo nano /etc/security/limits.conf

# Add:
* soft nofile 65536
* hard nofile 65536

# Logout and login
```

### Optimize Network

```bash
# Increase network buffer sizes
sudo sysctl -w net.core.rmem_max=26214400
sudo sysctl -w net.core.wmem_max=26214400

# Make permanent
echo "net.core.rmem_max=26214400" | sudo tee -a /etc/sysctl.conf
echo "net.core.wmem_max=26214400" | sudo tee -a /etc/sysctl.conf
```

---

## 🧹 Cleanup Old HLS Files

```bash
# Create cleanup script
cat > ~/stream-relay/cleanup-hls.sh << 'EOF'
#!/bin/bash
# Delete HLS files older than 1 hour
find ~/stream-relay/hls/ -type f -name "*.ts" -mmin +60 -delete
find ~/stream-relay/hls/ -type f -name "*.m3u8" -mmin +60 -delete
EOF

chmod +x ~/stream-relay/cleanup-hls.sh

# Add to crontab (run every hour)
crontab -e
# Add line:
# 0 * * * * ~/stream-relay/cleanup-hls.sh
```

---

## 🎯 Post-Setup Checklist

- [ ] MPTCP enabled: `sysctl net.mptcp.enabled` = 1
- [ ] mptcpize installed: `which mptcpize`
- [ ] socat installed: `which socat`
- [ ] Services running: `pm2 status` shows both online
- [ ] MPTCP wrappers active: `ps aux | grep mptcpize`
- [ ] Ports listening: `netstat -tulpn | grep -E '5000|6060|6061'`
- [ ] Auto-start enabled: `pm2 startup` configured
- [ ] Processes saved: `pm2 save` completed
- [ ] Health check passing: `curl http://localhost:6061/health`
- [ ] External access working: Test from client machine
- [ ] MPTCP verified: `ss -M | grep 5000` shows connections

---

## 📚 Related Documentation

- **CLIENT_SETUP.md** - Client configuration guide
- **QUICK_START.md** - Combined quick start guide
- **README.md** - Project overview

---

**Last Updated:** November 14, 2025  
**Server IP:** 13.212.221.200  
**Required Ports:** 5000, 6060, 6061  
**MPTCP Required:** Yes
