import { Outlet, NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useState, useEffect } from 'react';
import {
  LayoutDashboard,
  Settings,
  LogOut,
  Monitor,
  Menu,
  X,
  Key,
  Clock
} from 'lucide-react';

const Layout = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [version, setVersion] = useState('');
  const [serverStartTime, setServerStartTime] = useState(null);
  const [uptime, setUptime] = useState('');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const fetchVersion = async () => {
      try {
        const response = await fetch('/api/version');
        const data = await response.json();
        setVersion(data.version);
        setServerStartTime(data.serverStartTime);
      } catch (error) {
        setVersion('');
      }
    };
    fetchVersion();
  }, []);

  // Update uptime every second
  useEffect(() => {
    if (!serverStartTime) return;

    const formatUptime = () => {
      const elapsed = Date.now() - serverStartTime;
      const seconds = Math.floor(elapsed / 1000) % 60;
      const minutes = Math.floor(elapsed / (1000 * 60)) % 60;
      const hours = Math.floor(elapsed / (1000 * 60 * 60)) % 24;
      const days = Math.floor(elapsed / (1000 * 60 * 60 * 24));

      const parts = [];
      if (days > 0) parts.push(`${days}d`);
      if (hours > 0) parts.push(`${hours}h`);
      if (minutes > 0) parts.push(`${minutes}min`);
      parts.push(`${seconds}sec`);

      return parts.join(' ');
    };

    setUptime(formatUptime());
    const interval = setInterval(() => setUptime(formatUptime()), 1000);
    return () => clearInterval(interval);
  }, [serverStartTime]);

  // Close mobile menu on route change
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  // Prevent background scrolling when mobile menu is open
  useEffect(() => {
    document.body.style.overflow = mobileMenuOpen ? 'hidden' : '';
    return () => { document.body.style.overflow = ''; };
  }, [mobileMenuOpen]);

  const handleChangePassword = () => {
    navigate('/change-password');
    setMobileMenuOpen(false);
  };

  const navigation = [
    { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/settings', icon: Settings, label: 'Settings' },
  ];

  const navLinks = navigation.map(({ to, icon: Icon, label }) => (
    <NavLink
      key={to}
      to={to}
      className={({ isActive }) =>
        `flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${
          isActive
            ? 'bg-primary-50 text-primary-700 font-medium'
            : 'text-surface-600 hover:bg-surface-50 hover:text-surface-900'
        }`
      }
    >
      <Icon className="h-5 w-5 flex-shrink-0" />
      <span>{label}</span>
    </NavLink>
  ));

  return (
    <div className="min-h-screen bg-surface-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-surface-200 sticky top-0 z-40">
        <div className="px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Left side - brand */}
            <div className="flex items-center gap-2">
              <Monitor className="h-8 w-8 text-primary-600" />
              <span className="text-xl font-bold text-surface-900">FS-Monitor</span>
              <span className="text-sm text-surface-500 hidden sm:block">Server Monitoring</span>
              {version && (
                <span className="text-xs text-primary-600 font-medium" title="Version">
                  v.{version}
                </span>
              )}
            </div>

            {/* Right side - uptime, change password, logout */}
            <div className="flex items-center gap-2">
              {uptime && (
                <div className="flex items-center gap-1.5 text-xs text-surface-500" title="App uptime">
                  <Clock className="h-3.5 w-3.5" />
                  <span className="hidden md:block">Uptime: {uptime}</span>
                </div>
              )}

              <span className="hidden sm:block text-xs text-surface-400">-</span>

              <button
                onClick={handleChangePassword}
                className="hidden sm:flex items-center gap-2 text-surface-600 hover:text-surface-900 transition-colors p-2 rounded-lg hover:bg-surface-100"
                title="Change Password"
              >
                <Key className="h-4 w-4" />
                <span className="text-sm">Change Passwd</span>
              </button>

              <span className="hidden sm:block text-xs text-surface-400">-</span>

              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="sm:hidden p-2 rounded-lg text-surface-600 hover:text-surface-900 hover:bg-surface-100 transition-colors"
              >
                {mobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
              </button>

              <button
                onClick={logout}
                className="hidden sm:flex items-center gap-2 text-surface-600 hover:text-surface-900 transition-colors p-2 rounded-lg hover:bg-surface-100"
              >
                <LogOut className="h-5 w-5" />
                <span>Logout</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Mobile menu overlay */}
      {mobileMenuOpen && (
        <div className="fixed inset-0 z-50 sm:hidden">
          <div
            className="fixed inset-0 bg-black/25"
            onClick={() => setMobileMenuOpen(false)}
            style={{ touchAction: 'none' }}
          />
          <nav
            className="fixed top-0 left-0 bottom-0 w-64 bg-white shadow-xl flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between p-4 border-b border-surface-200 flex-shrink-0">
              <div className="flex items-center gap-2">
                <Monitor className="h-8 w-8 text-primary-600" />
                <span className="text-xl font-bold text-surface-900">FS-Monitor</span>
              </div>
              <button
                onClick={() => setMobileMenuOpen(false)}
                className="p-2 rounded-lg text-surface-600 hover:text-surface-900 hover:bg-surface-100"
              >
                <X className="h-6 w-6" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-2">
              {navLinks}

              <div className="border-t border-surface-200 pt-4 mt-4">
                <button
                  onClick={handleChangePassword}
                  className="flex items-center gap-3 w-full px-3 py-3 rounded-lg text-surface-600 hover:bg-surface-50 hover:text-surface-900 transition-colors"
                >
                  <Key className="h-5 w-5" />
                  <span>Change Password</span>
                </button>
                <button
                  onClick={logout}
                  className="flex items-center gap-3 w-full px-3 py-3 rounded-lg text-surface-600 hover:bg-surface-50 hover:text-surface-900 transition-colors"
                >
                  <LogOut className="h-5 w-5" />
                  <span>Logout</span>
                </button>
              </div>
            </div>
          </nav>
        </div>
      )}

      <div className="flex">
        {/* Desktop sidebar */}
        <nav className="hidden sm:block w-64 flex-shrink-0 bg-white shadow-sm min-h-[calc(100vh-4rem)] border-r border-surface-200">
          <div className="p-4 space-y-2">{navLinks}</div>
        </nav>

        {/* Main Content */}
        <main className="flex-1 overflow-x-hidden min-w-0">
          <div className="w-full max-w-[1600px] mx-auto p-2 sm:p-4 lg:p-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

export default Layout;
