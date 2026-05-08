import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { oauthStyles as s } from
  '../styles/OAuth2CallbackPage.styles';

export default function OAuth2CallbackPage() {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const params = new URLSearchParams(
      location.search
    );

    const token = params.get('token');
    const username = params.get('username');
    const role = params.get('role');
    const email = params.get('email');

    if (token) {
      localStorage.setItem('token', token);
      localStorage.setItem(
        'username', username || '');
      localStorage.setItem('role', role || '');
      localStorage.setItem('email', email || '');

      // Redirect based on role
      if (role === 'ATTENDANT') {
        navigate('/machines', { replace: true });
      } else {
        // CUSTOMER on web — still go to dashboard
        // but in real app mobile handles customers
        navigate('/machines', { replace: true });
      }
    } else {
      navigate('/login', { replace: true });
    }
  }, [navigate, location.search]);

  return (
    <div className={s.page}>
      <div className={s.card}>
        <div className={s.icon}>🫧</div>
        <h1 className={s.title}>
          Signing you in...
        </h1>
        <p className={s.subtitle}>
          Please wait while we verify
          your Google account
        </p>
      </div>
    </div>
  );
}