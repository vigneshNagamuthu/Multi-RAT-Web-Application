import { useRef, useState } from 'react';
import './VideoPlayer.css';

export default function VideoPlayer() {
  const videoRef = useRef(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [error, setError] = useState(null);

  const handleStart = async () => {
    try {
      setError(null);
      const stream = await navigator.mediaDevices.getUserMedia({ 
        video: {
          width: { ideal: 1280 },
          height: { ideal: 720 }
        },
        audio: false
      });
      
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        setIsPlaying(true);
      }
    } catch (err) {
      console.error('Error accessing camera:', err);
      setError('Could not access camera. Please check permissions.');
    }
  };

  const handleStop = () => {
    if (videoRef.current && videoRef.current.srcObject) {
      const tracks = videoRef.current.srcObject.getTracks();
      tracks.forEach(track => track.stop());
      videoRef.current.srcObject = null;
      setIsPlaying(false);
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