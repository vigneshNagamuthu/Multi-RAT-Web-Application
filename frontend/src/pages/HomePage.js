import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import './HomePage.css';

export default function HomePage() {
  const [config, setConfig] = useState(null);

  useEffect(() => {
    fetch('http://localhost:8080/api/scheduler/config')
      .then(res => res.json())
      .then(data => setConfig(data))
      .catch(err => console.error('Error fetching config:', err));
  }, []);

  return (
    <div className="home-page">
      <div className="home-container">
        <div className="home-header">
          <h1>Application-Aware MPTCP Scheduler</h1>
          <p>Port-Based Scheduler Demo</p>
          
          {config && (
            <div className="config-info">
              ✨ {config.description}
            </div>
          )}
        </div>

        <div className="feature-cards">
          {/* Streaming Card */}
          <Link to="/streaming" className="feature-card streaming">
            <div className="feature-icon-circle">
              📹
            </div>
            
            <h2>Video Streaming</h2>
            <p>
              Real-time webcam streaming optimized for maximum throughput using MPTCP LRTT scheduler on port 6060
            </p>
            
            <ul className="feature-list">
              <li>
                <span className="checkmark">✓</span>
                <span>Lowest RTT path selection</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Live FPS & bitrate monitoring</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>H.264 stream to AWS via HLS</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Real-time latency monitoring</span>
              </li>
            </ul>

            <div className="feature-badge-container">
              <span className="feature-badge streaming">
                🚀 Port 6060 - LRTT
              </span>
            </div>
          </Link>

          {/* Sensor Card */}
          <Link to="/sensor" className="feature-card sensor">
            <div className="feature-icon-circle">
              📡
            </div>
            
            <h2>IoT Sensor Data</h2>
            <p>
              Mission-critical TCP packet transmission using MPTCP Redundant scheduler to minimize packet delays on port 5000
            </p>
            
            <ul className="feature-list">
              <li>
                <span className="checkmark">✓</span>
                <span>MPTCP Redundant scheduler (port 5000)</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Round-trip latency tracking</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Stuck packet detection (>1000ms)</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Real-time delivery rate monitoring</span>
              </li>
            </ul>

            <div className="feature-badge-container">
              <span className="feature-badge sensor">
                🛡️ Port 5000 - Redundant
              </span>
            </div>
          </Link>
        </div>

        {/* Technical Architecture */}
        <div className="tech-section">
          <h3>🔧 Technical Architecture</h3>
          
          <div className="tech-grid">
            <div className="tech-item">
              <h4>LRTT Scheduler (Port 6060)</h4>
              <ul>
                <li>Kernel-level scheduler selection via source port binding</li>
                <li>Selects subflow with lowest RTT dynamically</li>
                <li>Optimized for throughput-sensitive video streaming</li>
                <li>Queue management & congestion awareness</li>
              </ul>
            </div>
            
            <div className="tech-item">
              <h4>Redundant Scheduler (Port 5000)</h4>
              <ul>
                <li>Kernel-level scheduler selection via source port binding</li>
                <li>MPTCP kernel sends duplicate packets on all paths</li>
                <li>First-arriving packet accepted, duplicates dropped</li>
                <li>Near-zero effective packet loss for critical data</li>
              </ul>
            </div>
          </div>
        </div>

        {/* WebSocket Info */}
        <div className="websocket-info">
          <p>WebSocket Endpoints:</p>
          <code>ws://localhost:8080/ws/streaming</code>
          <code>ws://localhost:8080/ws/sensor</code>
        </div>
      </div>
    </div>
  );
}