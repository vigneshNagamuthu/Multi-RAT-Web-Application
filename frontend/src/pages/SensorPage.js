import { useState, useEffect, useRef } from 'react';
import './SensorPage.css';

export default function SensorPage() {
  const [isTransmitting, setIsTransmitting] = useState(false);
  const [loading, setLoading] = useState(false);
  const [useMptcp, setUseMptcp] = useState(true); // NEW: MPTCP toggle state
  const [stats, setStats] = useState({
    sentPackets: 0,
    receivedPackets: 0,
    lostPackets: 0,
    packetLossRate: '0.00%',
    avgLatency: 0,
    minLatency: 0,
    maxLatency: 0,
    delayedPackets: 0,
    server: '13.212.221.200',
    port: 5000,
    packetsPerSecond: 10,
    useMptcp: true // NEW: Include in stats
  });
  const [receivedSequences, setReceivedSequences] = useState([]);
  const [retransmittedPackets, setRetransmittedPackets] = useState([]);
  const [packetsPerSecond, setPacketsPerSecond] = useState(10);
  
  const wsRef = useRef(null);

  // WebSocket connection for real-time updates
  useEffect(() => {
    const ws = new WebSocket('ws://localhost:8080/ws/sensor');
    wsRef.current = ws;

    ws.onopen = () => {
      console.log('✅ WebSocket connected');
    };

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        
        if (message.type === 'status') {
          // Update stats from server
          setStats(message.data);
          if (message.data.receivedSequences) {
            setReceivedSequences(message.data.receivedSequences.slice(-50)); // Keep last 50
          }
          if (message.data.retransmittedPackets) {
            setRetransmittedPackets(message.data.retransmittedPackets);
          }
          // Update useMptcp from server status
          if (message.data.hasOwnProperty('useMptcp')) {
            setUseMptcp(message.data.useMptcp);
          }
        }
      } catch (err) {
        console.error('Error parsing WebSocket message:', err);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    ws.onclose = () => {
      console.log('WebSocket disconnected');
    };

    // Fetch initial status
    fetchStatus();

    return () => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.close();
      }
    };
  }, []);

  const fetchStatus = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/sensor/tcp/status');
      const data = await response.json();
      setStats(data);
      setIsTransmitting(data.isRunning);
      if (data.receivedSequences) {
        setReceivedSequences(data.receivedSequences.slice(-50));
      }
      if (data.retransmittedPackets) {
        setRetransmittedPackets(data.retransmittedPackets);
      }
      // Update useMptcp from server status
      if (data.hasOwnProperty('useMptcp')) {
        setUseMptcp(data.useMptcp);
      }
    } catch (err) {
      console.error('Error fetching status:', err);
    }
  };

  // NEW: Handle MPTCP toggle
  const handleMptcpToggle = async (enabled) => {
    if (isTransmitting) {
      alert('⚠️ Cannot change protocol while transmission is running. Stop transmission first.');
      return;
    }

    try {
      const response = await fetch(
        `http://localhost:8080/api/sensor/tcp/set-mptcp?useMptcp=${enabled}`,
        { method: 'POST' }
      );
      const data = await response.json();
      
      if (data.status === 'success') {
        setUseMptcp(enabled);
        console.log(`✅ Protocol mode set to: ${enabled ? 'MPTCP' : 'TCP'}`);
      } else {
        alert(`❌ ${data.message}`);
        // Revert toggle if failed
        setUseMptcp(!enabled);
      }
    } catch (err) {
      console.error('Error setting MPTCP mode:', err);
      alert('❌ Error changing protocol mode');
      // Revert toggle if failed
      setUseMptcp(!enabled);
    }
  };

  const handleStart = async () => {
    setLoading(true);
    try {
      const response = await fetch(
        `http://localhost:8080/api/sensor/tcp/start?packetsPerSecond=${packetsPerSecond}`,
        { method: 'POST' }
      );
      const data = await response.json();
      
      if (data.status === 'success') {
        setIsTransmitting(true);
        setStats(data);
        const protocol = data.useMptcp ? 'MPTCP' : 'TCP';
        alert(`✅ Packet Transmission Started!\n\nProtocol: ${protocol}\nSending to: ${data.server}:${data.port}\nRate: ${packetsPerSecond} packets/second`);
      } else {
        alert(`❌ Failed to start transmission:\n${data.message}`);
      }
    } catch (err) {
      console.error('Error starting transmission:', err);
      alert('❌ Error connecting to backend. Make sure backend is running on localhost:8080');
    } finally {
      setLoading(false);
    }
  };

  const handleStop = async () => {
    setLoading(true);
    try {
      await fetch('http://localhost:8080/api/sensor/tcp/stop', { method: 'POST' });
      setIsTransmitting(false);
      alert('🛑 Packet transmission stopped');
      await fetchStatus(); // Refresh final stats
    } catch (err) {
      console.error('Error stopping transmission:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = async () => {
    setLoading(true);
    try {
      await fetch('http://localhost:8080/api/sensor/tcp/reset', { method: 'POST' });
      setReceivedSequences([]);
      setRetransmittedPackets([]);
      await fetchStatus();
    } catch (err) {
      console.error('Error resetting:', err);
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
              <h2>📡 Sensor Data Transmission</h2>
              <p>TCP Packet Testing - AWS Server (Port {stats.port})</p>
            </div>
            <div className={`status-badge ${isTransmitting ? 'active' : 'inactive'}`}>
              {isTransmitting ? '🟢 TRANSMITTING' : '⚫ IDLE'}
            </div>
          </div>
        </div>

        {/* Main Stats Grid */}
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon">📤</div>
            <div className="stat-content">
              <div className="stat-label">Packets Sent</div>
              <div className="stat-value">{stats.sentPackets.toLocaleString()}</div>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">📥</div>
            <div className="stat-content">
              <div className="stat-label">Packets Received</div>
              <div className="stat-value">{stats.receivedPackets.toLocaleString()}</div>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">⏱️</div>
            <div className="stat-content">
              <div className="stat-label">Avg Latency</div>
              <div className="stat-value latency">{stats.avgLatency}ms</div>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">⚠️</div>
            <div className="stat-content">
              <div className="stat-label">Retransmitted</div>
              <div className="stat-value delayed">{stats.delayedPackets.toLocaleString()}</div>
              <div className="stat-sublabel">&gt;1000ms</div>
            </div>
          </div>
        </div>

        {/* Latency Stats Grid */}
        <div className="latency-stats-grid">
          <div className="latency-stat-card">
            <div className="latency-stat-label">Min Latency</div>
            <div className="latency-stat-value">{stats.minLatency}ms</div>
          </div>
          
          <div className="latency-stat-card">
            <div className="latency-stat-label">Max Latency</div>
            <div className="latency-stat-value">{stats.maxLatency}ms</div>
          </div>
          
          <div className="latency-stat-card">
            <div className="latency-stat-label">In Transit</div>
            <div className="latency-stat-value">{stats.lostPackets.toLocaleString()}</div>
          </div>
          
          <div className="latency-stat-card">
            <div className="latency-stat-label">Delivery Rate</div>
            <div className="latency-stat-value">{stats.deliveryRate}</div>
          </div>
        </div>

        {/* Control Panel */}
        <div className="control-panel">
          <div className="control-section">
            <h3>⚙️ Configuration</h3>
            
            {/* NEW: MPTCP Toggle Section */}
            <div className="protocol-toggle-section">
              <label className="protocol-label">
                <span className="protocol-label-text">Protocol Mode</span>
                <div className="protocol-toggle-wrapper">
                  <button
                    onClick={() => handleMptcpToggle(false)}
                    disabled={isTransmitting}
                    className={`protocol-toggle-button ${!useMptcp ? 'active' : ''}`}
                  >
                    TCP
                  </button>
                  <button
                    onClick={() => handleMptcpToggle(true)}
                    disabled={isTransmitting}
                    className={`protocol-toggle-button ${useMptcp ? 'active' : ''}`}
                  >
                    MPTCP
                  </button>
                </div>
              </label>
              <p className="protocol-info">
                {useMptcp ? (
                  <>
                    <span className="protocol-icon">🚀</span>
                    <strong>MPTCP Mode:</strong> Uses multiple network paths for improved reliability. 
                    Requires mptcpize and socat installed on Linux.
                  </>
                ) : (
                  <>
                    <span className="protocol-icon">📶</span>
                    <strong>TCP Mode:</strong> Standard single-path TCP connection. 
                    Works on all systems including Mac.
                  </>
                )}
              </p>
            </div>

            <div className="config-grid">
              <div className="config-item">
                <label>Server Address</label>
                <input 
                  type="text" 
                  value={stats.server} 
                  disabled 
                  className="config-input"
                />
              </div>
              <div className="config-item">
                <label>Port</label>
                <input 
                  type="text" 
                  value={stats.port} 
                  disabled 
                  className="config-input"
                />
              </div>
              <div className="config-item">
                <label>Packets Per Second</label>
                <input 
                  type="number" 
                  value={packetsPerSecond}
                  onChange={(e) => setPacketsPerSecond(Math.max(1, Math.min(100, parseInt(e.target.value) || 10)))}
                  min="1"
                  max="100"
                  disabled={isTransmitting}
                  className="config-input"
                />
              </div>
              <div className="config-item">
                <label>Current Protocol</label>
                <input 
                  type="text" 
                  value={useMptcp ? "MPTCP" : "TCP"} 
                  disabled 
                  className="config-input"
                />
              </div>
            </div>
          </div>

          <div className="control-buttons">
            <button
              onClick={handleStart}
              disabled={isTransmitting || loading}
              className="btn-start"
            >
              {loading && !isTransmitting ? '⏳ Starting...' : '▶️ Start Transmission'}
            </button>
            
            <button
              onClick={handleStop}
              disabled={!isTransmitting || loading}
              className="btn-stop"
            >
              {loading && isTransmitting ? '⏳ Stopping...' : '⏹️ Stop Transmission'}
            </button>
            
            <button
              onClick={handleReset}
              disabled={isTransmitting || loading}
              className="btn-reset"
            >
              🔄 Reset
            </button>
          </div>
        </div>

        {/* Received Packets Display */}
        <div className="packets-display">
          <div className="packets-header">
            <h3>📊 Received Packets at AWS Server</h3>
            <span className="packet-count">{receivedSequences.length} packets shown (last 50)</span>
          </div>
          <div className="packets-list">
            {receivedSequences.length === 0 ? (
              <div className="no-packets">
                <div className="no-packets-icon">🔭</div>
                <p>No packets received yet</p>
                <p className="no-packets-hint">Start transmission to see packets</p>
              </div>
            ) : (
              receivedSequences.map((seqNum, index) => {
                const isRetransmitted = retransmittedPackets.includes(seqNum);
                return (
                  <div 
                    key={index} 
                    className={`packet-list-item ${isRetransmitted ? 'retransmitted' : 'successful'}`}
                  >
                    <span className="packet-number">#{seqNum}</span>
                    <span className={`packet-status-badge ${isRetransmitted ? 'retransmitted' : 'successful'}`}>
                      {isRetransmitted ? '⚠️ Retransmitted' : '✅ Successful'}
                    </span>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Info Banner */}
        <div className="info-banner">
          <strong>💡 About This Test:</strong> This page sends timestamped TCP packets to AWS 
          ({stats.server}:{stats.port}) and measures round-trip latency. <strong>"Retransmitted"</strong> 
          are packets that took &gt;1 second to arrive (often due to TCP retransmissions). 
          <strong>"In Transit"</strong> are packets currently being transmitted or retransmitted. 
          With normal TCP, retransmissions cause latency spikes - critical for real-time sensor data. 
          <strong>MPTCP Redundant</strong> will eliminate delays by sending duplicates on multiple paths simultaneously.
          {!useMptcp && (
            <div className="tcp-mode-notice">
              ⚠️ <strong>TCP Mode Active:</strong> You're using standard TCP. MPTCP provides better reliability on Linux systems with proper setup.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}