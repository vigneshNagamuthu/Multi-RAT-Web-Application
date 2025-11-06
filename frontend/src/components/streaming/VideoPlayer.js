import { useRef, useState, useEffect } from 'react';
import Hls from 'hls.js';
import './VideoPlayer.css';

const HLS_URL = 'http://13.212.221.200:6061/hls/stream.m3u8';

export default function VideoPlayer() {
  const videoRef = useRef(null);
  const hlsRef = useRef(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    return () => {
      // Cleanup on unmount
      if (hlsRef.current) {
        hlsRef.current.destroy();
      }
    };
  }, []);

  const handleStart = async () => {
    try {
      setError(null);
      console.log('🎬 Starting stream...');
      
      // Start streaming from backend
      console.log('📡 Calling backend to start streaming...');
      const response = await fetch('http://localhost:8080/api/streaming/start', {
        method: 'POST'
      });
      
      if (!response.ok) {
        throw new Error('Failed to start streaming from backend');
      }
      
      console.log('✅ Backend streaming started');
      
      // Reduce wait time - segments generate fast with copy mode
      console.log('⏳ Waiting for stream to initialize...');
      await new Promise(resolve => setTimeout(resolve, 3000));
      
      console.log('🎥 Setting up HLS player with URL:', HLS_URL);
      
      // Setup HLS player
      if (Hls.isSupported()) {
        console.log('✅ HLS.js is supported');
        const hls = new Hls({
          enableWorker: true,
          lowLatencyMode: true,  // Enable low-latency mode
          backBufferLength: 10,  // Minimize back buffer
          maxBufferLength: 10,   // Keep buffer small for low latency
          maxMaxBufferLength: 15,
          liveSyncDurationCount: 2,  // Stay close to live edge
          liveMaxLatencyDurationCount: 3,
          debug: true,
          manifestLoadingTimeOut: 10000,
          manifestLoadingMaxRetry: 3,
          manifestLoadingRetryDelay: 1000,
          levelLoadingTimeOut: 10000,
          levelLoadingMaxRetry: 3,
          levelLoadingRetryDelay: 1000
        });
        
        hls.loadSource(HLS_URL);
        hls.attachMedia(videoRef.current);
        
        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          console.log('✅ HLS manifest parsed, starting playback');
          videoRef.current.play();
          setIsPlaying(true);
        });
        
        hls.on(Hls.Events.ERROR, (event, data) => {
          console.error('❌ HLS Error:', data);
          if (data.fatal) {
            setError(`Stream error: ${data.type} - ${data.details}`);
            setIsPlaying(false);
          }
        });
        
        hlsRef.current = hls;
      } else if (videoRef.current.canPlayType('application/vnd.apple.mpegurl')) {
        // Native HLS support (Safari)
        console.log('✅ Using native HLS support');
        videoRef.current.src = HLS_URL;
        videoRef.current.addEventListener('loadedmetadata', () => {
          console.log('✅ Video metadata loaded');
          videoRef.current.play();
          setIsPlaying(true);
        });
      } else {
        setError('HLS is not supported in this browser');
      }
      
    } catch (err) {
      console.error('❌ Error starting stream:', err);
      setError('Could not start video stream. Check if backend is running.');
    }
  };

  const handleStop = async () => {
    try {
      // Stop HLS player
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
      
      if (videoRef.current) {
        videoRef.current.pause();
        videoRef.current.src = '';
      }
      
      // Stop streaming from backend
      await fetch('http://localhost:8080/api/streaming/stop', {
        method: 'POST'
      });
      
      setIsPlaying(false);
    } catch (err) {
      console.error('Error stopping stream:', err);
    }
  };

  return (
    <div className="video-player">
      <div className="video-container">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
        />
      </div>

      {error && (
        <div className="alert alert-error">
          ❌ {error}
        </div>
      )}

      <div className="video-controls">
        <button
          onClick={handleStart}
          disabled={isPlaying}
          className="btn-success"
        >
          ▶️ Start Streaming
        </button>
        
        <button
          onClick={handleStop}
          disabled={!isPlaying}
          className="btn-danger"
        >
          ⏹️ Stop Streaming
        </button>
      </div>

      <div className="video-status">
        <div className="status-left">
          <span className={`status-dot ${isPlaying ? 'active' : 'inactive'}`}></span>
          <span className="status-text">
            {isPlaying ? '🔴 Live - Port 6060 (LRTT Scheduler)' : '⚪ Not Streaming'}
          </span>
        </div>
        <span className="status-right">
          {isPlaying ? 'Camera Active' : 'Camera Inactive'}
        </span>
      </div>
    </div>
  );
}