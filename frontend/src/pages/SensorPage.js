import { useState } from 'react';
import PacketLossVisualizer from '../components/sensor/PacketLossVisualizer';
import useSensorWebSocket from '../hooks/useSensorWebSocket';
import './SensorPage.css';

export default function SensorPage() {
  const { packets, isConnected, error, clearPackets } = useSensorWebSocket('ws://localhost:8080/ws/sensor');
  const [iperfRunning, setIperfRunning] = useState(false);
  const [iperfServer, setIperfServer] = useState('127.0.0.1');
  const [loading, setLoading] = useState(false);

  const handleReset = async () => {
    try {
      await fetch('http://localhost:8080/api/sensor/reset', { method: 'POST' });
      clearPackets();
    } catch (err) {
      console.error('Error resetting sequence:', err);
    }
  };

  const handleStartIPerf = async () => {
    setLoading(true);
    try {
      const response = await fetch(
        `http://localhost:8080/api/sensor/iperf/start?server=${iperfServer}&port=5000&duration=60`,
        { method: 'POST' }
      );
      const data = await response.json();
      
      if (data.status === 'success') {
        setIperfRunning(true);
        alert(`✅ iperf3 started!\nServer: ${iperfServer}\nPort: 5000\nDuration: 60s`);
      } else {
        alert(`❌ Failed to start iperf3:\n${data.message}`);
      }
    } catch (err) {
      console.error('Error starting iperf3:', err);
      alert('❌ Error starting iperf3. Make sure iperf3 is installed and backend is running.');
    } finally {
      setLoading(false);
    }
  };

  const handleStopIPerf = async () => {
    setLoading(true);
    try {
      await fetch('http://localhost:8080/api/sensor/iperf/stop', { method: 'POST' });
      setIperfRunning(false);
      alert('🛑 iperf3 stopped');
    } catch (err) {
      console.error('Error stopping iperf3:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="sensor-page">
      <div className="sensor-container">
        {/* Header */}
        <div className="sensor-header">
          <div className="header-content">
            <div className="header-left">
              <h2>📡 IoT Sensor Data</h2>
              <p>Port 5000 - Redundant Scheduler (High Reliability)</p>
            </div>
            <div className="connection-status">
              <div className="connection-status-label">Connection Status</div>
              <div className="status-indicator">
                <span className={`status-dot ${isConnected ? 'connected' : 'disconnected'}`}></span>
                <span>{isConnected ? 'Connected' : 'Disconnected'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* iperf3 Controls */}
        <div className="iperf-controls">
          <div className="iperf-input-group">
            <label htmlFor="server-ip">iperf3 Server IP:</label>
            <input
              id="server-ip"
              type="text"
              value={iperfServer}
              onChange={(e) => setIperfServer(e.target.value)}
              placeholder="e.g., 192.168.1.100"
              disabled={iperfRunning}
              className="server-input"
            />
          </div>
          
          <div className="iperf-buttons">
            <button
              onClick={handleStartIPerf}
              disabled={iperfRunning || loading}
              className="btn-iperf-start"
            >
              {loading ? '⏳ Starting...' : '🚀 Start iperf3 Traffic'}
            </button>
            
            <button
              onClick={handleStopIPerf}
              disabled={!iperfRunning || loading}
              className="btn-iperf-stop"
            >
              {loading ? '⏳ Stopping...' : '🛑 Stop iperf3 Traffic'}
            </button>
          </div>
          
          {iperfRunning && (
            <div className="iperf-status-banner">
              <span className="status-pulse"></span>
              <span>🔴 iperf3 traffic active on port 5000</span>
            </div>
          )}
        </div>

        {/* Packet Controls */}
        <div className="sensor-controls">
          <button onClick={clearPackets} className="btn-secondary">
            🗑️ Clear Packets
          </button>
          
          <button onClick={handleReset} className="btn-primary">
            🔄 Reset Sequence
          </button>
        </div>

        {/* Packet Visualizer */}
        <div className="visualizer-card">
          <PacketLossVisualizer packets={packets} isConnected={isConnected} error={error} />
        </div>

        {/* Info Banner */}
        <div className="info-banner">
          <strong>ℹ️ Note:</strong> This page demonstrates IoT sensor data transmission using the Redundant scheduler. 
          Click "Start iperf3 Traffic" to send real network traffic through MPTCP on port 5000.
        </div>
      </div>
    </div>
  );
}