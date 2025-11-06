import { useEffect, useState, useRef } from 'react';

export default function useSensorWebSocket(url) {
  const [packets, setPackets] = useState([]);
  const [stats, setStats] = useState({ total: 0, lost: 0, lossRate: 0 });
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState(null);
  const ws = useRef(null);
  const reconnectTimeout = useRef(null);

  useEffect(() => {
    const connect = () => {
      try {
        ws.current = new WebSocket(url);

        ws.current.onopen = () => {
          console.log('Sensor WebSocket connected');
          setIsConnected(true);
          setError(null);
        };

        ws.current.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);

            if (data.packets) {
              // ✅ Normalize packet fields to ensure .isLost always exists
              const normalizedPackets = data.packets.map(p => ({
                sequenceNumber: p.sequenceNumber,
                timestamp: p.timestamp,
                // handle both backend formats: "isLost" or "lost"
                isLost: p.isLost !== undefined ? p.isLost : (p.lost === true)
              }));

              setPackets(prev => {
                const updated = [...prev, ...normalizedPackets];
                return updated.slice(-100);
              });

              if (
                data.total !== undefined &&
                data.lost !== undefined &&
                data.lossRate !== undefined
              ) {
                setStats({
                  total: data.total,
                  lost: data.lost,
                  lossRate: data.lossRate
                });
              }
            } else if (Array.isArray(data)) {
              // Fallback for old plain array format
              const normalizedPackets = data.map(p => ({
                sequenceNumber: p.sequenceNumber,
                timestamp: p.timestamp,
                isLost: p.isLost !== undefined ? p.isLost : (p.lost === true)
              }));

              setPackets(prev => {
                const updated = [...prev, ...normalizedPackets];
                return updated.slice(-100);
              });
            }
          } catch (err) {
            console.error('Error parsing WebSocket message:', err);
          }
        };

        ws.current.onerror = (err) => {
          console.error('WebSocket error:', err);
          setError('Connection error');
        };

        ws.current.onclose = () => {
          console.log('Sensor WebSocket disconnected');
          setIsConnected(false);
          reconnectTimeout.current = setTimeout(() => {
            console.log('Attempting to reconnect...');
            connect();
          }, 3000);
        };
      } catch (err) {
        console.error('Failed to create WebSocket:', err);
        setError('Failed to connect');
      }
    };

    connect();

    return () => {
      if (reconnectTimeout.current) clearTimeout(reconnectTimeout.current);
      if (ws.current) ws.current.close();
    };
  }, [url]);

  const clearPackets = () => setPackets([]);

  return { packets, stats, isConnected, error, clearPackets };
}
