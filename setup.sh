#!/bin/bash

# =============================================================================
# Multi-RAT Web Application - Setup Script
# =============================================================================
# This script installs all project dependencies for both frontend and backend.
# Run this ONCE after cloning the repository.
#
# Usage: ./setup.sh
# =============================================================================

set -e  # Exit on any error

echo "🚀 Starting Multi-RAT Web Application Setup..."
echo "=============================================="
echo ""

# Check if running from project root
if [ ! -d "backend" ] || [ ! -d "frontend" ] || [ ! -f "setup.sh" ]; then
    echo "❌ Error: Please run this script from the project root directory"
    exit 1
fi

# =============================================================================
# System Prerequisites Check
# =============================================================================

echo "🔍 Checking system prerequisites..."
echo ""

MISSING_DEPS=()

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        echo "✅ Java $JAVA_VERSION detected"
    else
        echo "⚠️  Java $JAVA_VERSION detected (Java 17+ recommended)"
    fi
else
    echo "❌ Java not found"
    MISSING_DEPS+=("Java 17+ (sudo apt install openjdk-17-jdk)")
fi

# Check Maven
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
    echo "✅ Maven $MVN_VERSION detected"
else
    echo "❌ Maven not found"
    MISSING_DEPS+=("Maven (sudo apt install maven)")
fi

# Check Node.js
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v)
    echo "✅ Node.js $NODE_VERSION detected"
else
    echo "❌ Node.js not found"
    MISSING_DEPS+=("Node.js (sudo apt install nodejs)")
fi

# Check npm
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm -v)
    echo "✅ npm $NPM_VERSION detected"
else
    echo "❌ npm not found"
    MISSING_DEPS+=("npm (sudo apt install npm)")
fi

# Check FFmpeg
if command -v ffmpeg &> /dev/null; then
    FFMPEG_VERSION=$(ffmpeg -version | head -n 1 | awk '{print $3}')
    echo "✅ FFmpeg $FFMPEG_VERSION detected"
else
    echo "❌ FFmpeg not found"
    MISSING_DEPS+=("FFmpeg (sudo apt install ffmpeg)")
fi

# Check socat (required for MPTCP sensor packets)
if command -v socat &> /dev/null; then
    echo "✅ socat detected"
else
    echo "❌ socat not found (required for MPTCP sensor testing)"
    MISSING_DEPS+=("socat (sudo apt install socat)")
fi

# Check MPTCP
if command -v mptcpize &> /dev/null; then
    echo "✅ mptcpize detected"
else
    echo "❌ mptcpize not found (required for MPTCP support)"
    MISSING_DEPS+=("mptcpize (sudo apt install mptcpize)")
fi

# Check MPTCP kernel support
if [ -f /proc/sys/net/mptcp/enabled ]; then
    MPTCP_ENABLED=$(cat /proc/sys/net/mptcp/enabled)
    if [ "$MPTCP_ENABLED" = "1" ]; then
        echo "✅ MPTCP enabled in kernel"
    else
        echo "⚠️  MPTCP disabled in kernel"
        echo "   Enable with: sudo sysctl -w net.mptcp.enabled=1"
    fi
else
    echo "⚠️  MPTCP not supported by kernel"
fi

# Check v4l2-ctl (optional)
if command -v v4l2-ctl &> /dev/null; then
    echo "✅ v4l2-ctl detected"
else
    echo "⚠️  v4l2-ctl not found (optional for camera detection)"
    echo "   Install with: sudo apt install v4l-utils"
fi

echo ""

# If critical dependencies are missing, exit
if [ ${#MISSING_DEPS[@]} -gt 0 ]; then
    echo "❌ Missing critical dependencies:"
    for dep in "${MISSING_DEPS[@]}"; do
        echo "   - $dep"
    done
    echo ""
    echo "💡 Install missing dependencies and run this script again."
    exit 1
fi

echo "✅ All critical system prerequisites found!"
echo ""

# =============================================================================
# Backend Setup (Maven)
# =============================================================================

echo "📦 Setting up Backend (Maven)..."
echo "-------------------------------------------"
cd backend

echo "🔨 Running: mvn clean install"
if mvn clean install -DskipTests; then
    echo "✅ Backend dependencies installed successfully"
else
    echo "❌ Backend setup failed"
    exit 1
fi

cd ..
echo ""

# =============================================================================
# Frontend Setup (npm)
# =============================================================================

echo "📦 Setting up Frontend (npm)..."
echo "-------------------------------------------"
cd frontend

echo "🔨 Running: npm install"
if npm install; then
    echo "✅ Frontend dependencies installed successfully"
else
    echo "❌ Frontend setup failed"
    exit 1
fi

cd ..
echo ""

# =============================================================================
# Setup Complete
# =============================================================================

echo "=============================================="
echo "✅ Setup Complete!"
echo "=============================================="
echo ""
echo "📝 Next steps:"
echo "   1. Start the application: ./start.sh"
echo "   2. Backend will run on: http://localhost:8080"
echo "   3. Frontend will run on: http://localhost:3000"
echo ""
echo "📚 For more information, see QUICK_START.md"
echo ""
