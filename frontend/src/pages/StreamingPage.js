import { useState, useEffect, useRef } from 'react';
import Hls from 'hls.js';
import './StreamingPage.css';

export default function StreamingPage() {
  const [streaming, setStreaming] = useState(false);
  const [loading, setLoading] = useState(false);
  const [videoLoaded, setVideoLoaded] = useState(false);
  const [videoError, setVideoError] = useState(null);

  const [devices, setDevices] = useState([]);
  const [selectedDevice, setSelectedDevice] = useState('');

  // Server configuration (must match your backend / AWS)
  const serverInfo = {
    server: '13.212.221.200',
    inputPort: 6060,  // Port for sending stream TO AWS
    outputPort: 6061  // Port for receiving HLS FROM AWS
  };

  const videoRef = useRef(null);
  const hlsRef = useRef(null);

  useEffect(() => {
    // Check streaming status on load
    fetch('http://localhost:8080/api/streaming/status')
      .then(res => res.json())
      .then(data => {
        if (typeof data.isStreaming === 'boolean') {
          setStreaming(data.isStreaming);
          if (data.isStreaming) {
            setTimeout(() => loadVideo(), 2000);
          }
        }
      })
      .catch(err => console.error('Error fetching status:', err));

    // Fetch available camera devices
    fetch('http://localhost:8080/api/streaming/devices')
      .then(res => res.json())
      .then(list => {
        setDevices(list || []);
        if (list && list.length > 0) {
          setSelectedDevice(list[0].path);
        }
      })
      .catch(err => console.error('Error fetching devices:', err));

    // Cleanup on unmount
    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadVideo = () => {
    const video = videoRef.current;
    if (!video) return;

    const hlsUrl = `http://${serverInfo.server}:${serverInfo.outputPort}/hls/stream.m3u8`;

    console.log('Loading HLS stream from AWS:', hlsUrl);

    if (Hls.isSupported()) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        maxBufferLength: 10,
        maxMaxBufferLength: 20,
        manifestLoadingTimeOut: 10000,
        manifestLoadingMaxRetry: 4,
        levelLoadingTimeOut: 10000,
        levelLoadingMaxRetry: 4,
      });

      hlsRef.current = hls;

      hls.loadSource(hlsUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        console.log('HLS manifest loaded successfully');
        video.play().then(() => {
          setVideoLoaded(true);
          setVideoError(null);
          console.log('Video playback started');
        }).catch(err => {
          console.error('Error playing video:', err);
          setVideoError('Click play button to start');
        });
      });

      hls.on(Hls.Events.ERROR, (event, data) => {
        console.error('HLS Error:', data);
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              console.log('Network error, retrying...');
              hls.startLoad();
              setVideoError('Network error - retrying...');
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              console.log('Media error, recovering...');
              hls.recoverMediaError();
              setVideoError('Media error - recovering...');
              break;
            default:
              console.error('Fatal error, destroying player');
              hls.destroy();
              setVideoError('Stream unavailable. Try restarting the stream.');
              break;
          }
        }
      });

    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      // Native HLS support (Safari)
      console.log('Using native HLS support');
      video.src = hlsUrl;
      video.addEventListener('loadedmetadata', () => {
        video.play().then(() => {
          setVideoLoaded(true);
          setVideoError(null);
        });
      });
    } else {
      setVideoError('HLS not supported in this browser');
      console.error('HLS not supported');
    }
  };

  const handleStartStream = async () => {
    setLoading(true);
    setVideoError(null);

    try {
      const body = {
        devicePath: selectedDevice || null
      };

      const response = await fetch('http://localhost:8080/api/streaming/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });

      const data = await response.json();

      if (data.status === 'success') {
        setStreaming(true);
        alert(
          `Streaming Started!\n\n` +
          `Sending to AWS: ${serverInfo.server}:${serverInfo.inputPort}\n` +
          `Receiving from: ${serverInfo.server}:${serverInfo.outputPort}\n` +
          `Scheduler: LRTT (Lowest RTT First)\n\n` +
          `Video will appear in 10–15 seconds.`
        );

        // Wait for stream to stabilise, then load video
        setTimeout(() => {
          loadVideo();
        }, 8000);
      } else {
        alert(
          `Failed to Start Stream\n\n` +
          `${data.message}\n\n` +
          `Requirements:\n` +
          `• FFmpeg installed on your computer\n` +
          `• Camera permission granted\n` +
          `• AWS server running (${serverInfo.server})`
        );
      }
    } catch (err) {
      console.error('Error starting stream:', err);
      alert('Error connecting to backend. Make sure backend is running on localhost:8080');
    } finally {
      setLoading(false);
    }
  };

  const handleStopStream = async () => {
    setLoading(true);

    try {
      await fetch('http://localhost:8080/api/streaming/stop', { method: 'POST' });
      setStreaming(false);
      setVideoLoaded(false);

      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }

      if (videoRef.current) {
        videoRef.current.pause();
        videoRef.current.src = '';
      }

      console.log('Streaming stopped');
      alert('Streaming stopped successfully');
    } catch (err) {
      console.error('Error stopping stream:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRetry = () => {
    setVideoError(null);
    setVideoLoaded(false);

    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    setTimeout(() => {
      loadVideo();
    }, 1000);
  };

  return (
    <div className="streaming-page">
      <div className="streaming-container">
        {/* Header */}
        <div className="page-header">
          <div className="header-content">
            <div className="header-left">
              <h2>Video Streaming</h2>
              <p>Port {serverInfo.inputPort} – LRTT Scheduler (Lowest RTT First)</p>
            </div>
          </div>
        </div>

        {/* Content Grid */}
        <div className="content-grid-full">
          {/* Video Section */}
          <div className="video-section">
            {/* Video Player Card */}
            <div className="video-card">
              <div className="video-header">
                <h3>Live Stream from AWS</h3>
                <span className={`video-status-badge ${streaming ? (videoLoaded ? 'live' : 'loading') : 'offline'}`}>
                  {streaming ? (videoLoaded ? 'LIVE' : 'LOADING') : 'OFFLINE'}
                </span>
              </div>

              <div className="video-container">
                {streaming ? (
                  <>
                    <video
                      ref={videoRef}
                      autoPlay
                      playsInline
                      muted
                      controls
                      className="video-player"
                    />
                    {!videoLoaded && !videoError && (
                      <div className="video-loading-overlay">
                        <div className="loading-spinner"></div>
                        <p>Connecting to AWS server...</p>
                        <p className="loading-subtext">Port {serverInfo.outputPort}</p>
                      </div>
                    )}
                    {videoError && (
                      <div className="video-error-overlay">
                        <div className="error-icon">!</div>
                        <p>{videoError}</p>
                        <button onClick={handleRetry} className="retry-btn">
                          Retry Connection
                        </button>
                      </div>
                    )}
                  </>
                ) : (
                  <div className="video-placeholder-offline">
                    <div className="offline-icon">📹</div>
                    <p>Ready to stream</p>
                    <p className="offline-subtext">Click "Start AWS Stream" to begin</p>
                  </div>
                )}
              </div>

              {streaming && videoLoaded && (
                <div className="video-info">
                  <span className="info-badge">
                    {serverInfo.server}:{serverInfo.outputPort}/hls
                  </span>
                  <span className="info-badge">MPTCP LRTT</span>
                  <span className="info-badge">H.264</span>
                  <span className="info-badge">HLS Stream</span>
                </div>
              )}
            </div>

            {/* AWS Control Card */}
            <div className="aws-control-card">
              <div className="card-header">
                <div className="header-icon-section">
                  <span className="header-icon">☁️</span>
                  <div>
                    <h3 className="card-title">AWS Lightsail Server</h3>
                    <p className="card-subtitle">MPTCP Video Streaming via LRTT Scheduler</p>
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
                    <div className="info-label">Upload Port</div>
                    <div className="info-value">{serverInfo.inputPort} (LRTT)</div>
                  </div>
                </div>

                <div className="info-item">
                  <span className="info-icon">📥</span>
                  <div>
                    <div className="info-label">Stream Port</div>
                    <div className="info-value">{serverInfo.outputPort} (HLS)</div>
                  </div>
                </div>

                <div className="info-item">
                  <span className="info-icon">🎥</span>
                  <div>
                    <div className="info-label">Status</div>
                    <div className="info-value">
                      {streaming ? (videoLoaded ? 'Streaming' : 'Connecting') : 'Inactive'}
                    </div>
                  </div>
                </div>
              </div>

              {/* Camera selector */}
              <div className="camera-selector">
                <label htmlFor="camera-select">Camera source</label>
                <select
                  id="camera-select"
                  value={selectedDevice}
                  onChange={e => setSelectedDevice(e.target.value)}
                  disabled={streaming || loading || devices.length === 0}
                >
                  {devices.length === 0 && (
                    <option value="">No cameras detected</option>
                  )}
                  {devices.map((dev, idx) => (
                    <option key={dev.path || idx} value={dev.path}>
                      {dev.name} ({dev.path})
                    </option>
                  ))}
                </select>
                {devices.length > 1 && (
                  <p className="camera-hint">
                    Select your external webcam before starting the stream.
                  </p>
                )}
              </div>

              <div className="stream-controls">
                <button
                  onClick={handleStartStream}
                  disabled={streaming || loading}
                  className="btn-stream-start"
                >
                  {loading && !streaming ? 'Starting...' : 'Start AWS Stream'}
                </button>

                <button
                  onClick={handleStopStream}
                  disabled={!streaming || loading}
                  className="btn-stream-stop"
                >
                  {loading && streaming ? 'Stopping...' : 'Stop Stream'}
                </button>
              </div>

              {streaming && (
                <div className="streaming-notice">
                  <span className="notice-pulse"></span>
                  <span>
                    Active: Camera → AWS:{serverInfo.inputPort} → Browser:{serverInfo.outputPort}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Info Banner */}
        <div className="info-banner">
          <strong>Stream Flow:</strong> Your camera → FFmpeg → MPTCP (LRTT Scheduler) →
          AWS ({serverInfo.server}:{serverInfo.inputPort}) → HLS Conversion →
          Your Browser ({serverInfo.server}:{serverInfo.outputPort}/hls/stream.m3u8)
        </div>
      </div>
    </div>
  );
}

