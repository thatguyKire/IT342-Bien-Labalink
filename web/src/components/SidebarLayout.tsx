import { useNavigate, useLocation } from 'react-router-dom';
import { CreditCard, LogOut, ReceiptText, Users, WashingMachine } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import LogoMark from './LogoMark';
import { clearSession, getSession } from '../lib/session';
import { logout } from '../services/authService';

const navItems: { label: string; path: string; icon: LucideIcon }[] = [
  { label: 'Machines', path: '/machines', icon: WashingMachine },
  { label: 'Bookings', path: '/bookings', icon: ReceiptText },
  { label: 'Transactions', path: '/transactions', icon: CreditCard },
  { label: 'Staff', path: '/staff', icon: Users },
];

export default function SidebarLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const navigate = useNavigate();
  const location = useLocation();
  const username = getSession()?.username || 'Attendant';

  const handleLogout = async () => {
    await logout();
    clearSession();
    navigate('/login', { replace: true });
  };

  const isActive = (path: string) => location.pathname === path;

  return (
    <div className="flex min-h-screen bg-gray-50">

      {/* Sidebar: the deep-water panel is the dashboard's one bold surface */}
      <aside className="
        w-64 min-h-screen bg-brand-900 text-white
        flex flex-col fixed left-0 top-0 z-10
      ">
        <div className="px-5 py-6 flex items-center gap-3">
          <LogoMark size={38} />
          <div>
            <p className="text-lg font-bold leading-tight">LabaLink</p>
            <p className="text-xs text-brand-200">Attendant dashboard</p>
          </div>
        </div>

        <nav className="flex-1 px-3 py-2" aria-label="Main">
          {navItems.map(({ label, path, icon: Icon }) => {
            const active = isActive(path);
            return (
              <button
                key={path}
                onClick={() => navigate(path)}
                aria-current={active ? 'page' : undefined}
                className={`
                  relative w-full flex items-center gap-3
                  px-3 py-2.5 rounded-xl mb-1
                  text-sm font-semibold text-left
                  transition-colors duration-150
                  ${active
                    ? 'bg-white/10 text-white'
                    : 'text-brand-200 hover:bg-white/5 hover:text-white'}
                `}
              >
                {active && (
                  <span
                    aria-hidden="true"
                    className="absolute left-0 top-2 bottom-2 w-1 rounded-full bg-accent-400 animate-fade"
                  />
                )}
                <Icon size={18} strokeWidth={2} aria-hidden="true" />
                {label}
              </button>
            );
          })}
        </nav>

        <div className="mx-3 mb-4 p-3 rounded-2xl bg-white/5 flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-accent-400 text-brand-900
                          flex items-center justify-center
                          font-bold text-sm shrink-0">
            {username.charAt(0).toUpperCase()}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold truncate">{username}</p>
            <p className="text-xs text-brand-200">Shop attendant</p>
          </div>
          <button
            onClick={handleLogout}
            title="Log out"
            aria-label="Log out"
            className="p-2 rounded-lg text-brand-200 hover:text-white
                       hover:bg-white/10 transition-colors"
          >
            <LogOut size={18} aria-hidden="true" />
          </button>
        </div>
      </aside>

      <main className="flex-1 ml-64 min-h-screen">
        {children}
      </main>

    </div>
  );
}
