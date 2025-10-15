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
          <h1>Hybrid MPTCP Scheduler</h1>
          <p>Port-Based Scheduler Implementation</p>
          
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
              Real-time video streaming with LRTT scheduler for optimal latency and performance on port 6060
            </p>
            
            <ul className="feature-list">
              <li>
                <span className="checkmark">✓</span>
                <span>Lowest RTT First selection</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Real-time latency monitoring</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Camera stream to MPTCP server</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Live FPS & packet loss metrics</span>
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
              High-reliability IoT data transmission with Redundant scheduler for mission-critical applications on port 5000
            </p>
            
            <ul className="feature-list">
              <li>
                <span className="checkmark">✓</span>
                <span>High reliability transmission</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Packet loss visualization</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Real-time sequence monitoring</span>
              </li>
              <li>
                <span className="checkmark">✓</span>
                <span>Loss rate statistics</span>
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
                <li>Selects subflow with lowest RTT</li>
                <li>Optimized for latency-sensitive apps</li>
                <li>Dynamic path switching</li>
                <li>Real-time metric monitoring</li>
              </ul>
            </div>
            
            <div className="tech-item">
              <h4>Redundant Scheduler (Port 5000)</h4>
              <ul>
                <li>Sends packets on multiple paths</li>
                <li>High reliability for IoT data</li>
                <li>Tolerates packet loss</li>
                <li>Sequence validation</li>
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