import { useEffect, useRef, useState } from 'react';
import { Download, FileUp, Info, ShieldCheck } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { EmptyState } from '../components/common/StateViews';
import { buildExport, isEmptyExport, type AcademicExport } from '../features/import/parseReport';
import { academicApi, getSafeErrorMessage } from '../services/api';
import type { ImportPreviewResponse, ImportConfirmResult } from '../types';

/**
 * Student-controlled export. The student logs into the SRM portal themselves (their password, their
 * CAPTCHA — EduPulse never sees either), saves their report page, and loads it here. Parsing and the
 * dropping of personal fields happen entirely in the browser; only the clean JSON leaves the machine.
 */
export default function ImportMyRecords() {
  const [fileName, setFileName] = useState<string | null>(null);
  const [reportDate, setReportDate] = useState('');
  const [result, setResult] = useState<AcademicExport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [serverCheck, setServerCheck] = useState<ImportPreviewResponse | null>(null);
  const [checking, setChecking] = useState(false);
  const [consent, setConsent] = useState<boolean | null>(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState<ImportConfirmResult | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  useEffect(() => {
    academicApi.getImportConsent().then((r) => setConsent(r.data.active)).catch(() => setConsent(null));
  }, []);

  const saveToProfile = async () => {
    if (!result) return;
    setSaving(true);
    setError(null);
    try {
      if (!consent) {
        await academicApi.grantImportConsent();
        setConsent(true);
      }
      setSaved((await academicApi.confirmImport(result)).data);
    } catch (e) {
      setError(getSafeErrorMessage(e, 'Could not save your import.'));
    } finally {
      setSaving(false);
    }
  };

  const validateOnServer = async () => {
    if (!result) return;
    setChecking(true);
    setServerCheck(null);
    try {
      setServerCheck((await academicApi.previewImport(result)).data);
    } catch (e) {
      setError(getSafeErrorMessage(e, 'Server validation is unavailable right now.'));
    } finally {
      setChecking(false);
    }
  };

  const handleFile = async (file: File) => {
    setError(null);
    setResult(null);
    if (file.size > 3 * 1024 * 1024) {
      setError('That file is larger than 3 MB. Please select a single saved report page.');
      return;
    }
    try {
      const html = await file.text();
      setFileName(file.name);
      setResult(buildExport(html, { reportDate: reportDate || null }));
    } catch {
      setError('Could not read that file. Save the report page as HTML and try again.');
    }
  };

  const download = () => {
    if (!result) return;
    const blob = new Blob([JSON.stringify(result, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'edupulse-academic-export.json';
    a.click();
    URL.revokeObjectURL(url);
  };

  const empty = result != null && isEmptyExport(result);

  return (
    <div className="max-w-3xl mx-auto space-y-6 pb-12">
      <div>
        <h1 className="text-2xl font-extrabold text-gray-900 dark:text-gray-100">Import my academic records</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400">Bring your own attendance, marks and CGPA into EduPulse — without sharing your portal password.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2"><ShieldCheck className="w-5 h-5 text-emerald-600" aria-hidden="true" /> You stay in control</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-gray-700 dark:text-gray-300 space-y-2">
          <ol className="list-decimal pl-5 space-y-1">
            <li>Log into the SRM AP Student Corner <strong>yourself</strong>, in your own browser, and open your report page.</li>
            <li>Save that page (Ctrl+S → "Webpage, HTML only").</li>
            <li>Load the saved file below. It is read on <strong>your device</strong>; your password and the CAPTCHA never reach EduPulse.</li>
          </ol>
          <p className="flex items-start gap-2 text-xs text-gray-500 dark:text-gray-400">
            <Info className="w-4 h-4 shrink-0 mt-0.5" aria-hidden="true" />
            Your name, date of birth, gender and photo are removed in the browser before anything is exported.
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Load your report</CardTitle>
          <CardDescription>Attendance, internal marks or the exam ledger page.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <label className="block text-sm">
            <span className="text-gray-600 dark:text-gray-300">Report date (optional)</span>
            <input type="date" value={reportDate} onChange={(e) => setReportDate(e.target.value)}
              className="mt-1 block rounded-md border border-gray-300 dark:border-gray-700 bg-transparent px-3 py-1.5 text-sm" />
          </label>
          <input ref={fileInput} type="file" accept=".html,.htm,text/html" className="hidden"
            onChange={(e) => e.target.files?.[0] && handleFile(e.target.files[0])} />
          <Button onClick={() => fileInput.current?.click()}>
            <FileUp className="w-4 h-4 mr-2" aria-hidden="true" /> Choose saved report file
          </Button>
          {fileName && <p className="text-xs text-gray-500">Loaded: {fileName}</p>}
          {error && <p role="alert" className="text-sm text-red-600 dark:text-red-400">{error}</p>}
        </CardContent>
      </Card>

      {result && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              Preview
              <Badge variant="outline">Imported by you — not independently verified</Badge>
            </CardTitle>
            <CardDescription>Nothing is sent anywhere yet. Review, then download the clean file.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {empty ? (
              <EmptyState title="No academic data found on this page">
                This looks like a login or summary page. Open your attendance, marks or exam-ledger report and save that page.
              </EmptyState>
            ) : (
              <>
                <dl className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  {[
                    ['Attendance rows', String(result.attendance.length)],
                    ['Ledger rows', String(result.ledger.length)],
                    ['CGPA', result.cgpa ?? '—'],
                    ['Profile fields', String(Object.keys(result.profile).length)],
                  ].map(([label, value]) => (
                    <div key={label} className="p-3 rounded-lg bg-gray-50 dark:bg-gray-900/60 border border-gray-100 dark:border-gray-800">
                      <dt className="text-xs text-gray-500 dark:text-gray-400">{label}</dt>
                      <dd className="text-lg font-bold text-gray-900 dark:text-gray-100">{value}</dd>
                    </div>
                  ))}
                </dl>
                <div className="flex flex-wrap gap-2">
                  <Button onClick={download}>
                    <Download className="w-4 h-4 mr-2" aria-hidden="true" /> Download clean export (.json)
                  </Button>
                  <Button variant="outline" onClick={validateOnServer} disabled={checking}>
                    {checking ? 'Checking…' : 'Validate with EduPulse'}
                  </Button>
                  <Button onClick={saveToProfile} disabled={saving}>
                    {saving ? 'Saving…' : consent ? 'Save to my profile' : 'Consent & save to my profile'}
                  </Button>
                </div>
                {consent === false && !saved && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    Saving records your consent to import your academic data for risk analytics. You can delete it anytime.
                  </p>
                )}
                {saved && (
                  <div role="status" className="p-3 rounded-lg border border-emerald-200 dark:border-emerald-900/50 bg-emerald-50 dark:bg-emerald-950/30 text-sm">
                    <p className="font-semibold text-emerald-800 dark:text-emerald-300">
                      {saved.duplicate ? 'Already imported — your profile is up to date.' : 'Saved to your profile.'}
                    </p>
                    <p className="text-gray-700 dark:text-gray-300">
                      {saved.acceptedAttendance} attendance + {saved.acceptedLedger} result rows
                      {saved.riskScore != null && <> · your risk is now <strong>{saved.riskScore}/100 ({saved.riskLevel})</strong></>}
                    </p>
                    <a href="/dashboard" className="text-indigo-600 dark:text-indigo-400 hover:underline">View my profile →</a>
                  </div>
                )}
                {serverCheck && (
                  <div className="p-3 rounded-lg border border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-gray-900/60 text-sm space-y-2">
                    <p className="font-semibold text-gray-900 dark:text-gray-100">
                      Server check — {serverCheck.sourceLabel}
                    </p>
                    <p className="text-gray-700 dark:text-gray-300">
                      {serverCheck.acceptedAttendance} attendance + {serverCheck.acceptedLedger} result rows accepted
                      {serverCheck.rejected > 0 && <span className="text-red-600 dark:text-red-400"> · {serverCheck.rejected} rejected</span>}
                      {serverCheck.warnings > 0 && <span className="text-amber-600 dark:text-amber-400"> · {serverCheck.warnings} warnings</span>}
                    </p>
                    {serverCheck.issues.length > 0 && (
                      <ul className="list-disc pl-5 text-xs text-gray-600 dark:text-gray-400 space-y-0.5">
                        {serverCheck.issues.slice(0, 20).map((i, idx) => (
                          <li key={idx}><span className="uppercase text-[10px] font-semibold">{i.severity}</span> {i.message}</li>
                        ))}
                      </ul>
                    )}
                  </div>
                )}
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Nothing is stored yet — the server only checks your data. Saving it into your profile is the next step, once the import tables are approved.
                </p>
              </>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
