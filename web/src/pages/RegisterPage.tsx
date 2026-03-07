import { useState } from 'react';
import axios from 'axios';
import { register } from '../services/authService';
import { useNavigate, Link } from 'react-router-dom';
import { registerStyles as s } from '../styles/RegisterPage.styles';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRegister = async () => {
    if (!username || !email || !password) {
      setError('Please fill in all fields');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    if (password.length < 8) {
      setError(
        'Password must be at least 8 characters'
      );
      return;
    }
    try {
      setLoading(true);
      setError('');
      await register({ username, email, password });
      navigate('/login');
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        setError('Email already exists');
      } else {
        setError('Registration failed. Try again.');
      }
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
        <h2 className={s.cardTitle}>Create Account</h2>
        <p className={s.cardSubtitle}>
          Join LabaLink and manage your laundry easily
        </p>

        {/* Error */}
        {error && (
          <div className={s.errorBox}>{error}</div>
        )}

        {/* Username */}
        <div className={s.inputGroup}>
          <label className={s.inputLabel}>
            Username
          </label>
          <div className={s.inputWrapper}>
            <span className={s.inputIcon}>👤</span>
            <input
              type="text"
              placeholder="Enter your username"
              value={username}
              onChange={(e) => 
                setUsername(e.target.value)}
              className={s.input}
            />
          </div>
        </div>

        {/* Email */}
        <div className={s.inputGroup}>
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
        <div className={s.inputGroup}>
          <label className={s.inputLabel}>
            Password
          </label>
          <div className={s.inputWrapper}>
            <span className={s.inputIcon}>🔒</span>
            <input
              type={showPassword ? 'text' : 'password'}
              placeholder="Min 8 characters"
              value={password}
              onChange={(e) => 
                setPassword(e.target.value)}
              className={s.input}
            />
            <span
              className={s.eyeIcon}
              onClick={() => 
                setShowPassword(!showPassword)}
            >
              {showPassword ? '🙈' : '👁️'}
            </span>
          </div>
        </div>

        {/* Confirm Password */}
        <div className={s.inputGroup}>
          <label className={s.inputLabel}>
            Confirm Password
          </label>
          <div className={s.inputWrapper}>
            <span className={s.inputIcon}>🔒</span>
            <input
              type="password"
              placeholder="Confirm your password"
              value={confirmPassword}
              onChange={(e) => 
                setConfirmPassword(e.target.value)}
              className={s.input}
            />
          </div>
        </div>

        {/* Register Button */}
        <button
          onClick={handleRegister}
          disabled={loading}
          className={s.registerButton}
        >
          {loading ? 'Creating account...' : 
                     'Create Account →'}
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

        {/* Login Link */}
        <p className={s.loginText}>
          Already have an account?{' '}
          <Link to="/login" className={s.loginLink}>
            Login here
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