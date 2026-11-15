import React from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Activity, LayoutDashboard, Users, AlertTriangle, LogOut, FileText, Menu, X } from 'lucide-react';
import { cn } from '../../utils/utils';
import { ThemeToggle } from '../ui/ThemeToggle';

export default function Layout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [networkError, setNetworkError] = React.useState<string | null>(null);
  const [mobileNavOpen, setMobileNavOpen] = React.useState(false);

  React.useEffect(() => {
    const handleNetworkError = (e: Event) => {
      const customEvent = e as CustomEvent;
      setNetworkError(customEvent.detail);
      setTimeout(() => setNetworkError(null), 5000);
    };
    window.addEventListener('network-error', handleNetworkError);
    return () => window.removeEventListener('network-error', handleNetworkError);
  }, []);

  // Close the mobile drawer whenever the route changes.
  React.useEffect(() => { setMobileNavOpen(false); }, [location.pathname]);

  const navItems = [
    { to: user?.role === 'STUDENT' ? '/students/' + user.id : '/dashboard', icon: LayoutDashboard, label: user?.role === 'STUDENT' ? 'My Profile' : 'Dashboard', roles: ['ADMIN', 'FACULTY', 'STUDENT'] },
    { to: '/students', icon: Users, label: 'Students', roles: ['ADMIN', 'FACULTY'] },
    { to: '/interventions', icon: AlertTriangle, label: 'Interventions', roles: ['ADMIN', 'FACULTY'] },
    { to: '/integration', icon: FileText, label: 'Account Linking', roles: ['STUDENT', 'ADMIN', 'FACULTY'] },
  ];

  const allowedNavItems = navItems.filter(item => user && item.roles.includes(user.role));
  const isDemo = user?.authProvider === 'DEMO' || user?.name?.includes('Demo');

  return (
    <div className="flex h-screen bg-gray-50 dark:bg-gray-950 text-gray-900 dark:text-gray-100 overflow-hidden transition-colors duration-200">
      {/* Mobile overlay */}
      {mobileNavOpen && (
        <div className="fixed inset-0 z-30 bg-black/50 md:hidden" onClick={() => setMobileNavOpen(false)} aria-hidden="true" />
      )}

      {/* Sidebar (static on md+, slide-over drawer on mobile) */}
      <aside
        className={cn(
          'fixed md:static inset-y-0 left-0 z-40 w-64 flex flex-col bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 transition-transform duration-200 md:translate-x-0',
          mobileNavOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="h-16 flex items-center justify-between px-6 border-b border-gray-200 dark:border-gray-800">
          <div className="flex items-center">
            <Activity className="h-6 w-6 text-indigo-600 dark:text-indigo-500 mr-2" />
            <span className="font-bold text-lg tracking-tight text-gray-900 dark:text-white">EDUPULSE</span>
          </div>
          <button className="md:hidden p-1 text-gray-500 hover:text-gray-900 dark:hover:text-white" onClick={() => setMobileNavOpen(false)} aria-label="Close menu">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto py-6 px-3">
          <div className="space-y-1">
            {allowedNavItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    "flex items-center px-3 py-2.5 text-sm font-medium rounded-md transition-colors",
                    isActive
                      ? "bg-indigo-50 dark:bg-indigo-600/10 text-indigo-600 dark:text-indigo-400 font-semibold"
                      : "text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-gray-100"
                  )
                }
              >
                <item.icon className="h-5 w-5 mr-3 shrink-0" />
                {item.label}
              </NavLink>
            ))}
          </div>
        </div>

        <div className="p-4 border-t border-gray-200 dark:border-gray-800">
          <div className="flex items-center mb-4 px-2">
            <div className="h-8 w-8 rounded-full bg-indigo-600 flex items-center justify-center text-white font-bold text-sm shrink-0">
              {user?.name?.charAt(0).toUpperCase()}
            </div>
            <div className="ml-3 flex-1 overflow-hidden">
              <p className="text-sm font-medium text-gray-900 dark:text-white truncate">{user?.name}</p>
              <div className="flex items-center space-x-1.5 mt-0.5">
                <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{user?.role}</p>
                {isDemo && (
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
      <main className="flex-1 flex flex-col overflow-hidden min-w-0">
        {networkError && (
          <div className="bg-red-500 text-white text-center py-2 text-sm font-medium animate-pulse">
            {networkError}
          </div>
        )}
        <header className="h-16 flex items-center justify-between px-4 sm:px-8 bg-white/80 dark:bg-gray-900/50 border-b border-gray-200 dark:border-gray-800/50 backdrop-blur-sm transition-colors duration-200">
          <div className="flex items-center gap-3 min-w-0">
            <button className="md:hidden p-1.5 -ml-1 rounded-md text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800" onClick={() => setMobileNavOpen(true)} aria-label="Open menu">
              <Menu className="h-5 w-5" />
            </button>
            <div className="text-sm text-gray-500 dark:text-gray-400 font-medium truncate">
              <span className="md:hidden font-bold text-gray-900 dark:text-white">EDUPULSE</span>
              <span className="hidden md:inline">{new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</span>
            </div>
          </div>

          <div className="flex items-center space-x-3 sm:space-x-4 shrink-0">
            {isDemo && (
              <div className="hidden sm:flex items-center space-x-2 bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/30 px-3 py-1 rounded-full text-xs font-semibold text-amber-800 dark:text-amber-400">
                <span className="h-2 w-2 rounded-full bg-amber-500 animate-pulse"></span>
                <span>DEMO SHOWCASE</span>
              </div>
            )}
            <ThemeToggle />
          </div>
        </header>

        <div className="flex-1 overflow-y-auto p-4 sm:p-6 md:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
