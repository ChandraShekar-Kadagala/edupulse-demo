import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api, { getSafeErrorMessage } from '../services/api';
import { ShieldCheck, AlertTriangle, RefreshCw, FileCode, CheckCircle2, Info, Lock, UploadCloud, BookOpen } from 'lucide-react';

interface IntegrationStatus {
  provider: string;
  mode: string;
  status: string;
  liveAvailable: boolean;
  liveRequirement: string;
  liveBlocker?: string;
  lastSync: string;
  lastSyncStatus?: string;
  lastSyncMessage?: string;
  provenance: string;
  message: string;
  availableDataStreams?: string[];
  supportedModes?: string[];
}

export default function AccountLinking() {
  const { user } = useAuth();
  const [status, setStatus] = useState<IntegrationStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [syncLoading, setSyncLoading] = useState(false);

  // Authentic Report Ingestion state
  const [showReportUpload, setShowReportUpload] = useState(false);
  const [attendanceHtml, setAttendanceHtml] = useState('');
  const [marksHtml, setMarksHtml] = useState('');
  const [ledgerHtml, setLedgerHtml] = useState('');
  const [reportLoading, setReportLoading] = useState(false);

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
      setError(getSafeErrorMessage(err, 'Failed to fetch integration status'));
    } finally {
      setLoading(false);
    }
  };

  const handleSync = async (providerType: 'fixture' | 'demo' | 'official_live') => {
    try {
      setSyncLoading(true);
      setError('');
      setSuccessMessage('');
      const response = await api.post(`/integration/link?provider=${providerType}`);
      setSuccessMessage(response.data.message || 'Synchronization completed successfully.');
      await fetchStatus();
    } catch (err: any) {
      const serverDetails = err.response?.data?.details || err.response?.data?.message;
      setError(serverDetails || getSafeErrorMessage(err, 'Synchronization failed'));
    } finally {
      setSyncLoading(false);
    }
  };

  const handleReportSync = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!attendanceHtml.trim() && !marksHtml.trim() && !ledgerHtml.trim()) {
      setError('Please provide at least one report section (Attendance, Marks, or Exam Ledger HTML).');
      return;
    }

    try {
      setReportLoading(true);
      setError('');
      setSuccessMessage('');
      const response = await api.post('/integration/sync-report', {
        attendanceHtml: attendanceHtml.trim() || undefined,
        marksHtml: marksHtml.trim() || undefined,
        ledgerHtml: ledgerHtml.trim() || undefined,
      });

      setSuccessMessage(response.data.message || 'Authentic report synchronized successfully.');
      setShowReportUpload(false);
      setAttendanceHtml('');
      setMarksHtml('');
      setLedgerHtml('');
      await fetchStatus();
    } catch (err: any) {
      const serverMsg = err.response?.data?.message;
      setError(serverMsg || getSafeErrorMessage(err, 'Report ingestion failed'));
    } finally {
      setReportLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">University Academic Integration</h1>
        <p className="mt-1 text-sm text-gray-600">
          EduPulse integrates with institutional academic data pipelines to empower students with attendance, assessment, and risk analytics.
        </p>
      </div>

      {/* Primary Integration Card */}
      <div className="bg-white shadow rounded-lg overflow-hidden border border-gray-200">
        <div className="p-6">
          <div className="flex items-center justify-between pb-4 border-b border-gray-200">
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-semibold text-gray-900">SRM AP Academic Portal Integration</h2>
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                  Pipeline Active
                </span>
              </div>
              <p className="text-sm text-gray-500 mt-1">Verified HTML DOM parser & risk intelligence pipeline</p>
            </div>
            <div>
              {loading ? (
                <span className="text-sm text-gray-500">Checking...</span>
              ) : status?.provenance === 'INSTITUTIONAL_PORTAL_REPORT' ? (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                  <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                  Authentic Report Ingested
                </span>
              ) : status?.prototypeStatus === 'CONFIGURED' || status?.mode === 'PROTOTYPE_FIXTURE' ? (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-800">
                  <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                  Parser Active (Fixtures & Reports)
                </span>
              ) : (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-800">
                  {status?.status || 'Unknown'}
                </span>
              )}
            </div>
          </div>

          {/* Messages */}
          {error && (
            <div className="mt-4 rounded-md bg-red-50 p-4 border border-red-200">
              <div className="flex">
                <AlertTriangle className="h-5 w-5 text-red-500 mr-2 flex-shrink-0" />
                <p className="text-sm text-red-800 break-words">{error}</p>
              </div>
            </div>
          )}

          {successMessage && (
            <div className="mt-4 rounded-md bg-green-50 p-4 border border-green-200">
              <div className="flex">
                <CheckCircle2 className="h-5 w-5 text-green-500 mr-2 flex-shrink-0" />
                <p className="text-sm text-green-800">{successMessage}</p>
              </div>
            </div>
          )}

          {/* Technical Details */}
          <div className="py-5">
            <dl className="grid grid-cols-1 gap-x-4 gap-y-4 sm:grid-cols-2">
              <div className="sm:col-span-1 bg-gray-50 p-3 rounded">
                <dt className="text-xs font-medium text-gray-500 uppercase">Active Provenance Mode</dt>
                <dd className="mt-1 text-sm font-semibold text-gray-900">
                  {status?.provenance === 'INSTITUTIONAL_PORTAL_REPORT'
                    ? '📄 Authentic Portal Report Ingestion'
                    : status?.provenance === 'INSTITUTIONAL_PORTAL_FIXTURE'
                    ? '🏛️ Institutional Portal Fixture'
                    : status?.provenance || 'N/A'}
                </dd>
              </div>
              <div className="sm:col-span-1 bg-gray-50 p-3 rounded">
                <dt className="text-xs font-medium text-gray-500 uppercase">Last Synchronized</dt>
                <dd className="mt-1 text-sm font-semibold text-gray-900">
                  {status?.lastSync && status.lastSync !== 'N/A' ? new Date(status.lastSync).toLocaleString() : 'Never'}
                </dd>
              </div>
              <div className="sm:col-span-2 bg-gray-50 p-3 rounded">
                <dt className="text-xs font-medium text-gray-500 uppercase">System Status & Transparency</dt>
                <dd className="mt-1 text-xs text-gray-700">
                  {status?.message || 'Academic portal integration is configured via verified HTML DOM parsers.'}
                </dd>
              </div>
            </dl>
          </div>

          {/* Supported Academic Data Streams */}
          <div className="mb-6 p-4 bg-slate-50 rounded-lg border border-slate-200">
            <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <BookOpen className="w-4 h-4 text-indigo-600" />
              Supported SRM AP Academic Data Streams
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-600">
              <div className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
                <span>Course Attendance (Conducted, Attended, Absent, OD/ML, %)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
                <span>Internal Assessments (Tests, Quizzes, Weights)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
                <span>Semester Exam Ledger (Grades, Credits, Pass/Fail)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
                <span>Cumulative Grade Point Average (CGPA)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
                <span>Timetable & Classroom Scheduling</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
                <span>Student Academic Profile & Department</span>
              </div>
            </div>
          </div>

          {/* Live Protection Notice */}
          <div className="rounded-md bg-amber-50 p-4 border border-amber-200 mb-6">
            <div className="flex items-start">
              <Lock className="h-5 w-5 text-amber-600 mr-2 flex-shrink-0 mt-0.5" />
              <div className="text-xs text-amber-800 space-y-1">
                <span className="font-semibold block">Credential Privacy & Institutional Safety Guarantee</span>
                <p>
                  EduPulse does not prompt for or persist university portal passwords. Automated credential scraping and CAPTCHA bypasses are strictly disabled by design. The pipeline accepts authentic student report HTML or verified fixtures, and will activate direct institutional OAuth2/OIDC SSO upon university agreement.
                </p>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-gray-200">
            <div className="flex flex-wrap items-center gap-2">
              <button
                onClick={() => setShowReportUpload(!showReportUpload)}
                className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
              >
                <UploadCloud className="w-4 h-4 mr-2" />
                {showReportUpload ? 'Hide Report Ingestion' : 'Ingest Authentic Portal Report'}
              </button>
              <button
                onClick={() => handleSync('fixture')}
                disabled={loading || syncLoading}
                className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
              >
                <FileCode className="w-4 h-4 mr-2" />
                {syncLoading ? 'Parsing Fixtures...' : 'Sync Portal Fixtures'}
              </button>
              <button
                onClick={() => handleSync('demo')}
                disabled={loading || syncLoading}
                className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
              >
                Sync Sandbox Demo Data
              </button>
            </div>
            <button
              onClick={() => handleSync('official_live')}
              disabled={loading || syncLoading}
              className="inline-flex items-center px-3 py-2 border border-gray-300 text-xs font-medium rounded-md text-gray-500 bg-gray-100 hover:bg-gray-200"
              title="Test Live Official SSO connection status"
            >
              Test Live Connection
            </button>
          </div>

          {/* Authentic Report Ingestion Form */}
          {showReportUpload && (
            <form onSubmit={handleReportSync} className="mt-6 pt-6 border-t border-gray-200 space-y-4">
              <div>
                <h3 className="text-sm font-bold text-gray-900 mb-1">
                  Ingest Authentic SRM AP Portal Report (HTML)
                </h3>
                <p className="text-xs text-gray-500">
                  Save your portal report page from <code className="bg-gray-100 px-1 py-0.5 rounded">student.srmap.edu.in</code> and paste the table HTML below. EduPulse parses the course codes, attendance percentages, and internal marks directly without ever handling your password.
                </p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Attendance Report HTML (Table #tblSubjectWiseAttendance or #attendanceTable)
                </label>
                <textarea
                  rows={3}
                  value={attendanceHtml}
                  onChange={(e) => setAttendanceHtml(e.target.value)}
                  placeholder="Paste attendance HTML or table snippet here..."
                  className="w-full text-xs font-mono p-2 border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Marks Report HTML (Internal Marks Table)
                </label>
                <textarea
                  rows={3}
                  value={marksHtml}
                  onChange={(e) => setMarksHtml(e.target.value)}
                  placeholder="Paste marks HTML or table snippet here (optional)..."
                  className="w-full text-xs font-mono p-2 border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Exam Ledger HTML (Semester Ledger & CGPA)
                </label>
                <textarea
                  rows={3}
                  value={ledgerHtml}
                  onChange={(e) => setLedgerHtml(e.target.value)}
                  placeholder="Paste exam ledger HTML snippet here (optional)..."
                  className="w-full text-xs font-mono p-2 border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>

              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowReportUpload(false)}
                  className="px-3 py-1.5 border border-gray-300 text-xs font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={reportLoading}
                  className="px-4 py-1.5 border border-transparent text-xs font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50"
                >
                  {reportLoading ? 'Processing Report...' : 'Sync Authentic Report'}
                </button>
              </div>
            </form>
          )}

        </div>
      </div>
    </div>
  );
}
