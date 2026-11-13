import { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, FlaskConical } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { academicApi, getSafeErrorMessage } from '../../services/api';
import type { EffectivenessGroup, EffectivenessReport } from '../../types';
import { EmptyState, ErrorState, LoadingState, fmt } from '../common/StateViews';

function GroupRow({ g }: { g: EffectivenessGroup }) {
  return (
    <tr className="border-t border-gray-100 dark:border-gray-800">
      <td className="py-2 pr-3 font-medium text-gray-900 dark:text-gray-100">{g.group}</td>
      <td className="py-2 px-3 text-right">{g.interventions}</td>
      <td className="py-2 px-3 text-right">{g.evaluable}</td>
      <td className="py-2 px-3 text-right font-mono">{fmt(g.meanRiskChange, ' pts')}</td>
      <td className="py-2 px-3 text-right text-green-700 dark:text-green-400">{g.improved}</td>
      <td className="py-2 px-3 text-right">{g.unchanged}</td>
      <td className="py-2 pl-3 text-right text-red-700 dark:text-red-400">{g.worsened}</td>
      <td className="py-2 pl-3">{g.smallSample && <Badge variant="warning">n &lt; 5</Badge>}</td>
    </tr>
  );
}

/** Observational before/after outcomes of interventions, with sample sizes and exclusions. */
export function InterventionEffectivenessCard() {
  const [report, setReport] = useState<EffectivenessReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setReport((await academicApi.getInterventionEffectiveness()).data);
    } catch (e) {
      setError(getSafeErrorMessage(e, 'Could not load intervention analytics.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2"><FlaskConical className="w-5 h-5 text-indigo-600" aria-hidden="true" /> Intervention outcomes</CardTitle>
        <CardDescription>Risk before vs. after each intervention, from official risk snapshots.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {loading && <LoadingState title="Loading intervention analytics" />}
        {!loading && error && <ErrorState title="Analytics unavailable" message={error} onRetry={load} />}
        {!loading && !error && report && (
          report.totalInterventions === 0 ? (
            <EmptyState title="No interventions recorded yet">Outcomes appear once interventions and risk snapshots exist.</EmptyState>
          ) : (
            <>
              <div className="flex items-start gap-2 p-3 rounded-lg bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-900/50 text-xs text-amber-900 dark:text-amber-200">
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" aria-hidden="true" />
                <span><strong>Observational data.</strong> {report.causalityNote}</span>
              </div>

              <dl className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                {[
                  ['Interventions', String(report.totalInterventions)],
                  ['Completion rate', fmt(report.completionRate, '%')],
                  ['With a follow-up', fmt(report.followUpCompletionRate, '%')],
                  ['Evaluable (n)', String(report.overall.evaluable)],
                ].map(([label, value]) => (
                  <div key={label} className="p-3 rounded-lg bg-gray-50 dark:bg-gray-900/60 border border-gray-100 dark:border-gray-800">
                    <dt className="text-xs text-gray-500 dark:text-gray-400">{label}</dt>
                    <dd className="text-lg font-bold text-gray-900 dark:text-gray-100">{value}</dd>
                  </div>
                ))}
              </dl>

              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <caption className="sr-only">Risk change by intervention type</caption>
                  <thead>
                    <tr className="text-left text-gray-500 dark:text-gray-400">
                      <th className="pb-2 pr-3 font-semibold">Group</th>
                      <th className="pb-2 px-3 text-right font-semibold">Total</th>
                      <th className="pb-2 px-3 text-right font-semibold">Evaluable</th>
                      <th className="pb-2 px-3 text-right font-semibold">Mean risk change</th>
                      <th className="pb-2 px-3 text-right font-semibold">Lower</th>
                      <th className="pb-2 px-3 text-right font-semibold">Same</th>
                      <th className="pb-2 pl-3 text-right font-semibold">Higher</th>
                      <th className="pb-2 pl-3" />
                    </tr>
                  </thead>
                  <tbody className="text-gray-700 dark:text-gray-300">
                    <GroupRow g={report.overall} />
                    {report.byType.map(g => <GroupRow key={'t-' + g.group} g={g} />)}
                  </tbody>
                </table>
              </div>

              {Object.keys(report.excluded).length > 0 && (
                <div className="text-xs text-gray-600 dark:text-gray-400">
                  <p className="font-semibold mb-1">Not evaluable:</p>
                  <ul className="list-disc pl-5 space-y-0.5">
                    {Object.entries(report.excluded).map(([reason, count]) => <li key={reason}>{reason}: {count}</li>)}
                  </ul>
                </div>
              )}
              <p className="text-xs text-gray-500 dark:text-gray-400">{report.methodology} {report.marksChangeNote}</p>
            </>
          )
        )}
      </CardContent>
    </Card>
  );
}
