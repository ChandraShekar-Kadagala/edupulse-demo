import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import { Activity, ArrowLeft, CheckCircle2 } from 'lucide-react';

export default function ForgotPassword() {
  const [username, setUsername] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    const trimmedUsername = username.trim();
    if (!trimmedUsername) {
      setError('Registration Number is required.');
      return;
    }

    setLoading(true);
    
    try {
      // Send reset request to backend
      await api.post('/auth/forgot-password', { username: trimmedUsername });
      
      // We always show success even if the account doesn't exist to prevent enumeration attacks.
      setSubmitted(true);
    } catch (err: any) {
      console.error("Forgot password API failed:", err);
      // Still show success to prevent enumeration, unless it's a 429 Rate Limit or 500
      if (err.response?.status === 429) {
        setError('Too many requests. Please try again later.');
      } else if (err.response?.status >= 500) {
        setError('Server error occurred. Please try again later.');
      } else {
        setSubmitted(true);
      }
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-4 sm:px-6 lg:px-8 font-sans">
        <div className="w-full max-w-md space-y-8 bg-white py-10 px-6 shadow sm:rounded-xl border border-gray-200 text-center">
          <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100 mb-4">
            <CheckCircle2 className="h-6 w-6 text-green-600" aria-hidden="true" />
          </div>
          <h2 className="text-2xl font-bold text-gray-900">Request Received</h2>
          <div className="mt-4 text-sm text-gray-600 space-y-4 text-left bg-gray-50 p-4 rounded-lg border border-gray-100">
            <p>
              If an account exists for <strong>{username}</strong>, a password reset link has been sent to the registered email address or phone number.
            </p>
            <p className="text-xs text-gray-500 italic border-t border-gray-200 pt-2 mt-2">
              Note: This system currently does not have an active SMTP email or SMS provider configured. 
              In a production environment, you would receive a link containing a secure, short-lived reset token.
            </p>
          </div>
          <div className="mt-6">
            <Link
              to="/login"
              className="text-indigo-600 hover:text-indigo-500 font-medium flex items-center justify-center gap-2"
            >
              <ArrowLeft className="h-4 w-4" /> Return to Login
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-4 sm:px-6 lg:px-8 font-sans">
      <div className="w-full max-w-md space-y-8">
        
        {/* Branding Header */}
        <div className="flex flex-col items-center">
          <div className="bg-indigo-600 p-3 rounded-2xl shadow-lg mb-4">
            <Activity className="h-8 w-8 text-white" aria-hidden="true" />
          </div>
          <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight">
            Reset Password
          </h1>
          <p className="mt-2 text-sm text-gray-600 text-center px-4">
            Enter your Registration Number or Faculty ID to receive a password reset link.
          </p>
        </div>
        
        {/* Form Card */}
        <div className="bg-white py-8 px-4 shadow sm:rounded-xl sm:px-10 border border-gray-200">
          <form className="space-y-6" onSubmit={handleSubmit} noValidate>
            
            {/* Error Message */}
            {error && (
              <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-md" role="alert">
                <p className="text-sm text-red-700 font-medium">{error}</p>
              </div>
            )}

            {/* Registration Number Field */}
            <div>
              <label htmlFor="registration-number" className="block text-sm font-medium text-gray-700">
                Registration Number
              </label>
              <div className="mt-1">
                <input
                  id="registration-number"
                  name="username"
                  type="text"
                  required
                  disabled={loading}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="appearance-none block w-full px-3 py-2.5 border border-gray-300 rounded-lg shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm disabled:opacity-50 transition-colors"
                  placeholder="e.g. AP25110070436"
                />
              </div>
            </div>

            {/* Submit Button */}
            <div>
              <button
                type="submit"
                disabled={loading}
                className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-70 disabled:cursor-not-allowed transition-all"
              >
                {loading ? 'Sending Request...' : 'Send Reset Link'}
              </button>
            </div>
            
            <div className="text-center mt-4">
              <Link
                to="/login"
                className="text-sm font-medium text-indigo-600 hover:text-indigo-500 flex items-center justify-center gap-1"
              >
                <ArrowLeft className="h-4 w-4" /> Back to Login
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
