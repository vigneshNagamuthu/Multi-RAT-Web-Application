import React, { useEffect, useRef, useState } from 'react';

const WS_URL = 'ws://localhost:8080/sensor';

export default function SensorPage() {
  const wsRef = useRef(null);
  const [messages, setMessages] = useState([]);
  const [connected, setConnected] = useState(false);
  const [value, setValue] = useState('');
  const [rtt, setRtt] = useState(null);
  const pingRef = useRef(null);

  useEffect(() => {
    connect();
    return () => { wsRef.current && wsRef.current.close(); };
  }, []);

  const connect = () => {
    wsRef.current = new WebSocket(WS_URL);
    wsRef.current.onopen = () => setConnected(true);
    wsRef.current.onclose = () => setConnected(false);
    wsRef.current.onmessage = ev => {
      try {
        const msg = JSON.parse(ev.data);
        if (msg.type === 'pong' && pingRef.current) {
          const now = Date.now();
          setRtt(now - pingRef.current);
          pingRef.current = null;
        } else {
          setMessages(m => [...m.slice(-199), msg]);
        }
      } catch { /* ignore */ }
    };
  };

  const sendSensor = () => {
    if (!wsRef.current || wsRef.current.readyState !== 1) return;
    const msg = { type: 'sensor', ts: Date.now(), value: value || Math.random().toFixed(3) };
    wsRef.current.send(JSON.stringify(msg));
    setValue('');
  };

  const ping = () => {
    if (!wsRef.current || wsRef.current.readyState !== 1) return;
    pingRef.current = Date.now();
    wsRef.current.send(JSON.stringify({ type: 'ping' }));
  };

  return (
    <div style={styles.container}>
      <h2>Sensor Channel Prototype</h2>
      <div>Connection: {connected ? 'Connected' : 'Disconnected'}</div>
      <div style={styles.controls}>
        <input placeholder="Value (optional)" value={value} onChange={e => setValue(e.target.value)} />
        <button onClick={sendSensor}>Send Sensor</button>
        <button onClick={ping}>Ping</button>
        {rtt !== null && <span style={styles.rtt}>RTT: {rtt} ms</span>}
      </div>
      <div style={styles.messages}>
        {messages.map((m, i) => <div key={i}>{JSON.stringify(m)}</div>)}
      </div>
      <p style={styles.note}>Broadcast JSON; future: bind this socket to a dedicated port for hybrid scheduler tests.</p>
    </div>
  );
}

const styles = {
  container: { padding: '1rem', fontFamily: 'sans-serif' },
  controls: { marginTop: '0.5rem', display: 'flex', gap: '0.5rem', alignItems: 'center' },
  messages: { marginTop: '1rem', maxHeight: 300, overflow: 'auto', background: '#f5f5f5', padding: '0.5rem', fontSize: '0.8rem' },
  rtt: { marginLeft: '0.75rem', fontSize: '0.85rem', color: '#333' },
  note: { marginTop: '1rem', fontSize: '0.75rem', color: '#555' }
};
