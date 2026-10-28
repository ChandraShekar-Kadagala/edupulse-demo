import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { Activity, Eye, EyeOff, Sparkles, UserCheck, GraduationCap, Building2 } from 'lucide-react';
import { ThemeToggle } from '../components/ui/ThemeToggle';

interface DemoStudent {
  username: string;
  regNo: string;
  name: string;
  risk: string;
  cgpa?: number;
}

interface DemoStatus {
  demoModeEnabled: boolean;
  demoMarker?: string;
  students?: DemoStudent[];
  defaultStudentUsername?: string;
  defaultStudentRegNo?: string;
  defaultFacultyUsername?: string;
  defaultAdminUsername?: string;
  disclaimer?: string;
}

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [demoStatus, setDemoStatus] = useState<DemoStatus | null>(null);
  
  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    let isMounted = true;
    api.get('/auth/demo-status')
      .then(res => {
        if (isMounted && res.data?.demoModeEnabled) {
          setDemoStatus(res.data);
        }
      })
      .catch(() => {
        // Silently keep demoStatus null if unavailable or endpoint fails
      });
    return () => { isMounted = false; };
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    const trimmedUsername = username.trim();
    if (!trimmedUsername) {
      setError('Registration Number or Username is required.');
      return;
    }

    setLoading(true);
    
    try {
      const response = await api.post('/auth/login', { username: trimmedUsername, password });
      login(response.data);
      navigate('/dashboard');
    } catch (err: any) {
      console.error("Login API failed:", err);
      setError(err.response?.data?.error || err.response?.data?.message || 'Invalid credentials or API unreachable. Ensure the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  const handleFillDemo = (userVal: string) => {
    setError('');
    setUsername(userVal);
    setPassword('DemoEduPulse2026!');
  };

  const demoStudentsList: DemoStudent[] = demoStatus?.students || [
    { username: 'aarav', regNo: 'STU-1001', name: 'Aarav Sharma', risk: 'LOW', cgpa: 9.4 },
    { username: 'bhavna', regNo: 'STU-1002', name: 'Bhavna Patel', risk: 'LOW', cgpa: 7.9 },
    { username: 'chirag', regNo: 'STU-1003', name: 'Chirag Verma', risk: 'MEDIUM', cgpa: 6.2 },
    { username: 'arun', regNo: 'STU-1004', name: 'Arun Kumar', risk: 'HIGH', cgpa: 5.1 },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex flex-col items-center justify-center p-4 sm:px-6 lg:px-8 font-sans transition-colors duration-200 relative">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>

      <div className="w-full max-w-md space-y-6">
        
        {/* Branding Header */}
        <div className="flex flex-col items-center">
          <div className="bg-indigo-600 p-3 rounded-2xl shadow-lg mb-4">
            <Activity className="h-8 w-8 text-white" aria-hidden="true" />
          </div>
          <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white tracking-tight">
            EduPulse
          </h1>
          <p className="mt-2 text-sm text-gray-600 dark:text-gray-400 text-center">
            Early Academic Risk Detection & Intervention Intelligence
          </p>
        </div>
        
        {/* Login Card */}
        <div className="bg-white dark:bg-gray-900 py-8 px-4 shadow-sm sm:rounded-xl sm:px-10 border border-gray-200 dark:border-gray-800 transition-colors duration-200">
          <form className="space-y-6" onSubmit={handleLogin} noValidate>
            
            {/* Error Message */}
            {error && (
              <div 
                className="bg-red-50 dark:bg-red-950/40 border-l-4 border-red-500 p-4 rounded-md"
                role="alert"
                aria-live="assertive"
              >
                <div className="flex">
                  <div className="ml-3">
                    <p className="text-sm text-red-700 dark:text-red-400 font-medium">
                      {error}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Registration Number Field */}
            <div>
              <label htmlFor="registration-number" className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Registration Number (or Faculty ID)
              </label>
              <div className="mt-1 relative">
                <input
                  id="registration-number"
                  name="username"
                  type="text"
                  autoComplete="username"
                  required
                  disabled={loading}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="appearance-none block w-full px-3 py-2.5 border border-gray-300 dark:border-gray-700 rounded-lg shadow-sm placeholder-gray-400 dark:placeholder-gray-500 bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm disabled:opacity-50 transition-colors duration-200"
                  placeholder="e.g. STU-1004 or arun"
                />
              </div>
            </div>

            {/* Password Field */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Password
              </label>
              <div className="mt-1 relative">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  required
                  disabled={loading}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="appearance-none block w-full px-3 py-2.5 border border-gray-300 dark:border-gray-700 rounded-lg shadow-sm placeholder-gray-400 dark:placeholder-gray-500 bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm pr-10 disabled:opacity-50 transition-colors duration-200"
                  placeholder="Enter your password"
                />
                <button
                  type="button"
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 focus:outline-none focus:text-indigo-500 disabled:opacity-50"
                  onClick={() => setShowPassword(!showPassword)}
                  disabled={loading}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? (
                    <EyeOff className="h-5 w-5" aria-hidden="true" />
                  ) : (
                    <Eye className="h-5 w-5" aria-hidden="true" />
                  )}
                </button>
              </div>
            </div>

            {/* Forgot Password Link */}
            <div className="flex items-center justify-end">
              <div className="text-sm">
                <Link
                  to="/forgot-password"
                  className="font-medium text-indigo-600 dark:text-indigo-400 hover:text-indigo-500 focus:outline-none focus:underline transition-colors"
                >
                  Forgot your password?
                </Link>
              </div>
            </div>

            {/* Submit Button */}
            <div>
              <button
                type="submit"
                disabled={loading}
                className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-70 disabled:cursor-not-allowed transition-all duration-200"
              >
                {loading ? (
                  <span className="flex items-center">
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Authenticating...
                  </span>
                ) : (
                  'Sign In'
                )}
              </button>
            </div>
          </form>
        </div>

        {/* Explicit Demo Credentials Panel */}
        {demoStatus?.demoModeEnabled && (
          <div className="bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800/50 rounded-xl p-4 shadow-sm text-amber-900 dark:text-amber-200">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center space-x-1.5">
                <Sparkles className="h-4 w-4 text-amber-600 dark:text-amber-400" />
                <span className="text-xs font-bold uppercase tracking-wider text-amber-800 dark:text-amber-300">
                  Demo Showcase Mode Active
                </span>
              </div>
              <span className="text-[10px] font-semibold bg-amber-200 dark:bg-amber-800/50 text-amber-900 dark:text-amber-200 px-2 py-0.5 rounded-full">
                SANDBOX
              </span>
            </div>

            <p className="text-xs text-amber-800 dark:text-amber-300/90 mb-3 leading-relaxed">
              {demoStatus.disclaimer || 'Illustrative demonstration accounts only. Select any showcase student profile or faculty role below for 1-click login.'}
            </p>

            <div className="space-y-2">
              <div className="text-[11px] font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Showcase Student Cohort:
              </div>
              <div className="grid grid-cols-2 gap-2">
                {demoStudentsList.map((stu) => {
                  const riskColor = stu.risk === 'HIGH'
                    ? 'text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950/40 border-red-200 dark:border-red-800/40'
                    : stu.risk === 'MEDIUM'
                    ? 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 border-amber-200 dark:border-amber-800/40'
                    : 'text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-950/40 border-green-200 dark:border-green-800/40';

                  return (
                    <button
                      key={stu.username}
                      type="button"
                      onClick={() => handleFillDemo(stu.username)}
                      className="flex flex-col p-2 rounded-lg bg-white dark:bg-gray-800/90 border border-gray-200 dark:border-gray-700 hover:border-indigo-500 dark:hover:border-indigo-400 hover:shadow transition-all text-left group"
                    >
                      <div className="flex items-center justify-between w-full">
                        <span className="text-xs font-semibold text-gray-900 dark:text-white truncate group-hover:text-indigo-600 dark:group-hover:text-indigo-400">
                          {stu.name.split(' ')[0]}
                        </span>
                        <span className={`text-[9px] font-bold px-1.5 py-0.2 rounded border ${riskColor}`}>
                          {stu.risk}
                        </span>
                      </div>
                      <span className="text-[10px] text-gray-500 dark:text-gray-400 font-mono mt-0.5">
                        {stu.username} • {stu.regNo}
                      </span>
                    </button>
                  );
                })}
              </div>

              <div className="text-[11px] font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider pt-2">
                Staff & Admin Roles:
              </div>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => handleFillDemo(demoStatus.defaultFacultyUsername || 'faculty')}
                  className="flex items-center p-2 rounded-lg bg-white dark:bg-gray-800/90 border border-gray-200 dark:border-gray-700 hover:border-indigo-500 dark:hover:border-indigo-400 transition-colors text-left"
                >
                  <Building2 className="h-4 w-4 text-emerald-600 dark:text-emerald-400 mr-2 flex-shrink-0" />
                  <div>
                    <span className="text-xs font-semibold text-gray-900 dark:text-white block">Faculty</span>
                    <span className="text-[10px] text-gray-500 dark:text-gray-400 font-mono">faculty</span>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => handleFillDemo(demoStatus.defaultAdminUsername || 'admin')}
                  className="flex items-center p-2 rounded-lg bg-white dark:bg-gray-800/90 border border-gray-200 dark:border-gray-700 hover:border-indigo-500 dark:hover:border-indigo-400 transition-colors text-left"
                >
                  <UserCheck className="h-4 w-4 text-purple-600 dark:text-purple-400 mr-2 flex-shrink-0" />
                  <div>
                    <span className="text-xs font-semibold text-gray-900 dark:text-white block">Admin</span>
                    <span className="text-[10px] text-gray-500 dark:text-gray-400 font-mono">admin</span>
                  </div>
                </button>
              </div>
            </div>

            <div className="mt-3 pt-2 border-t border-amber-200/60 dark:border-amber-800/40 flex items-center justify-between text-[11px] text-amber-800 dark:text-amber-300">
              <span>Demo Password:</span>
              <code className="bg-amber-100 dark:bg-amber-900/60 px-1.5 py-0.5 rounded font-mono text-[10px] select-all font-semibold">
                DemoEduPulse2026!
              </code>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
