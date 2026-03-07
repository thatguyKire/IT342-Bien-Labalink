import { useNavigate } from 'react-router-dom';
import { dashboardStyles as s } from '../styles/DashboardPage.styles';

export default function DashboardPage() {
  const navigate = useNavigate();
  const username = localStorage.getItem('username');
  const role = localStorage.getItem('role');

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  return (
    <div className={s.page}>
      <div className={s.card}>

        <div className={s.icon}>🫧</div>

        <h1 className={s.title}>
          Welcome to LabaLink!
        </h1>

        <p className={s.usernameText}>
          Hello,{' '}
          <span className={s.usernameSpan}>
            {username}
          </span>
        </p>

        <p className={s.roleText}>
          Role: {role}
        </p>

        <p className={s.description}>
          Login successful! Full dashboard 
          coming in Phase 4 of development.
        </p>

        <button
          onClick={handleLogout}
          className={s.logoutButton}
        >
          Logout
        </button>

      </div>
    </div>
  );
}