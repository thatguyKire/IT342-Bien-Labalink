import { api } from '../lib/api';
import { API_BASE_URL } from '../lib/config';
import type { LoginResponse } from '../lib/session';

export const login = async (
  email: string,
  password: string,
): Promise<LoginResponse> => {
  const response = await api.post<LoginResponse>(
    '/auth/login', { email, password });
  return response.data;
};

export const exchangeOAuth2Code = async (
  code: string,
): Promise<LoginResponse> => {
  const response = await api.post<LoginResponse>(
    '/auth/oauth2/exchange', { code });
  return response.data;
};

/** Revokes the token server-side. Failures are ignored: we log out anyway. */
export const logout = async (): Promise<void> => {
  try {
    await api.post('/auth/logout');
  } catch {
    // ignore
  }
};

export const googleLoginUrl = `${API_BASE_URL}/oauth2/authorization/google`;
