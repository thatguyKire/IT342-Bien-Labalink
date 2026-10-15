import axios from 'axios';
import { API_BASE_URL } from './config';
import { clearSession, getSession } from './session';

export const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const session = getSession();
  if (session) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Expired or revoked token: drop it and send the user to sign in.
    if (axios.isAxiosError(error)
        && error.response?.status === 401
        && getSession()) {
      clearSession();
      window.location.assign('/login?expired=1');
    }
    return Promise.reject(error);
  },
);

/** Human-readable message from an API error (server messages are safe). */
export function errorMessage(
  err: unknown,
  fallback = 'Something went wrong. Please try again.',
): string {
  if (axios.isAxiosError(err)) {
    const message = (err.response?.data as { message?: unknown } | undefined)
      ?.message;
    if (typeof message === 'string' && message.length > 0) return message;
    if (!err.response) return 'Cannot reach the server. Check your connection.';
  }
  return fallback;
}
