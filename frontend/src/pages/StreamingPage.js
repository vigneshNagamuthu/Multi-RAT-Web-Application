import { useState, useEffect, useRef } from 'react';
import MetricsDisplay from '../components/streaming/MetricsDisplay';
import useStreamingWebSocket from '../hooks/useStreamingWebSocket';
import './StreamingPage.css';

export default function StreamingPage() {
  const { metrics, isConnected, error } = useStreamingWebSocket('ws://localhost:8080/ws/streaming');
  const [streaming, setStreaming] = useState(false);
  const [loading, setLoading] = useState(false);
  const [serverInfo] = useState({ server: '54.169.114.206', port: 6060 });
  const videoRef = useRef(null);

  useEffect(() => {
    // Check streaming status on load
    fetch('http://localhost:8080/api/streaming/status')
      .then(res => res.json())
      .then(data => {
        setStreaming(data.isStreaming);
      })
      .catch(err => console.error('Error fetching status:', err));
  }, []);

  const handleStartStream = async () => {
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/streaming/start', { 
        method: 'POST' 
      });
      const data = await response.json();
      
      if (data.status === 'success') {
        setStreaming(true);
        alert(`✅ Streaming started!\n\nCamera → AWS (${data.server}:${data.port})\nAWS → Your Browser (${data.server}:6061)\n\nWait 5-10 seconds for video to appear...`);
        
        // Start receiving video from AWS after a short delay
        setTimeout(() => {
          startReceivingVideo();
        }, 3000);
      } else {
        alert(`❌ ${data.message}\n\nMake sure:\n• FFmpeg is installed\n• Camera permission granted\n• AWS server is running`);
      }
    } catch (err) {
      console.error('Error:', err);
      alert('❌ Error starting stream. Check console for details.');
    } finally {
      setLoading(false);
    }
  };

  const handleStopStream = async () => {
    setLoading(true);
    try {
      await fetch('http://localhost:8080/api/streaming/stop', { method: 'POST' });
      setStreaming(false);
      
      // Stop video playback
      if (videoRef.current && videoRef.current.srcObject) {
        const stream = videoRef.current.srcObject;
        const tracks = stream.getTracks();
        tracks.forEach(track => track.stop());
        videoRef.current.srcObject = null;
      }
      
      alert('🛑 Streaming stopped');
    } catch (err) {
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const startReceivingVideo = async () => {
    try {
      // For WebRTC or direct stream, you'd use Media Source Extensions
      // For now, we'll show instructions for using VLC or similar
      console.log(`📺 To view stream: vlc tcp://${serverInfo.server}:6061`);
      
      // Alternative: Use img tag for MJPEG stream if AWS is configured for it
      // Or implement WebRTC for true browser playback
    } catch (err) {
      console.error('Error receiving video:', err);
    }
  };

  return (
    <div className="streaming-page">
      <div className="streaming-container">
        {/* Header */}
        <div className="page-header">
          <div className="header-content">
            <div className="header-left">
              <h2>📹 Video Streaming</h2>
              <p>Port 6060 - LRTT Scheduler (Lowest RTT First)</p>
            </div>
            <div className="connection-status">
              <div className="connection-status-label">WebSocket Status</div>
              <div className="status-indicator">
                <span className={`status-dot ${isConnected ? 'connected' : 'disconnected'}`}></span>
                <span>{isConnected ? 'Connected' : 'Disconnected'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Content Grid - Video + Controls */}
        <div className="content-grid-full">
          {/* Video Display Section */}
          <div className="video-section">
            <div className="video-card">
              <div className="video-header">
                <h3>📺 Live Stream from AWS</h3>
                <span className={`video-status-badge ${streaming ? 'live' : 'offline'}`}>
                  {streaming ? '🔴 LIVE' : '⚪ OFFLINE'}
                </span>
              </div>
              
              <div className="video-container">
                {streaming ? (
                  <div className="video-placeholder">
                    <div className="placeholder-content">
                      <div className="streaming-animation">
                        <div className="pulse-circle"></div>
                        <div className="pulse-circle delay-1"></div>
                        <div className="pulse-circle delay-2"></div>
                      </div>
                      <h4>🎥 Stream Active</h4>
                      <p>Video is streaming to AWS server</p>
                      <div className="stream-path">
                        <span className="path-item">Your Camera</span>
                        <span className="path-arrow">→</span>
                        <span className="path-item">AWS ({serverInfo.server}:6060)</span>
                        <span className="path-arrow">→</span>
                        <span className="path-item">Port 6061</span>
                      </div>
                      <div className="view-instructions">
                        <p><strong>To view the stream:</strong></p>
                        <code>ffplay tcp://{serverInfo.server}:6061</code>
                        <p className="instruction-note">or use VLC: Media → Open Network Stream → <code>tcp://{serverInfo.server}:6061</code></p>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="video-placeholder-offline">
                    <div className="offline-icon">📹</div>
                    <p>Click "Start AWS Stream" to begin streaming</p>
                  </div>
                )}
                <video 
                  ref={videoRef} 
                  autoPlay 
                  playsInline 
                  muted
                  style={{ display: 'none' }}
                />
              </div>
            </div>

            {/* AWS Control Card */}
            <div className="aws-control-card">
              <div className="card-header">
                <div className="header-icon-section">
                  <span className="header-icon">☁️</span>
                  <div>
                    <h3 className="card-title">AWS Lightsail Server</h3>
                    <p className="card-subtitle">Stream your camera to AWS using MPTCP</p>
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
                  <span className="info-icon">🔌</span>
                  <div>
                    <div className="info-label">Streaming Port</div>
                    <div className="info-value">{serverInfo.port} (LRTT)</div>
                  </div>
                </div>

                <div className="info-item">
                  <span className="info-icon">📤</span>
                  <div>
                    <div className="info-label">Return Port</div>
                    <div className="info-value">6061</div>
                  </div>
                </div>

                <div className="info-item">
                  <span className="info-icon">🎥</span>
                  <div>
                    <div className="info-label">Status</div>
                    <div className="info-value">{streaming ? 'Active' : 'Inactive'}</div>
                  </div>
                </div>
              </div>

              <div className="stream-controls">
                <button 
                  onClick={handleStartStream} 
                  disabled={streaming || loading} 
                  className="btn-stream-start"
                >
                  {loading && !streaming ? '⏳ Starting...' : '🚀 Start AWS Stream'}
                </button>
                
                <button 
                  onClick={handleStopStream} 
                  disabled={!streaming || loading} 
                  className="btn-stream-stop"
                >
                  {loading && streaming ? '⏳ Stopping...' : '🛑 Stop Stream'}
                </button>
              </div>

              {streaming && (
                <div className="streaming-notice">
                  <span className="notice-pulse"></span>
                  <span>Camera streaming to AWS server at {serverInfo.server}:{serverInfo.port}</span>
                </div>
              )}
            </div>
          </div>

          {/* Metrics Section */}
          <div className="metrics-section">
            <div className="section-card">
              <h3>📊 Real-Time Metrics</h3>
              <MetricsDisplay metrics={metrics} isConnected={isConnected} error={error} />
            </div>
          </div>
        </div>

        {/* Info Banner */}
        <div className="info-banner">
          <strong>ℹ️ How it works:</strong> Your camera streams to AWS ({serverInfo.server}:6060) using MPTCP with LRTT scheduler. 
          AWS server receives the stream and makes it available on port 6061. 
          Use FFplay or VLC to view: <code>ffplay tcp://{serverInfo.server}:6061</code>
        </div>
      </div>
    </div>
  );
}