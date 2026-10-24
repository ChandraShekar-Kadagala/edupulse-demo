import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { Activity, Eye, EyeOff, Sparkles, UserCheck, GraduationCap, Building2 } from 'lucide-react';

interface DemoStatus {
  demoModeEnabled: boolean;
  demoMarker?: string;
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
      setError('Registration Number is required.');
      return;
    }

    setLoading(true);
    
    try {
      // The backend accepts 'username', which represents the Reg No. or Faculty ID.
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

  const handleFillDemo = (role: 'student' | 'faculty' | 'admin') => {
    setError('');
    if (role === 'student') {
      setUsername(demoStatus?.defaultStudentUsername || 'arun');
      setPassword('DemoEduPulse2026!');
    } else if (role === 'faculty') {
      setUsername(demoStatus?.defaultFacultyUsername || 'faculty');
      setPassword('DemoEduPulse2026!');
    } else if (role === 'admin') {
      setUsername(demoStatus?.defaultAdminUsername || 'admin');
      setPassword('DemoEduPulse2026!');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-4 sm:px-6 lg:px-8 font-sans">
      <div className="w-full max-w-md space-y-6">
        
        {/* Branding Header */}
        <div className="flex flex-col items-center">
          <div className="bg-indigo-600 p-3 rounded-2xl shadow-lg mb-4">
            <Activity className="h-8 w-8 text-white" aria-hidden="true" />
          </div>
          <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight">
            EduPulse
          </h1>
          <p className="mt-2 text-sm text-gray-600 text-center">
            Early Academic Risk Detection & Intervention Intelligence
          </p>
        </div>
        
        {/* Login Card */}
        <div className="bg-white py-8 px-4 shadow sm:rounded-xl sm:px-10 border border-gray-200">
          <form className="space-y-6" onSubmit={handleLogin} noValidate>
            
            {/* Error Message */}
            {error && (
              <div 
                className="bg-red-50 border-l-4 border-red-500 p-4 rounded-md"
                role="alert"
                aria-live="assertive"
              >
                <div className="flex">
                  <div className="ml-3">
                    <p className="text-sm text-red-700 font-medium">
                      {error}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Registration Number Field */}
            <div>
              <label htmlFor="registration-number" className="block text-sm font-medium text-gray-700">
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
                  className="appearance-none block w-full px-3 py-2.5 border border-gray-300 rounded-lg shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm disabled:opacity-50 disabled:bg-gray-50 transition-colors duration-200"
                  placeholder="e.g. AP25110070436 or arun"
                />
              </div>
            </div>

            {/* Password Field */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700">
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
                  className="appearance-none block w-full px-3 py-2.5 border border-gray-300 rounded-lg shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm pr-10 disabled:opacity-50 disabled:bg-gray-50 transition-colors duration-200"
                  placeholder="Enter your password"
                />
                <button
                  type="button"
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 focus:outline-none focus:text-indigo-500 disabled:opacity-50"
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
                  className="font-medium text-indigo-600 hover:text-indigo-500 focus:outline-none focus:underline transition-colors"
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

        {/* Explicit Demo Credentials Panel (Rendered only when Demo Mode is explicitly enabled) */}
        {demoStatus?.demoModeEnabled && (
          <div className="bg-amber-50/70 border border-amber-200 rounded-xl p-4 shadow-sm text-amber-900">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center space-x-1.5">
                <Sparkles className="h-4 w-4 text-amber-600" />
                <span className="text-xs font-bold uppercase tracking-wider text-amber-800">
                  Demo Showcase Mode Active
                </span>
              </div>
              <span className="text-[10px] font-semibold bg-amber-200 text-amber-900 px-2 py-0.5 rounded-full">
                SANDBOX
              </span>
            </div>

            <p className="text-xs text-amber-800 mb-3 leading-relaxed">
              {demoStatus.disclaimer || 'Illustrative demonstration accounts only. Synthetic data for evaluation; not affiliated with real SRM student records.'}
            </p>

            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => handleFillDemo('student')}
                className="flex flex-col items-center justify-center p-2 rounded-lg bg-white border border-amber-200 hover:border-amber-400 hover:bg-amber-100/50 transition-colors text-left"
              >
                <GraduationCap className="h-4 w-4 text-indigo-600 mb-1" />
                <span className="text-xs font-semibold text-gray-900">Student</span>
                <span className="text-[10px] text-gray-500 font-mono">arun</span>
              </button>

              <button
                type="button"
                onClick={() => handleFillDemo('faculty')}
                className="flex flex-col items-center justify-center p-2 rounded-lg bg-white border border-amber-200 hover:border-amber-400 hover:bg-amber-100/50 transition-colors text-left"
              >
                <Building2 className="h-4 w-4 text-emerald-600 mb-1" />
                <span className="text-xs font-semibold text-gray-900">Faculty</span>
                <span className="text-[10px] text-gray-500 font-mono">faculty</span>
              </button>

              <button
                type="button"
                onClick={() => handleFillDemo('admin')}
                className="flex flex-col items-center justify-center p-2 rounded-lg bg-white border border-amber-200 hover:border-amber-400 hover:bg-amber-100/50 transition-colors text-left"
              >
                <UserCheck className="h-4 w-4 text-purple-600 mb-1" />
                <span className="text-xs font-semibold text-gray-900">Admin</span>
                <span className="text-[10px] text-gray-500 font-mono">admin</span>
              </button>
            </div>

            <div className="mt-2.5 pt-2 border-t border-amber-200/60 flex items-center justify-between text-[11px] text-amber-800">
              <span>Demo Password:</span>
              <code className="bg-amber-100/80 px-1.5 py-0.5 rounded font-mono text-[10px] select-all">
                DemoEduPulse2026!
              </code>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
