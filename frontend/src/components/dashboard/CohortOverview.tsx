import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { academicApi, getSafeErrorMessage } from '../../services/api';
import type { DashboardSummary } from '../../types';
import { EmptyState, ErrorState, LoadingState, fmt } from '../common/StateViews';
import { InterventionEffectivenessCard } from './InterventionEffectivenessCard';

/** Administrator view: cohort-wide counts from /dashboard/summary (no invented defaults). */
export function CohortOverview() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setSummary((await academicApi.getDashboardSummary()).data);
    } catch (e) {
      setError(getSafeErrorMessage(e, 'Unable to load the cohort summary.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  if (loading) return <LoadingState title="Loading cohort overview" />;
  if (error || !summary) return <ErrorState title="Cohort overview unavailable" message={error ?? 'No data returned.'} onRetry={load} />;

  const tiles: [string, string][] = [
    ['Students', String(summary.totalStudents)],
    ['High risk', String(summary.highRisk)],
    ['Medium risk', String(summary.mediumRisk)],
    ['Low risk', String(summary.lowRisk)],
    ['Insufficient data', String(summary.insufficientData)],
    ['Average attendance', fmt(summary.cohortAverageAttendance, '%')],
    ['Average marks', fmt(summary.cohortAverageMarks, '%')],
    ['Open interventions', String(summary.activeInterventions)],
  ];

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      <div>
        <h1 className="text-2xl font-extrabold text-gray-900 dark:text-gray-100">Cohort overview</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400">All students, from the latest official risk results.</p>
      </div>
      <dl className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {tiles.map(([label, value]) => (
          <div key={label} className="p-4 rounded-xl bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800">
            <dt className="text-xs text-gray-500 dark:text-gray-400">{label}</dt>
            <dd className="text-2xl font-bold text-gray-900 dark:text-gray-100">{value}</dd>
          </div>
        ))}
      </dl>

      <div className="grid lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>High-risk students</CardTitle>
            <CardDescription>Most recent HIGH results</CardDescription>
          </CardHeader>
          <CardContent>
            {!summary.recentHighRisk?.length ? <EmptyState title="No students currently at HIGH risk" /> : (
              <ul className="divide-y divide-gray-100 dark:divide-gray-800 text-sm">
                {summary.recentHighRisk.map(s => (
                  <li key={s.id} className="py-2 flex justify-between gap-3">
                    <span>
                      <Link to={`/students/${s.id}`} className="font-semibold text-indigo-700 dark:text-indigo-300 hover:underline">{s.name}</Link>
                      <span className="block text-[11px] text-gray-500">{s.factor}</span>
                    </span>
                    <span className="text-right">
                      <Badge variant="destructive">{fmt(s.riskScore)}</Badge>
                      <span className="block text-[11px] text-gray-500 mt-0.5">Intervention: {s.intervention}</span>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Cohort signals</CardTitle></CardHeader>
          <CardContent className="text-sm space-y-3">
            <div>
              <p className="text-xs font-semibold text-green-700 dark:text-green-400">Strengths</p>
              <ul className="list-disc pl-5">{(summary.cohortStrengths ?? []).map(s => <li key={s}>{s}</li>)}</ul>
            </div>
            <div>
              <p className="text-xs font-semibold text-amber-700 dark:text-amber-400">Areas to improve</p>
              <ul className="list-disc pl-5">{(summary.cohortImprovementAreas ?? []).map(s => <li key={s}>{s}</li>)}</ul>
            </div>
          </CardContent>
        </Card>
      </div>

      <InterventionEffectivenessCard />
    </div>
  );
}
