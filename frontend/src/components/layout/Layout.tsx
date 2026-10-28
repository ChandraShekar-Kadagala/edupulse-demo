import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Activity, LayoutDashboard, Users, AlertTriangle, LogOut, FileText, Upload } from 'lucide-react';
import { cn } from '../../utils/utils';
import { ThemeToggle } from '../ui/ThemeToggle';

export default function Layout() {
  const { user, logout } = useAuth();
  const [networkError, setNetworkError] = React.useState<string | null>(null);

  React.useEffect(() => {
    const handleNetworkError = (e: Event) => {
      const customEvent = e as CustomEvent;
      setNetworkError(customEvent.detail);
      setTimeout(() => setNetworkError(null), 5000);
    };
    window.addEventListener('network-error', handleNetworkError);
    return () => window.removeEventListener('network-error', handleNetworkError);
  }, []);

  const navItems = [
    { to: user?.role === 'STUDENT' ? '/students/' + user.id : '/dashboard', icon: LayoutDashboard, label: user?.role === 'STUDENT' ? 'My Profile' : 'Dashboard', roles: ['ADMIN', 'FACULTY', 'STUDENT'] },
    { to: '/students', icon: Users, label: 'Students', roles: ['ADMIN', 'FACULTY'] },
    { to: '/interventions', icon: AlertTriangle, label: 'Interventions', roles: ['ADMIN', 'FACULTY'] },
    { to: '/import', icon: Upload, label: 'Data Import', roles: ['ADMIN', 'FACULTY'] },
    { to: '/integration', icon: FileText, label: 'Account Linking', roles: ['STUDENT', 'ADMIN', 'FACULTY'] },
  ];

  const allowedNavItems = navItems.filter(item => user && item.roles.includes(user.role));

  return (
    <div className="flex h-screen bg-gray-50 dark:bg-gray-950 text-gray-900 dark:text-gray-100 overflow-hidden transition-colors duration-200">
      {/* Sidebar */}
      <aside className="w-64 flex flex-col bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 transition-colors duration-200">
        <div className="h-16 flex items-center px-6 border-b border-gray-200 dark:border-gray-800">
          <Activity className="h-6 w-6 text-indigo-600 dark:text-indigo-500 mr-2" />
          <span className="font-bold text-lg tracking-tight text-gray-900 dark:text-white">EDUPULSE</span>
        </div>
        
        <div className="flex-1 overflow-y-auto py-6 px-3">
          <div className="space-y-1">
            {allowedNavItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    "flex items-center px-3 py-2 text-sm font-medium rounded-md transition-colors",
                    isActive
                      ? "bg-indigo-50 dark:bg-indigo-600/10 text-indigo-600 dark:text-indigo-400 font-semibold"
                      : "text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-gray-100"
                  )
                }
              >
                <item.icon className="h-5 w-5 mr-3" />
                {item.label}
              </NavLink>
            ))}
          </div>
        </div>
        
        <div className="p-4 border-t border-gray-200 dark:border-gray-800">
          <div className="flex items-center mb-4 px-2">
            <div className="h-8 w-8 rounded-full bg-indigo-600 flex items-center justify-center text-white font-bold text-sm">
              {user?.name?.charAt(0).toUpperCase()}
            </div>
            <div className="ml-3 flex-1 overflow-hidden">
              <p className="text-sm font-medium text-gray-900 dark:text-white truncate">{user?.name}</p>
              <div className="flex items-center space-x-1.5 mt-0.5">
                <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{user?.role}</p>
                {(user?.authProvider === 'DEMO' || user?.name?.includes('Demo')) && (
                  <span className="text-[9px] bg-amber-500/20 text-amber-700 dark:text-amber-300 font-bold px-1 rounded border border-amber-500/30 uppercase tracking-wide">
                    DEMO
                  </span>
                )}
              </div>
            </div>
          </div>
          <button
            onClick={logout}
            className="flex w-full items-center px-3 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 rounded-md hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-white transition-colors"
          >
            <LogOut className="h-5 w-5 mr-3" />
            Sign Out
          </button>
        </div>
      </aside>
      
      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden">
        {networkError && (
          <div className="bg-red-500 text-white text-center py-2 text-sm font-medium animate-pulse">
            {networkError}
          </div>
        )}
        <header className="h-16 flex items-center justify-between px-8 bg-white/80 dark:bg-gray-900/50 border-b border-gray-200 dark:border-gray-800/50 backdrop-blur-sm transition-colors duration-200">
          <div className="text-sm text-gray-500 dark:text-gray-400 font-medium">
            {new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
          </div>
          
          <div className="flex items-center space-x-4">
            {(user?.authProvider === 'DEMO' || user?.name?.includes('Demo')) && (
              <div className="hidden sm:flex items-center space-x-2 bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/30 px-3 py-1 rounded-full text-xs font-semibold text-amber-800 dark:text-amber-400">
                <span className="h-2 w-2 rounded-full bg-amber-500 animate-pulse"></span>
                <span>DEMO SHOWCASE ENVIRONMENT</span>
              </div>
            )}
            <ThemeToggle />
          </div>
        </header>
        
        <div className="flex-1 overflow-y-auto p-6 md:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
