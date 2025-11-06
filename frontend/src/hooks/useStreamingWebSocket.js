import { useEffect, useState, useRef } from 'react';

export default function useStreamingWebSocket(url) {
  const [metrics, setMetrics] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState(null);
  const ws = useRef(null);
  const reconnectTimeout = useRef(null);

  useEffect(() => {
    const connect = () => {
      try {
        ws.current = new WebSocket(url);

        ws.current.onopen = () => {
          console.log('Streaming WebSocket connected');
          setIsConnected(true);
          setError(null);
        };

        ws.current.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            setMetrics(data);
          } catch (err) {
            console.error('Error parsing message:', err);
          }
        };

        ws.current.onerror = (err) => {
          console.error('WebSocket error:', err);
          setError('Connection error');
        };

        ws.current.onclose = () => {
          console.log('Streaming WebSocket disconnected');
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

  return { metrics, isConnected, error };
}