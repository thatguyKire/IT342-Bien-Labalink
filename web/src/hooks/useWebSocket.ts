import { useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { API_BASE_URL } from '../lib/config';
import { getSession } from '../lib/session';

interface WebSocketMessage {
  type: string;
  message: string;
  data: unknown;
  timestamp: string;
}

interface Handlers {
  onMachineUpdate?: (data: unknown) => void;
  onBookingUpdate?: (data: unknown) => void;
}

const RECONNECT_DELAY_MS = 5000;

/**
 * Live updates over STOMP. The JWT is sent in the CONNECT frame and the
 * server checks it (and what you may subscribe to).
 *
 * Handlers are kept in a ref so passing new inline callbacks on every
 * render does NOT tear down and re-open the socket (the old hook did,
 * reconnecting on every render).
 */
export function useWebSocket(handlers: Handlers): boolean {
  const handlersRef = useRef(handlers);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    handlersRef.current = handlers;
  });

  useEffect(() => {
    let client: Stomp.Client | null = null;
    let retryTimer: number | undefined;
    let cancelled = false;

    const parse = (body: string): unknown => {
      try {
        return (JSON.parse(body) as WebSocketMessage).data;
      } catch {
        return null;
      }
    };

    const connect = () => {
      const session = getSession();
      if (!session || cancelled) return;

      const stomp = Stomp.over(new SockJS(`${API_BASE_URL}/ws`));
      stomp.debug = () => {};
      client = stomp;

      stomp.connect(
        { Authorization: `Bearer ${session.token}` },
        () => {
          if (cancelled) {
            stomp.disconnect(() => {});
            return;
          }
          setConnected(true);
          stomp.subscribe('/topic/machines', (msg) =>
            handlersRef.current.onMachineUpdate?.(parse(msg.body)));
          if (session.role === 'ATTENDANT') {
            stomp.subscribe('/topic/bookings', (msg) =>
              handlersRef.current.onBookingUpdate?.(parse(msg.body)));
          }
        },
        () => {
          setConnected(false);
          if (!cancelled) {
            retryTimer = window.setTimeout(connect, RECONNECT_DELAY_MS);
          }
        },
      );
    };

    connect();

    return () => {
      cancelled = true;
      window.clearTimeout(retryTimer);
      if (client?.connected) {
        client.disconnect(() => {});
      } else {
        client?.ws?.close();
      }
    };
  }, []);

  return connected;
}
