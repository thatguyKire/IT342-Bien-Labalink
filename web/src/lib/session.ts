// The access token lives in sessionStorage (cleared when the tab closes)
// rather than localStorage, and is short-lived (60 min by default) and
// revocable server-side via /api/auth/logout. See DECISIONS.md (ADR-006).

export type Role = 'CUSTOMER' | 'ATTENDANT';

export interface Session {
  token: string;
  expiresAt: number;
  userId: number;
  username: string;
  email: string;
  role: Role;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  role: Role;
  email: string;
  username: string;
  userId: number;
}

const KEY = 'labalink.session';

export function saveSession(response: LoginResponse): Session {
  const session: Session = {
    token: response.accessToken,
    expiresAt: Date.now() + response.expiresIn * 1000,
    userId: response.userId,
    username: response.username,
    email: response.email,
    role: response.role,
  };
  try {
    sessionStorage.setItem(KEY, JSON.stringify(session));
  } catch {
    // Storage blocked (private mode): the session lasts for this page only.
  }
  return session;
}

export function getSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return null;
    const session = JSON.parse(raw) as Session;
    if (!session.token || Date.now() >= session.expiresAt) {
      sessionStorage.removeItem(KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

export function clearSession(): void {
  try {
    sessionStorage.removeItem(KEY);
    // Remove values written by older versions of the dashboard.
    ['token', 'role', 'username', 'email', 'userId']
      .forEach((k) => localStorage.removeItem(k));
  } catch {
    // ignore
  }
}
