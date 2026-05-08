import { useNavigate, useLocation } from 'react-router-dom';

const navItems = [
  { label: 'Machines', path: '/machines', icon: '🫧' },
  { label: 'Bookings', path: '/bookings', icon: '📋' },
  { label: 'Transactions', path: '/wallet', icon: '💳' },
];

const settingsItems: { label: string; path: string; icon: string }[] = [];

export default function SidebarLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const navigate = useNavigate();
  const location = useLocation();
  const username = localStorage.getItem('username') || 'Attendant';

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  const isActive = (path: string) =>
    location.pathname === path;

  return (
    <div className="flex min-h-screen bg-[#F5F3FF]">

      {/* Sidebar */}
      <aside className="
        w-60 min-h-screen bg-white
        border-r border-gray-100
        flex flex-col fixed left-0 top-0
        shadow-sm z-10
      ">
        {/* Logo */}
        <div className="px-6 py-5 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <span className="text-2xl">🫧</span>
            <span className="text-xl font-bold text-[#9333EA]">
              LabaLink
            </span>
          </div>
        </div>

        {/* Main Menu */}
        <nav className="flex-1 px-3 py-4">
          <p className="text-xs font-semibold text-gray-400 
                        uppercase tracking-wider px-3 mb-2">
            Main Menu
          </p>
          {navItems.map((item) => (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              className={`
                w-full flex items-center gap-3
                px-3 py-2.5 rounded-xl mb-1
                text-sm font-medium transition-all
                text-left
                ${isActive(item.path)
                  ? 'bg-[#EDE9FE] text-[#9333EA]'
                  : 'text-gray-500 hover:bg-gray-50 hover:text-gray-700'}
              `}
            >
              <span className="text-base">
                {item.icon}
              </span>
              {item.label}
            </button>
          ))}

          {/* Settings Section (hidden if no items) */}
          {settingsItems.length > 0 && (
            <>
              <p className="text-xs font-semibold 
                            text-gray-400 uppercase 
                            tracking-wider px-3 
                            mb-2 mt-6">
                Settings
              </p>
              {settingsItems.map((item) => (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  className={`
                    w-full flex items-center gap-3
                    px-3 py-2.5 rounded-xl mb-1
                    text-sm font-medium transition-all
                    text-left
                    ${isActive(item.path)
                      ? 'bg-[#EDE9FE] text-[#9333EA]'
                      : 'text-gray-500 hover:bg-gray-50 hover:text-gray-700'}
                  `}
                >
                  <span className="text-base">
                    {item.icon}
                  </span>
                  {item.label}
                </button>
              ))}
            </>
          )}
        </nav>

        {/* User Profile Footer */}
        <div className="px-4 py-4 border-t border-gray-100">
          <div className="flex items-center gap-3">
            {/* Avatar */}
            <div className="w-9 h-9 rounded-full 
                            bg-[#EDE9FE] flex items-center 
                            justify-center text-[#9333EA] 
                            font-bold text-sm flex-shrink-0">
              {username.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold 
                            text-gray-800 truncate">
                {username}
              </p>
              <p className="text-xs text-[#9333EA] 
                            font-medium">
                Shop Attendant
              </p>
            </div>
            {/* Logout button */}
            <button
              onClick={handleLogout}
              title="Logout"
              className="text-gray-400 hover:text-red-500 
                         transition-colors text-lg"
            >
              ↪
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 ml-60 min-h-screen">
        {children}
      </main>

    </div>
  );
}