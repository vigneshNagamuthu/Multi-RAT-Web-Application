import { useEffect, useState, useRef } from 'react';

export default function useSensorWebSocket(url) {
  const [packets, setPackets] = useState([]);
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
            const newPackets = JSON.parse(event.data);
            setPackets(prev => {
              const updated = [...prev, ...newPackets];
              return updated.slice(-100);
            });
          } catch (err) {
            console.error('Error parsing message:', err);
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
      if (reconnectTimeout.current) {
        clearTimeout(reconnectTimeout.current);
      }
      if (ws.current) {
        ws.current.close();
      }
    };
  }, [url]);

  const clearPackets = () => setPackets([]);

  return { packets, isConnected, error, clearPackets };
}