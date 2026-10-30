import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { Activity, Eye, EyeOff, Sparkles, UserCheck, GraduationCap, Building2, Copy, Check, ShieldCheck, ArrowRight, Info } from 'lucide-react';
import { ThemeToggle } from '../components/ui/ThemeToggle';

interface ShowcaseStudent {
  username: string;
  regNo: string;
  name: string;
  role: 'STUDENT';
  risk: 'LOW' | 'MEDIUM' | 'HIGH';
  riskScore: number;
  cgpa: number;
  attendance: string;
  marks: string;
  profile: string;
  provenance: string;
}

interface ShowcaseFaculty {
  username: string;
  name: string;
  role: 'FACULTY' | 'ADMIN';
  title: string;
  course?: string;
  courseName?: string;
  profile: string;
}

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'students' | 'faculty'>('students');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const { login } = useAuth();
  const navigate = useNavigate();

  const publicDemoPassword = 'DemoEduPulse2026!';

  const studentAccounts: ShowcaseStudent[] = [
    {
      username: 'demo.student.a',
      regNo: 'STU-1001',
      name: 'Student A — Reference',
      role: 'STUDENT',
      risk: 'LOW',
      riskScore: 0.0,
      cgpa: 9.1,
      attendance: '99.6% (275/276 classes)',
      marks: '44.2% (150.44/340 marks)',
      profile: 'Anonymized SRM AP academic reference data (High attendance, clean on-time coursework)',
      provenance: 'STUDENT-SUPPLIED REFERENCE DATA'
    },
    {
      username: 'demo.student.b',
      regNo: 'STU-1002',
      name: 'Student B — Top Performer',
      role: 'STUDENT',
      risk: 'LOW',
      riskScore: 0.0,
      cgpa: 9.4,
      attendance: '97.1% (268/276 classes)',
      marks: '92.4% (314/340 marks)',
      profile: 'High academic performance across all assessments, peer mentor track',
      provenance: 'SYNTHETIC DEMO DATA'
    },
    {
      username: 'demo.student.c',
      regNo: 'STU-1003',
      name: 'Student C — Average Performer',
      role: 'STUDENT',
      risk: 'MEDIUM',
      riskScore: 37.0,
      cgpa: 7.2,
      attendance: '71.0% (196/276 classes)',
      marks: '38.8% (132/340 marks)',
      profile: 'Average performance with 4 overdue deliverables (Medium risk advisory)',
      provenance: 'SYNTHETIC DEMO DATA'
    },
    {
      username: 'demo.student.d',
      regNo: 'STU-1004',
      name: 'Student D — High Risk',
      role: 'STUDENT',
      risk: 'HIGH',
      riskScore: 74.8,
      cgpa: 5.1,
      attendance: '59.8% (165/276 classes)',
      marks: '27.6% (94/340 marks)',
      profile: 'Critical attendance shortfall and 6 overdue deliverables (Urgent intervention)',
      provenance: 'SYNTHETIC DEMO DATA'
    }
  ];

  const facultyAccounts: ShowcaseFaculty[] = [
    {
      username: 'demo.faculty.1',
      name: 'Faculty A',
      role: 'FACULTY',
      title: 'Associate Professor',
      course: 'CSE 204',
      courseName: 'Design and Analysis of Algorithms',
      profile: 'Faculty Instructor & Course Evaluator for CSE 204'
    },
    {
      username: 'demo.faculty.2',
      name: 'Faculty B',
      role: 'FACULTY',
      title: 'Assistant Professor',
      course: 'CSE 213',
      courseName: 'Database Systems',
      profile: 'Faculty Instructor & Course Evaluator for CSE 213'
    },
    {
      username: 'admin',
      name: 'Administrator',
      role: 'ADMIN',
      title: 'System Administrator',
      course: 'Department Admin',
      courseName: 'CSE Department Administration',
      profile: 'Full governance, audit logs, and cohort administration access'
    }
  ];

  useEffect(() => {
    // Ping demo-status to prime the backend or verify connection
    api.get('/auth/demo-status').catch(() => {
      // Backend waking up or silent pass
    });
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

  const handleUseAccount = (userVal: string) => {
    setError('');
    setUsername(userVal);
    setPassword(publicDemoPassword);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const copyToClipboard = async (text: string, key: string) => {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
      }
      setCopiedKey(key);
      setTimeout(() => setCopiedKey(null), 2000);
    } catch (err) {
      console.error("Failed to copy:", err);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex flex-col items-center justify-start py-8 px-4 sm:px-6 lg:px-8 font-sans transition-colors duration-200 relative">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>

      <div className="w-full max-w-2xl space-y-6">

        {/* Branding Header */}
        <div className="flex flex-col items-center text-center">
          <div className="bg-indigo-600 p-3 rounded-2xl shadow-lg mb-3">
            <Activity className="h-8 w-8 text-white" aria-hidden="true" />
          </div>
          <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white tracking-tight">
            EduPulse
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400 max-w-md">
            Early Academic Risk Detection & Intervention Intelligence
          </p>
        </div>

        {/* Login Form Card */}
        <div className="bg-white dark:bg-gray-900 py-6 px-6 sm:px-8 shadow-sm rounded-xl border border-gray-200 dark:border-gray-800 transition-colors duration-200">
          <form className="space-y-4" onSubmit={handleLogin} noValidate>

            {/* Error Message */}
            {error && (
              <div
                className="bg-red-50 dark:bg-red-950/40 border-l-4 border-red-500 p-3 rounded-md"
                role="alert"
                aria-live="assertive"
              >
                <p className="text-sm text-red-700 dark:text-red-400 font-medium">
                  {error}
                </p>
              </div>
            )}

            {/* Username / Reg Number Field */}
            <div>
              <label htmlFor="login-username" className="block text-xs font-semibold uppercase tracking-wider text-gray-700 dark:text-gray-300">
                Registration Number or Username
              </label>
              <div className="mt-1">
                <input
                  id="login-username"
                  name="username"
                  type="text"
                  autoComplete="username"
                  required
                  disabled={loading}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="appearance-none block w-full px-3 py-2 border border-gray-300 dark:border-gray-700 rounded-lg shadow-sm placeholder-gray-400 dark:placeholder-gray-500 bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-sm disabled:opacity-50 transition-colors"
                  placeholder="e.g. demo.student.a or demo.faculty.1"
                />
              </div>
            </div>

            {/* Password Field */}
            <div>
              <label htmlFor="login-password" className="block text-xs font-semibold uppercase tracking-wider text-gray-700 dark:text-gray-300">
                Password
              </label>
              <div className="mt-1 relative">
                <input
                  id="login-password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  required
                  disabled={loading}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="appearance-none block w-full px-3 py-2 border border-gray-300 dark:border-gray-700 rounded-lg shadow-sm placeholder-gray-400 dark:placeholder-gray-500 bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-sm pr-10 disabled:opacity-50 transition-colors"
                  placeholder="Enter your account password"
                />
                <button
                  type="button"
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 focus:outline-none focus:text-indigo-500"
                  onClick={() => setShowPassword(!showPassword)}
                  disabled={loading}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? (
                    <EyeOff className="h-4 w-4" aria-hidden="true" />
                  ) : (
                    <Eye className="h-4 w-4" aria-hidden="true" />
                  )}
                </button>
              </div>
            </div>

            {/* Forgot Password Link */}
            <div className="flex items-center justify-end">
              <Link
                to="/forgot-password"
                className="text-xs font-medium text-indigo-600 dark:text-indigo-400 hover:text-indigo-500 transition-colors"
              >
                Forgot your password?
              </Link>
            </div>

            {/* Submit Button */}
            <div>
              <button
                type="submit"
                disabled={loading}
                className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-70 disabled:cursor-not-allowed transition-all"
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

        {/* =========================================================================
            DEMO ACCESS ACCOUNTS PANEL (Always Visible Directly on /login)
            ========================================================================= */}
        <div className="bg-white dark:bg-gray-900 border border-indigo-100 dark:border-indigo-950/60 rounded-xl p-5 shadow-sm space-y-4">

          {/* Panel Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-gray-100 dark:border-gray-800 pb-3">
            <div className="flex items-center space-x-2">
              <Sparkles className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
              <div>
                <h2 className="text-sm font-bold text-gray-900 dark:text-white uppercase tracking-wider">
                  DEMO ACCESS ACCOUNTS
                </h2>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Select, copy, or auto-fill dedicated showcase credentials
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-300/50">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5 animate-pulse"></span>
                DEMO SHOWCASE ACTIVE
              </span>
            </div>
          </div>

          {/* Public Demo Password Banner */}
          <div className="bg-indigo-50/80 dark:bg-indigo-950/30 border border-indigo-200 dark:border-indigo-900/40 rounded-lg p-3 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
            <div className="flex items-center space-x-2">
              <ShieldCheck className="h-4 w-4 text-indigo-600 dark:text-indigo-400 flex-shrink-0" />
              <div className="text-gray-700 dark:text-gray-300">
                <span className="font-semibold text-gray-900 dark:text-white">Configured Public Demo Password:</span>{' '}
                <code className="font-mono font-bold bg-white dark:bg-gray-800 text-indigo-700 dark:text-indigo-300 px-1.5 py-0.5 rounded border border-indigo-200 dark:border-indigo-800 ml-1">
                  {publicDemoPassword}
                </code>
              </div>
            </div>
            <button
              type="button"
              onClick={() => copyToClipboard(publicDemoPassword, 'master-password')}
              className="inline-flex items-center justify-center px-2.5 py-1 rounded bg-white dark:bg-gray-800 border border-indigo-200 dark:border-indigo-800 hover:bg-indigo-100 dark:hover:bg-indigo-900 text-indigo-700 dark:text-indigo-300 font-medium transition-colors"
            >
              {copiedKey === 'master-password' ? (
                <>
                  <Check className="h-3.5 w-3.5 mr-1 text-emerald-600" /> Copied!
                </>
              ) : (
                <>
                  <Copy className="h-3.5 w-3.5 mr-1" /> Copy Password
                </>
              )}
            </button>
          </div>

          {/* Illustrative Notice Disclaimer */}
          <div className="flex items-start space-x-2 text-[11px] text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-800/40 p-2.5 rounded-lg border border-gray-200 dark:border-gray-800">
            <Info className="h-4 w-4 text-gray-400 flex-shrink-0 mt-0.5" />
            <p className="leading-relaxed">
              These are public demonstration accounts with pre-loaded SRM AP curriculum scenarios. Student A reflects verified reference records; Students B, C, D and Faculty accounts provide synthetic comparative benchmarks.
            </p>
          </div>

          {/* Role Navigation Tabs */}
          <div className="flex border-b border-gray-200 dark:border-gray-800">
            <button
              type="button"
              onClick={() => setActiveTab('students')}
              className={`flex-1 py-2 text-xs font-bold uppercase tracking-wider text-center border-b-2 transition-colors ${
                activeTab === 'students'
                  ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              }`}
            >
              <GraduationCap className="h-4 w-4 inline mr-1.5" />
              Student Accounts (4 Profiles)
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('faculty')}
              className={`flex-1 py-2 text-xs font-bold uppercase tracking-wider text-center border-b-2 transition-colors ${
                activeTab === 'faculty'
                  ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              }`}
            >
              <Building2 className="h-4 w-4 inline mr-1.5" />
              Faculty & Admin (3 Profiles)
            </button>
          </div>

          {/* Tab 1: Student Accounts */}
          {activeTab === 'students' && (
            <div className="space-y-3 pt-1">
              <div className="text-[11px] font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                STUDENT ACCOUNTS:
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {studentAccounts.map((stu) => {
                  const isHigh = stu.risk === 'HIGH';
                  const isMed = stu.risk === 'MEDIUM';
                  const riskBadgeClass = isHigh
                    ? 'bg-red-100 text-red-800 dark:bg-red-950/60 dark:text-red-300 border-red-200 dark:border-red-800'
                    : isMed
                    ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border-amber-200 dark:border-amber-800'
                    : 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800';

                  const userCopyKey = 'stu-user-' + stu.username;
                  const passCopyKey = 'stu-pass-' + stu.username;

                  return (
                    <div
                      key={stu.username}
                      className="p-3.5 rounded-lg border border-gray-200 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/50 hover:border-indigo-300 dark:hover:border-indigo-700 transition-all flex flex-col justify-between"
                    >
                      <div>
                        {/* Title and Badges */}
                        <div className="flex items-start justify-between gap-1 mb-1.5">
                          <span className="text-xs font-bold text-gray-900 dark:text-white">
                            {stu.name}
                          </span>
                          <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded border ${riskBadgeClass}`}>
                            {stu.risk} ({stu.riskScore})
                          </span>
                        </div>

                        {/* Provenance Badge */}
                        <div className="mb-2">
                          {stu.provenance === 'STUDENT-SUPPLIED REFERENCE DATA' ? (
                            <span className="text-[9px] font-semibold bg-emerald-600 text-white px-1.5 py-0.5 rounded">
                              ⭐ REFERENCE DATA
                            </span>
                          ) : (
                            <span className="text-[9px] font-semibold border border-amber-300 text-amber-700 dark:text-amber-300 px-1.5 py-0.5 rounded">
                              SYNTHETIC COHORT
                            </span>
                          )}
                        </div>

                        {/* Profile Description */}
                        <p className="text-[11px] text-gray-600 dark:text-gray-400 leading-snug mb-2">
                          {stu.profile}
                        </p>

                        {/* Credentials Details */}
                        <div className="bg-white dark:bg-gray-900 p-2 rounded border border-gray-200 dark:border-gray-700/80 space-y-1 text-[11px] font-mono mb-3">
                          <div className="flex items-center justify-between text-gray-700 dark:text-gray-300">
                            <span className="text-gray-500 font-sans text-[10px]">Username:</span>
                            <span className="font-semibold text-indigo-600 dark:text-indigo-400">{stu.username}</span>
                          </div>
                          <div className="flex items-center justify-between text-gray-700 dark:text-gray-300">
                            <span className="text-gray-500 font-sans text-[10px]">ID / Reg:</span>
                            <span>{stu.regNo}</span>
                          </div>
                          <div className="flex items-center justify-between text-gray-700 dark:text-gray-300">
                            <span className="text-gray-500 font-sans text-[10px]">Password:</span>
                            <span className="text-gray-500">{publicDemoPassword}</span>
                          </div>
                        </div>
                      </div>

                      {/* Action Buttons */}
                      <div className="grid grid-cols-3 gap-1.5 pt-2 border-t border-gray-200/60 dark:border-gray-700/50">
                        <button
                          type="button"
                          onClick={() => handleUseAccount(stu.username)}
                          className="px-2 py-1 bg-indigo-600 hover:bg-indigo-700 text-white rounded text-[11px] font-semibold flex items-center justify-center transition-colors"
                          title="Fills username and password into the login form"
                        >
                          Use Account
                        </button>
                        <button
                          type="button"
                          onClick={() => copyToClipboard(stu.username, userCopyKey)}
                          className="px-2 py-1 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 rounded text-[11px] font-medium flex items-center justify-center transition-colors"
                          title="Copy username to clipboard"
                        >
                          {copiedKey === userCopyKey ? (
                            <>
                              <Check className="h-3 w-3 mr-1 text-emerald-600" /> Copied
                            </>
                          ) : (
                            <>
                              <Copy className="h-3 w-3 mr-1 text-gray-400" /> Username
                            </>
                          )}
                        </button>
                        <button
                          type="button"
                          onClick={() => copyToClipboard(publicDemoPassword, passCopyKey)}
                          className="px-2 py-1 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 rounded text-[11px] font-medium flex items-center justify-center transition-colors"
                          title="Copy password to clipboard"
                        >
                          {copiedKey === passCopyKey ? (
                            <>
                              <Check className="h-3 w-3 mr-1 text-emerald-600" /> Copied
                            </>
                          ) : (
                            <>
                              <Copy className="h-3 w-3 mr-1 text-gray-400" /> Password
                            </>
                          )}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Tab 2: Faculty & Admin Accounts */}
          {activeTab === 'faculty' && (
            <div className="space-y-3 pt-1">
              <div className="text-[11px] font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                FACULTY ACCOUNTS:
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {facultyAccounts.map((fac) => {
                  const userCopyKey = 'fac-user-' + fac.username;
                  const passCopyKey = 'fac-pass-' + fac.username;

                  return (
                    <div
                      key={fac.username}
                      className="p-3.5 rounded-lg border border-gray-200 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/50 hover:border-indigo-300 dark:hover:border-indigo-700 transition-all flex flex-col justify-between"
                    >
                      <div>
                        {/* Title and Badges */}
                        <div className="flex items-start justify-between gap-1 mb-1">
                          <span className="text-xs font-bold text-gray-900 dark:text-white">
                            {fac.name}
                          </span>
                          <span className="text-[10px] font-bold px-1.5 py-0.5 rounded border border-indigo-300 dark:border-indigo-800 bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300">
                            {fac.role}
                          </span>
                        </div>

                        {/* Title and Subject */}
                        <div className="text-[11px] font-semibold text-indigo-600 dark:text-indigo-400 mb-1">
                          {fac.course ? `${fac.course}: ${fac.courseName}` : fac.title}
                        </div>

                        {/* Profile Description */}
                        <p className="text-[11px] text-gray-600 dark:text-gray-400 leading-snug mb-2">
                          {fac.profile}
                        </p>

                        {/* Credentials Details */}
                        <div className="bg-white dark:bg-gray-900 p-2 rounded border border-gray-200 dark:border-gray-700/80 space-y-1 text-[11px] font-mono mb-3">
                          <div className="flex items-center justify-between text-gray-700 dark:text-gray-300">
                            <span className="text-gray-500 font-sans text-[10px]">Username:</span>
                            <span className="font-semibold text-indigo-600 dark:text-indigo-400">{fac.username}</span>
                          </div>
                          <div className="flex items-center justify-between text-gray-700 dark:text-gray-300">
                            <span className="text-gray-500 font-sans text-[10px]">Role / Title:</span>
                            <span className="truncate max-w-[140px]">{fac.title}</span>
                          </div>
                          <div className="flex items-center justify-between text-gray-700 dark:text-gray-300">
                            <span className="text-gray-500 font-sans text-[10px]">Password:</span>
                            <span className="text-gray-500">{publicDemoPassword}</span>
                          </div>
                        </div>
                      </div>

                      {/* Action Buttons */}
                      <div className="grid grid-cols-3 gap-1.5 pt-2 border-t border-gray-200/60 dark:border-gray-700/50">
                        <button
                          type="button"
                          onClick={() => handleUseAccount(fac.username)}
                          className="px-2 py-1 bg-indigo-600 hover:bg-indigo-700 text-white rounded text-[11px] font-semibold flex items-center justify-center transition-colors"
                          title="Fills username and password into the login form"
                        >
                          Use Account
                        </button>
                        <button
                          type="button"
                          onClick={() => copyToClipboard(fac.username, userCopyKey)}
                          className="px-2 py-1 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 rounded text-[11px] font-medium flex items-center justify-center transition-colors"
                          title="Copy username to clipboard"
                        >
                          {copiedKey === userCopyKey ? (
                            <>
                              <Check className="h-3 w-3 mr-1 text-emerald-600" /> Copied
                            </>
                          ) : (
                            <>
                              <Copy className="h-3 w-3 mr-1 text-gray-400" /> Username
                            </>
                          )}
                        </button>
                        <button
                          type="button"
                          onClick={() => copyToClipboard(publicDemoPassword, passCopyKey)}
                          className="px-2 py-1 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 rounded text-[11px] font-medium flex items-center justify-center transition-colors"
                          title="Copy password to clipboard"
                        >
                          {copiedKey === passCopyKey ? (
                            <>
                              <Check className="h-3 w-3 mr-1 text-emerald-600" /> Copied
                            </>
                          ) : (
                            <>
                              <Copy className="h-3 w-3 mr-1 text-gray-400" /> Password
                            </>
                          )}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

        </div>

      </div>
    </div>
  );
}
