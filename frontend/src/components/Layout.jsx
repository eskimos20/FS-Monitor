import { Outlet, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  Home, 
  Settings, 
  LogOut, 
  User, 
  Monitor
} from 'lucide-react';

const Layout = () => {
  const { user, logout } = useAuth();
  const location = useLocation();

  const handleLogout = () => {
    logout();
  };

  const isActive = (path) => {
    return location.pathname === path;
  };

  const navigation = [
    { name: 'Dashboard', href: '/dashboard', icon: Home },
    { name: 'Settings', href: '/settings', icon: Settings },
  ];

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <div className="w-64 bg-white shadow-sm border-r border-gray-200">
        <div className="p-6">
          {/* Logo */}
          <div className="flex items-center">
            <Monitor className="h-8 w-8 text-primary-600" />
            <h1 className="ml-3 text-xl font-semibold text-gray-900">FS-Monitor</h1>
          </div>
        </div>
        
        {/* Navigation */}
        <nav className="px-4 pb-6 space-y-1">
          {navigation.map((item) => {
            const Icon = item.icon;
            return (
              <Link
                key={item.name}
                to={item.href}
                className={`flex items-center px-3 py-2 text-sm font-medium rounded-md transition-colors ${
                  isActive(item.href)
                    ? 'text-primary-600 bg-primary-50'
                    : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
                }`}
              >
                <Icon className="h-4 w-4 mr-3" />
                {item.name}
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Top Header */}
        <header className="bg-white shadow-sm border-b border-gray-200">
          <div className="px-6 py-4 flex justify-end items-center">
            <div className="flex items-center space-x-3">
              <div className="flex items-center space-x-2">
                <User className="h-5 w-5 text-gray-400" />
                <span className="text-sm font-medium text-gray-700">{user?.username}</span>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center space-x-1 p-2 text-gray-400 hover:text-red-600 transition-colors"
              >
                <LogOut className="h-5 w-5" />
                <span className="text-sm">Logout</span>
              </button>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 px-6 py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
