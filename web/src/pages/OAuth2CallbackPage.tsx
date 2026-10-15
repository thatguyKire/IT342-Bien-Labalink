import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { exchangeOAuth2Code } from '../services/authService';
import { saveSession } from '../lib/session';
import { oauthStyles as s } from '../styles/OAuth2CallbackPage.styles';

/**
 * Receives a one-time code in the URL fragment after Google sign-in and
 * swaps it for a JWT. The fragment is never sent to any server, and it is
 * wiped from the address bar immediately.
 */
export default function OAuth2CallbackPage() {
  const navigate = useNavigate();
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return; // StrictMode runs effects twice in dev
    started.current = true;

    const code = new URLSearchParams(window.location.hash.slice(1))
      .get('code');
    window.history.replaceState(null, '', window.location.pathname);

    if (!code) {
      navigate('/login?error=google_cancelled', { replace: true });
      return;
    }

    exchangeOAuth2Code(code)
      .then((response) => {
        if (response.role !== 'ATTENDANT') {
          navigate('/login?error=not_attendant', { replace: true });
          return;
        }
        saveSession(response);
        navigate('/machines', { replace: true });
      })
      .catch(() => navigate('/login?error=google_cancelled', {
        replace: true,
      }));
  }, [navigate]);

  return (
    <div className={s.page}>
      <div className={s.card}>
        <div className={s.icon}>🫧</div>
        <h1 className={s.title}>Signing you in...</h1>
        <p className={s.subtitle}>
          Please wait while we verify your Google account
        </p>
      </div>
    </div>
  );
}
