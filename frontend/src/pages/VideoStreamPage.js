import React, { useEffect, useRef, useState } from 'react';

const WS_URL = 'ws://localhost:8080/video';

export default function VideoStreamPage() {
  const videoRef = useRef(null);
  const imgRef = useRef(null);
  const wsRef = useRef(null);
  const [fps, setFps] = useState(5);
  const [sent, setSent] = useState(0);
  const [received, setReceived] = useState(0);
  const [connected, setConnected] = useState(false);
  const runningRef = useRef(false);

  useEffect(() => {
    navigator.mediaDevices.getUserMedia({ video: true, audio: false })
      .then(stream => { videoRef.current.srcObject = stream; })
      .catch(err => console.error('getUserMedia error', err));
  }, []);

  useEffect(() => {
    return () => { runningRef.current = false; wsRef.current && wsRef.current.close(); };
  }, []);

  const start = () => {
    if (connected) return;
    wsRef.current = new WebSocket(WS_URL);
    wsRef.current.onopen = () => { setConnected(true); runningRef.current = true; loop(); };
    wsRef.current.onmessage = ev => {
      try {
        const msg = JSON.parse(ev.data);
        if (msg.type === 'frame' && imgRef.current) {
          imgRef.current.src = msg.data;
          setReceived(r => r + 1);
        }
      } catch { /* ignore */ }
    };
    wsRef.current.onclose = () => { setConnected(false); runningRef.current = false; };
  };

  const stop = () => {
    runningRef.current = false;
    wsRef.current && wsRef.current.close();
  };

  const loop = () => {
    if (!runningRef.current) return;
    const v = videoRef.current;
    if (!v) return;
    const canvas = document.createElement('canvas');
    canvas.width = v.videoWidth || 640;
    canvas.height = v.videoHeight || 480;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(v, 0, 0, canvas.width, canvas.height);
    const dataUrl = canvas.toDataURL('image/jpeg', 0.6);
    try {
      wsRef.current.send(JSON.stringify({ type: 'frame', ts: Date.now(), data: dataUrl }));
      setSent(s => s + 1);
    } catch {}
    setTimeout(loop, 1000 / fps);
  };

  return (
    <div style={styles.container}>
      <h2>Video Stream Prototype</h2>
      <div style={styles.section}>
        <h4>Local Camera</h4>
        <video ref={videoRef} autoPlay playsInline style={styles.video} />
      </div>
      <div style={styles.section}>
        <h4>Received (Broadcast)</h4>
        <img ref={imgRef} alt="stream" style={styles.video} />
      </div>
      <div style={styles.controls}>
        <label>FPS: <input type="number" min={1} max={15} value={fps} onChange={e => setFps(+e.target.value)} /></label>
        {!connected ? <button onClick={start}>Connect & Start</button> : <button onClick={stop}>Stop</button>}
      </div>
      <div style={styles.stats}>Sent: {sent} | Received: {received} | Connected: {connected ? 'yes' : 'no'}</div>
      <p style={styles.note}>Note: JPEG frame rebroadcast to all connected clients. For demo only.</p>
    </div>
  );
}

const styles = {
  container: { padding: '1rem', fontFamily: 'sans-serif' },
  section: { display: 'inline-block', marginRight: '1rem', verticalAlign: 'top' },
  video: { width: 320, height: 240, background: '#222', objectFit: 'cover' },
  controls: { marginTop: '1rem' },
  stats: { marginTop: '0.5rem', fontSize: '0.9rem' },
  note: { marginTop: '1rem', fontSize: '0.8rem', color: '#555' }
};
