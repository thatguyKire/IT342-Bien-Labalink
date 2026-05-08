import { useEffect, useRef, useCallback } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

const WS_URL = 'http://localhost:8080/ws';

interface WebSocketMessage {
  type: string;
  message: string;
  data: unknown;
  timestamp: string;
}

interface UseWebSocketOptions {
  onMachineUpdate?: (data: unknown) => void;
  onBookingUpdate?: (data: unknown) => void;
  onWalletUpdate?: (data: unknown) => void;
}

export function useWebSocket(
    options: UseWebSocketOptions) {

  const clientRef = 
    useRef<Stomp.Client | null>(null);
  const connectedRef = useRef(false);
  const reconnectRef = useRef<() => void>(() => {});

  const {
    onMachineUpdate,
    onBookingUpdate,
    onWalletUpdate,
  } = options;

  const connect = useCallback(() => {
    if (connectedRef.current) return;

    const socket = new SockJS(WS_URL);
    const client = Stomp.over(socket);

    // Disable console logs from STOMP
    client.debug = () => {};

    client.connect(
      {},
      () => {
        connectedRef.current = true;
        console.log('WebSocket connected ✅');

        // Subscribe to machine updates
        if (onMachineUpdate) {
          client.subscribe(
            '/topic/machines',
            (msg) => {
              const wsMessage: WebSocketMessage =
                JSON.parse(msg.body);
              onMachineUpdate(wsMessage.data);
            });
        }

        // Subscribe to booking updates
        if (onBookingUpdate) {
          client.subscribe(
            '/topic/bookings',
            (msg) => {
              const wsMessage: WebSocketMessage =
                JSON.parse(msg.body);
              onBookingUpdate(wsMessage.data);
            });
        }

        // Subscribe to wallet updates
        const email = 
          localStorage.getItem('email');
        if (onWalletUpdate && email) {
          client.subscribe(
            `/topic/wallet/${email}`,
            (msg) => {
              const wsMessage: WebSocketMessage =
                JSON.parse(msg.body);
              onWalletUpdate(wsMessage.data);
            });
        }
      },
      (error) => {
        console.error(
          'WebSocket error:', error);
        connectedRef.current = false;

        // Reconnect after 3 seconds
        setTimeout(() => {
          reconnectRef.current();
        }, 3000);
      });

    clientRef.current = client;
  }, [onMachineUpdate, onBookingUpdate, 
      onWalletUpdate]);

  useEffect(() => {
    reconnectRef.current = connect;
  }, [connect]);

  useEffect(() => {
    connect();

    return () => {
      if (clientRef.current && 
          connectedRef.current) {
        clientRef.current.disconnect(() => {
          console.log('WebSocket disconnected');
        });
        connectedRef.current = false;
      }
    };
  }, [connect]);
}