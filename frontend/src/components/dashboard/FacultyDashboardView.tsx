import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { BookOpen, CalendarDays, GraduationCap, RefreshCw, Users } from 'lucide-react';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { academicApi, getSafeErrorMessage } from '../../services/api';
import type { FacultyDashboard } from '../../types';
import { EmptyState, ErrorState, LoadingState, fmt, riskBadgeVariant } from '../common/StateViews';
import { InterventionEffectivenessCard } from './InterventionEffectivenessCard';

const RISK_COLORS = { LOW: '#059669', MEDIUM: '#d97706', HIGH: '#dc2626' } as const;

function Kpi({ icon, label, value, hint }: { icon: React.ReactNode; label: string; value: string; hint?: string }) {
  return (
    <Card>
      <CardContent className="p-4 flex items-start gap-3">
        <div className="p-2 rounded-lg bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-300" aria-hidden="true">{icon}</div>
        <div>
          <p className="text-xs text-gray-500 dark:text-gray-400">{label}</p>
          <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{value}</p>
          {hint && <p className="text-[11px] text-gray-500 dark:text-gray-400">{hint}</p>}
        </div>
      </CardContent>
    </Card>
  );
}

export function FacultyDashboardView() {
  const [data, setData] = useState<FacultyDashboard | null>(null);
  const [courseCode, setCourseCode] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<{ status?: number; message: string } | null>(null);

  const load = useCallback(async (code?: string) => {
    setLoading(true);
    setError(null);
    try {
      setData((await academicApi.getMyFacultyDashboard(code)).data);
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } })?.response?.status;
      setError({ status, message: getSafeErrorMessage(e, 'Unable to load the faculty dashboard.') });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(courseCode); }, [load, courseCode]);

  if (loading && !data) return <LoadingState title="Loading course dashboard" detail="Fetching roster, attendance, marks and risk." />;
  if (error?.status === 404) {
    return <ErrorState title="No faculty profile" message="This account has no faculty profile, so there is no course dashboard to show." />;
  }
  if (error && !data) return <ErrorState title="Dashboard unavailable" message={error.message} onRetry={() => load(courseCode)} />;
  if (!data) return null;

  const f = data.faculty;
  const risk = [
    { name: 'Low', key: 'LOW' as const, value: data.lowRiskCount },
    { name: 'Medium', key: 'MEDIUM' as const, value: data.mediumRiskCount },
    { name: 'High', key: 'HIGH' as const, value: data.highRiskCount },
  ];

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      <header className="p-6 rounded-2xl bg-gradient-to-r from-indigo-900 via-indigo-800 to-slate-900 text-white shadow">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-extrabold">{f.name ?? f.username}</h1>
            <p className="text-sm text-indigo-200 mt-1">{[f.title, f.department].filter(Boolean).join(' · ') || 'Faculty'}</p>
          </div>
          <div className="flex flex-wrap items-center gap-2" role="tablist" aria-label="Assigned courses">
            {f.assignedCourses.map(c => (
              <button
                key={c.courseCode}
                role="tab"
                aria-selected={data.courseCode === c.courseCode}
                onClick={() => setCourseCode(c.courseCode)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors ${data.courseCode === c.courseCode ? 'bg-white text-indigo-950' : 'bg-white/10 text-white hover:bg-white/20'}`}
              >
                {c.courseCode}
              </button>
            ))}
            <Button variant="outline" size="sm" onClick={() => load(courseCode)} className="bg-white/10 border-white/20 text-white hover:bg-white/20" aria-label="Refresh dashboard">
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} aria-hidden="true" />
            </Button>
          </div>
        </div>
        {error && <p role="alert" className="mt-3 text-xs text-red-200">{error.message}</p>}
      </header>

      {!data.courseCode ? (
        <EmptyState title="No course assigned">This faculty member has no course assignment yet. An administrator can add one.</EmptyState>
      ) : (
        <>
          <section aria-label="Course summary">
            <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100">{data.courseCode} · {data.courseName}</h2>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {[data.credits != null ? `${data.credits} credits` : null, data.classroom, data.slotDetails].filter(Boolean).join(' · ')}
            </p>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mt-3">
              <Kpi icon={<Users className="w-5 h-5" />} label="Students on roster" value={String(data.enrolledStudentsCount)} />
              <Kpi icon={<CalendarDays className="w-5 h-5" />} label="Sessions recorded" value={String(data.totalConductedSessions)} hint="Distinct attendance dates" />
              <Kpi icon={<GraduationCap className="w-5 h-5" />} label="Average attendance" value={fmt(data.courseAverageAttendance, '%')} />
              <Kpi icon={<BookOpen className="w-5 h-5" />} label="Average evaluated marks" value={fmt(data.courseAverageMarks, '%')} />
            </div>
          </section>

          <div className="grid lg:grid-cols-3 gap-6">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>Students</CardTitle>
                <CardDescription>Attendance and marks in {data.courseCode}; risk is the official overall score.</CardDescription>
              </CardHeader>
              <CardContent>
                {data.students.length === 0 ? (
                  <EmptyState title="No students on this course yet" />
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="text-left text-xs text-gray-500 dark:text-gray-400">
                          <th className="pb-2 pr-3">Student</th>
                          <th className="pb-2 px-3 text-right">Attendance</th>
                          <th className="pb-2 px-3 text-right">Marks</th>
                          <th className="pb-2 px-3 text-right">Missing</th>
                          <th className="pb-2 pl-3">Risk</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.students.map(s => (
                          <tr key={s.studentId} className="border-t border-gray-100 dark:border-gray-800 align-top">
                            <td className="py-2 pr-3">
                              <Link to={`/students/${s.studentId}`} className="font-semibold text-indigo-700 dark:text-indigo-300 hover:underline">{s.studentName ?? s.studentNumber}</Link>
                              <div className="text-[11px] text-gray-500 dark:text-gray-400">{s.studentNumber} · {s.provenance}</div>
                              <div className="text-[11px] text-gray-600 dark:text-gray-400">{s.riskReason}</div>
                            </td>
                            <td className="py-2 px-3 text-right whitespace-nowrap">
                              {fmt(s.courseAttendancePercentage, '%')}
                              <div className="text-[11px] text-gray-500">{s.coursePresentClasses}/{s.courseTotalClasses}</div>
                            </td>
                            <td className="py-2 px-3 text-right">{fmt(s.courseAverageMarksPercentage, '%')}</td>
                            <td className="py-2 px-3 text-right">{s.missingAssignmentsCount}</td>
                            <td className="py-2 pl-3 whitespace-nowrap">
                              <Badge variant={riskBadgeVariant(s.riskLevel)}>{s.riskLevel ?? 'NO DATA'}</Badge>
                              <div className="text-[11px] text-gray-500 mt-0.5">{fmt(s.riskScore)} / 100</div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Risk distribution</CardTitle>
                <CardDescription>{data.enrolledStudentsCount} student(s) on this roster</CardDescription>
              </CardHeader>
              <CardContent>
                {risk.every(r => r.value === 0) ? (
                  <EmptyState title="No risk results yet" />
                ) : (
                  <>
                    <div className="h-48" aria-hidden="true">
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie data={risk} dataKey="value" nameKey="name" innerRadius={45} outerRadius={70} paddingAngle={2}>
                            {risk.map(r => <Cell key={r.key} fill={RISK_COLORS[r.key]} />)}
                          </Pie>
                          <Tooltip />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                    <ul className="text-sm space-y-1">
                      {risk.map(r => (
                        <li key={r.key} className="flex justify-between">
                          <span className="flex items-center gap-2"><span className="w-2.5 h-2.5 rounded-full" style={{ background: RISK_COLORS[r.key] }} />{r.name}</span>
                          <span className="font-semibold">{r.value}</span>
                        </li>
                      ))}
                    </ul>
                  </>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="grid lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Assessments</CardTitle>
                <CardDescription>Class average is the mean percentage of recorded marks.</CardDescription>
              </CardHeader>
              <CardContent>
                {data.assessments.length === 0 ? <EmptyState title="No assessments defined for this course" /> : (
                  <ul className="divide-y divide-gray-100 dark:divide-gray-800 text-sm">
                    {data.assessments.map(a => (
                      <li key={a.id} className="py-2 flex justify-between gap-3">
                        <span>
                          <span className="font-medium text-gray-900 dark:text-gray-100">{a.name}</span>
                          <span className="block text-[11px] text-gray-500">Max {a.maxMarks}{a.weightage != null ? ` · weight ${a.weightage}` : ''}</span>
                        </span>
                        <span className="text-right">
                          <Badge variant={a.status === 'EVALUATED' ? 'success' : 'outline'}>{a.status}</Badge>
                          <span className="block text-[11px] text-gray-500 mt-0.5">avg {fmt(a.classAverage, '%')}</span>
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Open interventions</CardTitle>
                <CardDescription>For students on this roster</CardDescription>
              </CardHeader>
              <CardContent>
                {data.activeInterventions.length === 0 ? <EmptyState title="No interventions for this roster" /> : (
                  <ul className="divide-y divide-gray-100 dark:divide-gray-800 text-sm">
                    {data.activeInterventions.map(i => (
                      <li key={i.id} className="py-2">
                        <div className="flex justify-between gap-2">
                          <span className="font-medium text-gray-900 dark:text-gray-100">{i.type}</span>
                          <Badge variant="outline">{i.status}</Badge>
                        </div>
                        <p className="text-[11px] text-gray-500">{i.studentName} · {i.priority ?? 'No priority'}{i.dueDate ? ` · due ${i.dueDate}` : ''}</p>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          </div>
        </>
      )}

      <InterventionEffectivenessCard />

      {data.riskEngineSummary && (
        <Card>
          <CardHeader><CardTitle>How risk is calculated</CardTitle></CardHeader>
          <CardContent className="text-xs text-gray-600 dark:text-gray-300 space-y-1">
            <p className="font-mono">{data.riskEngineSummary.formula}</p>
            <p>{data.riskEngineSummary.attendanceThresholdRule}</p>
            <p>{data.riskEngineSummary.marksThresholdRule}</p>
            <p>{data.riskEngineSummary.missingAssignmentRule}</p>
            <p>{data.riskEngineSummary.pendingAssessmentRule}</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
