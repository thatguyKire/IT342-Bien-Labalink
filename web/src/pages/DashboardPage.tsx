import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useWebSocket } from '../hooks/useWebSocket';
import { dashboardStyles as s } from
  '../styles/DashboardPage.styles';

export default function DashboardPage() {
  const navigate = useNavigate();
  const username = 
    localStorage.getItem('username');
  const [wsConnected, setWsConnected] = 
    useState(false);

  useWebSocket({
    onMachineUpdate: () => {
      setWsConnected(true);
    },
    onBookingUpdate: () => {
      setWsConnected(true);
    },
  });

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  return (
    <div className={s.page}>
      <div className={s.card}>

        {/* WebSocket Status */}
        <div className="flex items-center 
                        justify-center gap-2 
                        mb-4">
          <div className={`w-2 h-2 rounded-full 
            ${wsConnected 
              ? 'bg-green-500 animate-pulse' 
              : 'bg-gray-400'}`}
          />
          <span className="text-xs text-gray-400">
            {wsConnected
              ? 'WebSocket Connected'
              : 'Connecting...'}
          </span>
        </div>

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

        {/* Removed role display — 
            web is always ATTENDANT */}
        <p className="text-sm text-[#9333EA] 
                      font-medium mb-6">
          Shop Attendant Dashboard
        </p>

        <button
          onClick={() => navigate('/machines')}
          className="w-full bg-[#9333EA] 
                     text-white px-6 py-3 
                     rounded-xl font-semibold 
                     mb-3 hover:bg-purple-700 
                     transition-all"
        >
          🫧 Machine Management
        </button>

        <button
          onClick={() => navigate('/bookings')}
          className="w-full border 
                     border-[#9333EA]
                     text-[#9333EA] px-6 py-3
                     rounded-xl font-semibold 
                     mb-3 hover:bg-purple-50 
                     transition-all"
        >
          📋 Bookings & Transactions
        </button>

        <button
          onClick={() => navigate('/wallet')}
          className="w-full border 
                     border-[#9333EA]
                     text-[#9333EA] px-6 py-3
                     rounded-xl font-semibold 
                     mb-3 hover:bg-purple-50 
                     transition-all"
        >
          💰 Wallet Top-Up
        </button>

        <button
          onClick={handleLogout}
          className="w-full border 
                     border-gray-200
                     text-gray-600 px-6 py-3
                     rounded-xl font-semibold
                     hover:bg-gray-50 
                     transition-all"
        >
          Logout
        </button>
      </div>
    </div>
  );
}