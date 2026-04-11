import { useNavigate } from 'react-router-dom';
import { dashboardStyles as s } from
  '../styles/DashboardPage.styles';

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
        <p className={s.roleText}>Role: {role}</p>

        {/* Navigate to Machines */}
        <button
          onClick={() => navigate('/machines')}
          className="w-full bg-[#9333EA] text-white 
                     px-6 py-3 rounded-xl 
                     font-semibold mb-3
                     hover:bg-purple-700 
                     transition-all"
        >
          🫧 Go to Machine Management
        </button>

        <button
          onClick={handleLogout}
          className="w-full border border-gray-200 
                     text-gray-600 px-6 py-3 
                     rounded-xl font-semibold
                     hover:bg-gray-50 transition-all"
        >
          Logout
        </button>
      </div>
    </div>
  );
}