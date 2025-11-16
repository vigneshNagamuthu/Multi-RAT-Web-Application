#!/bin/bash

# =============================================================================
# Multi-RAT Web Application - Start Script
# =============================================================================
# This script starts both the backend (Spring Boot) and frontend (React)
# servers concurrently.
#
# Usage: ./start.sh
#
# Prerequisites:
#   - Run ./setup.sh first to install dependencies
#   - AWS relay server should be running on 13.212.221.200
#   - MPTCP configuration: Run python3 ~/Desktop/mptcp_quick_setup.py first
# =============================================================================

set -e  # Exit on any error

echo "🚀 Starting Multi-RAT Web Application..."
echo "=============================================="
echo ""

# Check if running from project root
if [ ! -d "backend" ] || [ ! -d "frontend" ]; then
    echo "❌ Error: Please run this script from the project root directory"
    exit 1
fi

# Check if dependencies are installed
if [ ! -d "frontend/node_modules" ]; then
    echo "❌ Error: Frontend dependencies not installed"
    echo "💡 Run ./setup.sh first"
    exit 1
fi

if [ ! -f "backend/target/demo-0.0.1-SNAPSHOT.jar" ]; then
    echo "⚠️  Backend not built yet. Running setup..."
    cd backend
    mvn clean install -DskipTests
    cd ..
fi

# =============================================================================
# MPTCP Configuration Check
# =============================================================================

echo "🔧 Checking MPTCP configuration..."
MPTCP_ENDPOINTS=$(ip mptcp endpoint show 2>/dev/null | wc -l)

if [ "$MPTCP_ENDPOINTS" -lt 2 ]; then
    echo "⚠️  MPTCP endpoints not configured (found $MPTCP_ENDPOINTS endpoints)"
    echo "💡 Configuring MPTCP now..."
    
    if [ -f "$HOME/Desktop/mptcp_quick_setup.py" ]; then
        python3 "$HOME/Desktop/mptcp_quick_setup.py"
        echo "✅ MPTCP configured"
    else
        echo "❌ Warning: mptcp_quick_setup.py not found at ~/Desktop/"
        echo "   MPTCP may not work properly"
    fi
else
    echo "✅ MPTCP already configured ($MPTCP_ENDPOINTS endpoints)"
fi
echo ""

# =============================================================================
# Cleanup function (called on Ctrl+C)
# =============================================================================

cleanup() {
    echo ""
    echo "🛑 Shutting down servers..."
    
    # Kill background processes
    if [ ! -z "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null || true
    fi
    if [ ! -z "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null || true
    fi
    
    # Kill any remaining Java/Node processes started by this script
    pkill -P $$ 2>/dev/null || true
    
    echo "✅ Servers stopped"
    exit 0
}

# Register cleanup function for Ctrl+C
trap cleanup SIGINT SIGTERM

# =============================================================================
# Start Backend (Spring Boot)
# =============================================================================

echo "📦 Starting Backend Server..."
echo "-------------------------------------------"
cd backend

# Start backend with MPTCP support in background
echo "   Using mptcpize to enable MPTCP at OS level"
echo "   ⚠️  Note: 'Reconfigure MPTCP' will restart this backend"

mptcpize run java -jar target/demo-0.0.1-SNAPSHOT.jar > ../backend.log 2>&1 &
BACKEND_PID=$!

cd ..

echo "✅ Backend starting... (PID: $BACKEND_PID)"
echo "   Logs: tail -f backend.log"
echo "   URL: http://localhost:8080"
echo ""

# Wait for backend to start (check for port 8080)
echo "⏳ Waiting for backend to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1 || \
       nc -z localhost 8080 > /dev/null 2>&1; then
        echo "✅ Backend is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "⚠️  Backend taking longer than expected (still starting...)"
    fi
    sleep 1
done
echo ""

# =============================================================================
# Start Frontend (React)
# =============================================================================

echo "📦 Starting Frontend Server..."
echo "-------------------------------------------"
cd frontend

# Start frontend in background
npm start > ../frontend.log 2>&1 &
FRONTEND_PID=$!

cd ..

echo "✅ Frontend starting... (PID: $FRONTEND_PID)"
echo "   Logs: tail -f frontend.log"
echo "   URL: http://localhost:3000"
echo ""

# Wait for frontend to start (check for port 3000)
echo "⏳ Waiting for frontend to be ready..."
for i in {1..60}; do
    if nc -z localhost 3000 > /dev/null 2>&1; then
        echo "✅ Frontend is ready!"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "⚠️  Frontend taking longer than expected (still starting...)"
    fi
    sleep 1
done
echo ""

# =============================================================================
# Application Running
# =============================================================================

echo "=============================================="
echo "✅ Multi-RAT Web Application is Running!"
echo "=============================================="
echo ""
echo "🌐 Access the application:"
echo "   Frontend: http://localhost:3000"
echo "   Backend:  http://localhost:8080"
echo ""
echo "📊 Available pages:"
echo "   Home:      http://localhost:3000/"
echo "   Streaming: http://localhost:3000/streaming"
echo "   Sensor:    http://localhost:3000/sensor"
echo ""
echo "📝 Logs:"
echo "   Backend:  tail -f backend.log"
echo "   Frontend: tail -f frontend.log"
echo ""
echo "⚙️  MPTCP Status:"
echo "   Endpoints: $(ip mptcp endpoint show | wc -l)"
echo "   Scheduler: $(sysctl -n net.mptcp.scheduler 2>/dev/null || echo 'unknown')"
echo ""
echo "⚠️  Important Notes:"
echo "   • Pressing 'Reconfigure MPTCP' in the UI will restart the backend"
echo "   • If backend stops, you'll need to restart this script"
echo "   • After network changes, use 'Reconfigure MPTCP' button in UI"
echo ""
echo "🛑 Press Ctrl+C to stop both servers"
echo ""

# Keep script running and wait for processes
wait $BACKEND_PID $FRONTEND_PID