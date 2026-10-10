import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

interface IntegrationStatus {
  provider: string;
  status: string;
  message: string;
  lastSync: string;
}

export default function AccountLinking() {
  const { user } = useAuth();
  const [status, setStatus] = useState<IntegrationStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [linkLoading, setLinkLoading] = useState(false);

  useEffect(() => {
    fetchStatus();
  }, []);

  const fetchStatus = async () => {
    try {
      setLoading(true);
      setError('');
      const response = await api.get('/integration/status');
      setStatus(response.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch integration status');
    } finally {
      setLoading(false);
    }
  };

  const handleLinkAccount = async () => {
    try {
      setLinkLoading(true);
      setError('');
      await api.post('/integration/link');
      await fetchStatus();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to link account');
    } finally {
      setLinkLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">University Account Integration</h1>
        <p className="mt-2 text-sm text-gray-600">
          Connect your official university account to automatically synchronize your academic data, attendance, and grades.
        </p>
      </div>

      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-medium text-gray-900">SRM AP Official Portal</h2>
              <p className="text-sm text-gray-500">Authorized data provider</p>
            </div>
            {loading ? (
              <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800">
                Loading...
              </span>
            ) : status?.status === 'CONNECTED' ? (
              <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-800">
                Connected
              </span>
            ) : status?.status === 'NOT_CONFIGURED' ? (
              <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-yellow-100 text-yellow-800">
                Not Configured
              </span>
            ) : (
              <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800">
                {status?.status || 'Unknown'}
              </span>
            )}
          </div>

          {error && (
            <div className="mb-6 rounded-md bg-red-50 p-4">
              <div className="flex">
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-red-800">{error}</h3>
                </div>
              </div>
            </div>
          )}

          <div className="border-t border-gray-200 py-5">
            <dl className="grid grid-cols-1 gap-x-4 gap-y-6 sm:grid-cols-2">
              <div className="sm:col-span-1">
                <dt className="text-sm font-medium text-gray-500">Data Access Scope</dt>
                <dd className="mt-1 text-sm text-gray-900">
                  <ul className="list-disc pl-5 space-y-1">
                    <li>Semester grades and CGPA</li>
                    <li>Course attendance records</li>
                    <li>Assignment submissions</li>
                  </ul>
                </dd>
              </div>
              <div className="sm:col-span-1">
                <dt className="text-sm font-medium text-gray-500">Last Synchronization</dt>
                <dd className="mt-1 text-sm text-gray-900">{status?.lastSync || 'Never'}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-sm font-medium text-gray-500">Integration Status</dt>
                <dd className="mt-1 text-sm text-gray-900">
                  {status?.message || 'Checking availability...'}
                </dd>
              </div>
            </dl>
          </div>

          <div className="mt-6 flex items-center justify-end border-t border-gray-200 pt-5">
            {user?.role === 'STUDENT' && (
              <button
                onClick={handleLinkAccount}
                disabled={loading || linkLoading || status?.status === 'CONNECTED'}
                className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
              >
                {linkLoading ? 'Linking...' : 'Link Account'}
              </button>
            )}
            {user?.role !== 'STUDENT' && (
              <p className="text-sm text-gray-500">Only student accounts can link to the portal.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
