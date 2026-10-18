import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { googleLoginUrl, login } from '../services/authService';
import { errorMessage } from '../lib/api';
import { PRIVACY_URL, TERMS_URL } from '../lib/config';
import { saveSession } from '../lib/session';
import { loginStyles as s } from '../styles/LoginPage.styles';

const REDIRECT_ERRORS: Record<string, string> = {
  google_cancelled: 'Google sign in was cancelled. Please try again.',
  google_unverified:
    'Your Google email address is not verified. Please use another account.',
  account_disabled: 'This account is disabled. Contact your administrator.',
  not_attendant:
    'This dashboard is for shop attendants. Customers, please use the '
    + 'LabaLink mobile app.',
};

const NOT_ATTENDANT_MESSAGE = REDIRECT_ERRORS.not_attendant;

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const redirectError = params.get('error');
  const expired = params.get('expired') === '1';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(
    (redirectError && REDIRECT_ERRORS[redirectError])
      || (expired ? 'Your session expired. Please sign in again.' : ''));
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please fill in all fields');
      return;
    }
    try {
      setLoading(true);
      setError('');
      const response = await login(email.trim(), password);
      if (response.role !== 'ATTENDANT') {
        setError(NOT_ATTENDANT_MESSAGE);
        return;
      }
      saveSession(response);
      navigate('/machines', { replace: true });
    } catch (err) {
      setError(errorMessage(err, 'Invalid email or password'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={s.page}>

      {/* Logo */}
      <div className={s.logoWrapper}>
        <div className={s.logoBox}>
          <span className={s.logoIcon}>🫧</span>
        </div>
        <h1 className={s.logoTitle}>LabaLink</h1>
        <p className={s.logoSubtitle}>
          Smart Laundry Management System
        </p>
      </div>

      {/* Card */}
      <form className={s.card} onSubmit={handleLogin} noValidate>
        <h2 className={s.cardTitle}>Attendant Sign In</h2>
        <p className={s.cardSubtitle}>
          Manage machines, bookings and transactions
        </p>

        {error && (
          <div className={s.errorBox} role="alert">
            {error}
          </div>
        )}

        {/* Email */}
        <div className="mb-4">
          <label className={s.inputLabel} htmlFor="email">
            Email
          </label>
          <div className={s.inputWrapper}>
            <span className={s.inputIcon} aria-hidden="true">✉️</span>
            <input
              id="email"
              type="email"
              autoComplete="username"
              placeholder="Enter your email"
              value={email}
              maxLength={254}
              onChange={(e) => setEmail(e.target.value)}
              className={s.input}
            />
          </div>
        </div>

        {/* Password */}
        <div className="mb-2">
          <label className={s.inputLabel} htmlFor="password">
            Password
          </label>
          <div className={s.inputWrapper}>
            <span className={s.inputIcon} aria-hidden="true">🔒</span>
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="Enter your password"
              value={password}
              maxLength={128}
              onChange={(e) => setPassword(e.target.value)}
              className={s.input}
            />
            <button
              type="button"
              className={s.eyeIcon}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              onClick={() => setShowPassword(!showPassword)}
            >
              {showPassword ? '🙈' : '👁️'}
            </button>
          </div>
        </div>

        <button type="submit" disabled={loading} className={s.loginButton}>
          {loading ? 'Signing in...' : 'Sign In →'}
        </button>

        {/* Divider */}
        <div className={s.dividerWrapper}>
          <div className={s.dividerLine} />
          <span className={s.dividerText}>OR CONTINUE WITH</span>
          <div className={s.dividerLine} />
        </div>

        <a href={googleLoginUrl} className={s.googleButton}>
          <svg className={s.googleIcon} viewBox="0 0 48 48" aria-hidden="true">
            <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.4-.4-3.5z" />
            <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z" />
            <path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-8l-6.5 5C9.5 39.6 16.2 44 24 44z" />
            <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.4-.4-3.5z" />
          </svg>
          Continue with Google
        </a>

        <p className={s.registerText}>
          Attendant accounts are created by an existing attendant.
        </p>
      </form>

      <p className={s.footer}>
        © 2026 LabaLink ·{' '}
        <a href={TERMS_URL} target="_blank" rel="noopener noreferrer" className={s.footerLink}>
          Terms of Service
        </a>{' '}
        ·{' '}
        <a href={PRIVACY_URL} target="_blank" rel="noopener noreferrer" className={s.footerLink}>
          Privacy Policy
        </a>
      </p>
    </div>
  );
}
