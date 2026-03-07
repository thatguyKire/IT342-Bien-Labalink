import { useState } from 'react';
import { login } from '../services/authService';
import { useNavigate, Link } from 'react-router-dom';
import { loginStyles as s } from '../styles/LoginPage.styles';

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!email || !password) {
      setError('Please fill in all fields');
      return;
    }
    try {
      setLoading(true);
      setError('');
      const response = await login({ email, password });
      localStorage.setItem('token', response.accessToken);
      localStorage.setItem('role', response.role);
      localStorage.setItem('username', response.username);
      navigate('/dashboard');
    } catch {
      setError('Invalid email or password');
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
      <div className={s.card}>
        <h2 className={s.cardTitle}>Welcome Back</h2>
        <p className={s.cardSubtitle}>
          Sign in to your account to manage your laundry
        </p>

        {/* Error */}
        {error && (
          <div className={s.errorBox}>{error}</div>
        )}

        {/* Email */}
        <div className="mb-4">
          <label className={s.inputLabel}>Email</label>
          <div className={s.inputWrapper}>
            <span className={s.inputIcon}>✉️</span>
            <input
              type="email"
              placeholder="Enter your email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={s.input}
            />
          </div>
        </div>

        {/* Password */}
        <div className="mb-2">
          <div className={s.passwordRow}>
            <label className={s.inputLabel}>
              Password
            </label>
            <span className={s.forgotPassword}>
              Forgot Password?
            </span>
          </div>
          <div className={s.inputWrapper}>
            <span className={s.inputIcon}>🔒</span>
            <input
              type={showPassword ? 'text' : 'password'}
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={s.input}
            />
            <span
              className={s.eyeIcon}
              onClick={() => setShowPassword(!showPassword)}
            >
              {showPassword ? '🙈' : '👁️'}
            </span>
          </div>
        </div>

        {/* Login Button */}
        <button
          onClick={handleLogin}
          disabled={loading}
          className={s.loginButton}
        >
          {loading ? 'Logging in...' : 'Login →'}
        </button>

        {/* Divider */}
        <div className={s.dividerWrapper}>
          <div className={s.dividerLine} />
          <span className={s.dividerText}>
            OR CONTINUE WITH
          </span>
          <div className={s.dividerLine} />
        </div>

        {/* Google Button */}
        <button className={s.googleButton}>
          <img
            src="https://www.google.com/favicon.ico"
            alt="Google"
            className={s.googleIcon}
          />
          Continue with Google
        </button>

        {/* Register Link */}
        <p className={s.registerText}>
          Don't have an account?{' '}
          <Link to="/register" className={s.registerLink}>
            Register here
          </Link>
        </p>
      </div>

      {/* Footer */}
      <p className={s.footer}>
        © 2026 LabaLink. All rights reserved.
      </p>
    </div>
  );
}