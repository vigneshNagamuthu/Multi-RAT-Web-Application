import { useState } from 'react';
import PacketLossVisualizer from '../components/sensor/PacketLossVisualizer';
import useSensorWebSocket from '../hooks/useSensorWebSocket';
import './SensorPage.css';

export default function SensorPage() {
  const { packets, isConnected, error, clearPackets } = useSensorWebSocket('ws://localhost:8080/ws/sensor');
  const [iperfRunning, setIperfRunning] = useState(false);
  const [loading, setLoading] = useState(false);

  // Server configuration
  const serverInfo = {
    server: '13.212.221.200',
    port: 5000  // Sensor data port
  };

  const handleReset = async () => {
    try {
      await fetch('http://localhost:8080/api/sensor/reset', { method: 'POST' });
      clearPackets();
      console.log('✅ Sequence reset');
    } catch (err) {
      console.error('Error resetting sequence:', err);
    }
  };

  const handleStartIPerf = async () => {
    setLoading(true);
    try {
      const response = await fetch(
        'http://localhost:8080/api/sensor/iperf/start?duration=60',
        { method: 'POST' }
      );
      const data = await response.json();
      
      if (data.status === 'success') {
        setIperfRunning(true);
        alert(
          `✅ iperf3 Started!\n\n` +
          `📤 Sending to AWS: ${serverInfo.server}:${serverInfo.port}\n` +
          `📡 Protocol: TCP (Redundant Scheduler)\n` +
          `⏱️ Duration: 60 seconds\n\n` +
          `Packets will appear in real-time below...`
        );
      } else {
        alert(
          `❌ Failed to Start iperf3\n\n` +
          `${data.message}\n\n` +
          `Requirements:\n` +
          `• iperf3 installed on your computer\n` +
          `• AWS server running (${serverInfo.server})`
        );
      }
    } catch (err) {
      console.error('Error starting iperf3:', err);
      alert('❌ Error connecting to backend. Make sure backend is running on localhost:8080');
    } finally {
      setLoading(false);
    }
  };

  const handleStopIPerf = async () => {
    setLoading(true);
    try {
      await fetch('http://localhost:8080/api/sensor/iperf/stop', { method: 'POST' });
      setIperfRunning(false);
      alert('🛑 iperf3 stopped successfully');
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
        <div className="page-header">
          <div className="header-content">
            <div className="header-left">
              <h2>📡 IoT Sensor Data</h2>
              <p>Port {serverInfo.port} - Redundant Scheduler (High Reliability)</p>
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

        {/* Content Grid */}
        <div className="content-grid-full">
          {/* Main Section */}
          <div className="main-section">
            {/* Packet Visualizer Card */}
            <div className="visualizer-card">
              <PacketLossVisualizer packets={packets} isConnected={isConnected} error={error} />
            </div>

            {/* AWS Control Card */}
            <div className="aws-control-card">
              <div className="card-header">
                <div className="header-icon-section">
                  <span className="header-icon">☁️</span>
                  <div>
                    <h3 className="card-title">AWS Lightsail Server</h3>
                    <p className="card-subtitle">TCP Traffic via Redundant Scheduler</p>
                  </div>
                </div>
              </div>

              <div className="server-info-grid">
                <div className="info-item">
                  <span className="info-icon">🌐</span>
                  <div>
                    <div className="info-label">Server IP</div>
                    <div className="info-value">{serverInfo.server}</div>
                  </div>
                </div>

                <div className="info-item">
                  <span className="info-icon">📤</span>
                  <div>
                    <div className="info-label">Port</div>
                    <div className="info-value">{serverInfo.port} (Redundant)</div>
                  </div>
                </div>

                <div className="info-item">
                  <span className="info-icon">🔧</span>
                  <div>
                    <div className="info-label">Protocol</div>
                    <div className="info-value">TCP/iperf3</div>
                  </div>
                </div>

                <div className="info-item">
                  <span className="info-icon">📊</span>
                  <div>
                    <div className="info-label">Status</div>
                    <div className="info-value">{iperfRunning ? 'Sending Traffic' : 'Inactive'}</div>
                  </div>
                </div>
              </div>

              <div className="stream-controls">
                <button 
                  onClick={handleStartIPerf} 
                  disabled={iperfRunning || loading} 
                  className="btn-stream-start"
                >
                  {loading && !iperfRunning ? '⏳ Starting...' : '🚀 Start AWS iperf3 Traffic'}
                </button>
                
                <button 
                  onClick={handleStopIPerf} 
                  disabled={!iperfRunning || loading} 
                  className="btn-stream-stop"
                >
                  {loading && iperfRunning ? '⏳ Stopping...' : '🛑 Stop Traffic'}
                </button>
              </div>

              {iperfRunning && (
                <div className="streaming-notice">
                  <span className="notice-pulse"></span>
                  <span>Active: iperf3 → AWS:{serverInfo.port} → Packet Monitor</span>
                </div>
              )}

              {/* Additional Controls */}
              <div className="additional-controls">
                <button onClick={clearPackets} className="btn-secondary">
                  🗑️ Clear Packets
                </button>
                
                <button onClick={handleReset} className="btn-secondary">
                  🔄 Reset Sequence
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Info Banner */}
        <div className="info-banner">
          <strong>ℹ️ Data Flow:</strong> iperf3 Client → TCP (Redundant Scheduler) → 
          AWS ({serverInfo.server}:{serverInfo.port}) → Packet Loss Monitor → 
          Real-time Visualization (100 packets displayed)
        </div>
      </div>
    </div>
  );
}