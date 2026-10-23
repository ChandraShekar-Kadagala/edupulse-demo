import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { AlertCircle, TrendingDown, Clock, Activity, Target, CheckCircle2, FileText, Database, ShieldAlert, Sparkles } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import api, { getSafeErrorMessage } from '../services/api';

export default function StudentProfile() {
  const { id } = useParams();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [student, setStudent] = useState<any>(null);
  const [riskAssessment, setRiskAssessment] = useState<any>(null);
  const [interventions, setInterventions] = useState<any[]>([]);
  const [analytics, setAnalytics] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchStudentData = async () => {
    try {
      setError(null);
      const [studentRes, riskRes, intRes, analyticsRes] = await Promise.all([
        api.get('/students/' + id),
        api.get('/students/' + id + '/risk'),
        api.get('/students/' + id + '/interventions'),
        api.get('/students/' + id + '/analytics')
      ]);
      setStudent(studentRes.data);
      setRiskAssessment(riskRes.data);
      setInterventions(intRes.data || []);
      setAnalytics(analyticsRes.data);
    } catch (err: any) {
      console.error("Failed to load student profile:", err);
      setError(getSafeErrorMessage(err, "Failed to load student analytics from server."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStudentData();
  }, [id]);

  const handleRecalculateRisk = async () => {
    try {
      await api.post('/students/' + id + '/risk/recalculate');
      fetchStudentData();
    } catch (err) {
      console.error("Recalculation error:", err);
    }
  };

  if (loading) return <div className="text-gray-400 p-8">Loading student academic analytics...</div>;
  if (error && !student) return <div className="text-red-400 p-8">{error}</div>;
  if (!student) return <div className="text-red-400 p-8">Student record not found.</div>;

  const isHighRisk = riskAssessment?.riskLevel === 'HIGH';
  const riskColor = isHighRisk ? 'text-red-500' : riskAssessment?.riskLevel === 'MEDIUM' ? 'text-amber-500' : 'text-green-500';

  const trendData = analytics?.trendPoints || [];

  return (
    <div className="space-y-6 max-w-6xl mx-auto p-4 md:p-6">
      {/* Header Profile */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center bg-gray-900 border border-gray-800 p-6 rounded-xl">
        <div className="flex items-center space-x-6">
          <div className="h-20 w-20 rounded-full bg-indigo-900 border-2 border-indigo-500 flex items-center justify-center text-3xl font-bold text-indigo-200">
            {student.name ? student.name.charAt(0) : 'S'}
          </div>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-3xl font-bold text-white">{student.name}</h1>
              {analytics?.provenance && (
                <Badge
                  variant={analytics.provenance === 'SYNTHETIC_TEST_FIXTURE' ? 'outline' : 'default'}
                  className={`text-xs ${
                    analytics.provenance === 'INSTITUTIONAL_PORTAL_FIXTURE'
                      ? 'bg-blue-600 text-white border-blue-500'
                      : analytics.provenance === 'OFFICIAL_INSTITUTIONAL_API'
                      ? 'bg-indigo-600 text-white border-indigo-500'
                      : analytics.provenance === 'CSV_FACULTY_IMPORT'
                      ? 'bg-emerald-600 text-white border-emerald-500'
                      : 'border-amber-500 text-amber-400'
                  }`}
                >
                  {analytics.provenance === 'INSTITUTIONAL_PORTAL_FIXTURE'
                    ? '🏛️ Portal Fixture (Verified)'
                    : analytics.provenance === 'OFFICIAL_INSTITUTIONAL_API'
                    ? '🏛️ Official University API'
                    : analytics.provenance === 'CSV_FACULTY_IMPORT'
                    ? '📁 Faculty Import'
                    : '🧪 Synthetic Demo Fixture'}
                </Badge>
              )}
            </div>
            <p className="text-gray-400 mt-1">{student.studentNumber} • {student.department} • Semester {student.currentSemester}</p>
            <div className="flex items-center mt-2 space-x-2">
              <Badge variant="outline">CGPA: {student.currentCgpa}</Badge>
              <Badge variant="outline" className={analytics?.dataStatus === 'COMPLETE' ? 'text-green-400' : 'text-amber-400'}>
                Data Status: {analytics?.dataStatus || 'PARTIAL'}
              </Badge>
            </div>
          </div>
        </div>
        <div className="mt-4 md:mt-0 flex flex-col items-end">
          <div className="text-sm text-gray-400 mb-1">
            Current Risk Indicator {riskAssessment?.calculatedAt ? `(Updated: ${new Date(riskAssessment.calculatedAt).toLocaleDateString()})` : ''}
          </div>
          <div className="flex items-center space-x-3">
            <span className={'text-4xl font-bold ' + riskColor}>{riskAssessment?.riskScore || 0}</span>
            <Badge variant={isHighRisk ? 'destructive' : 'warning'} className="text-sm px-3 py-1">
              {riskAssessment?.riskLevel || 'LOW'}
            </Badge>
          </div>
          {user?.role !== 'STUDENT' && (
            <Button variant="outline" size="sm" className="mt-3" onClick={handleRecalculateRisk}>
              Recalculate Risk
            </Button>
          )}
        </div>
      </div>

      {/* Analytics KPI Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="bg-gray-900 border-gray-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-400">Overall Attendance</CardTitle>
          </CardHeader>
          <CardContent>
            {analytics?.hasAttendanceData ? (
              <div>
                <div className="text-2xl font-bold text-white">{analytics.overallAttendancePercentage}%</div>
                <p className="text-xs text-gray-400 mt-1">{analytics.presentClasses} of {analytics.totalClasses} sessions attended</p>
              </div>
            ) : (
              <div>
                <div className="text-lg font-semibold text-gray-500">No Attendance Records</div>
                <p className="text-xs text-gray-500 mt-1">Pending faculty upload</p>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="bg-gray-900 border-gray-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-400">Average Assessment Score</CardTitle>
          </CardHeader>
          <CardContent>
            {analytics?.hasMarksData ? (
              <div>
                <div className="text-2xl font-bold text-white">{analytics.overallAverageMarksPercentage}%</div>
                <p className="text-xs text-gray-400 mt-1">Evaluated across {analytics.totalAssessments} assessment(s)</p>
              </div>
            ) : (
              <div>
                <div className="text-lg font-semibold text-gray-500">No Assessment Records</div>
                <p className="text-xs text-gray-500 mt-1">Pending exam evaluations</p>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="bg-gray-900 border-gray-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-400">Coursework Status</CardTitle>
          </CardHeader>
          <CardContent>
            {analytics?.hasAssignmentData ? (
              <div>
                <div className="text-2xl font-bold text-white">{analytics.submittedAssignments} / {analytics.totalAssignments}</div>
                <p className={`text-xs mt-1 ${analytics.missingAssignments > 0 ? 'text-red-400' : 'text-gray-400'}`}>
                  {analytics.missingAssignments} overdue or missing
                </p>
              </div>
            ) : (
              <div>
                <div className="text-lg font-semibold text-gray-500">No Assignments Tracked</div>
                <p className="text-xs text-gray-500 mt-1">No course assignments assigned</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column - Explainable Risk & Recommendations */}
        <div className="lg:col-span-1 space-y-6">
          <Card className="border-red-900/30 bg-gray-900">
            <CardHeader>
              <CardTitle className="flex items-center text-red-400">
                <AlertCircle className="w-5 h-5 mr-2" />
                Explainable Risk Factors
              </CardTitle>
              <CardDescription>Verified inputs contributing to academic friction</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {riskAssessment?.riskFactors?.map((factor: any, idx: number) => (
                <div key={idx} className="p-3 bg-gray-950 rounded border border-gray-800">
                  <div className="flex justify-between items-start mb-1">
                    <span className="font-semibold text-gray-200">{factor.factorType || factor.factorName}</span>
                    <Badge variant={factor.impactLevel === 'HIGH' || factor.impactScore >= 70 ? 'destructive' : 'warning'}>
                      {factor.impactLevel || (factor.impactScore >= 70 ? 'HIGH' : 'MEDIUM')}
                    </Badge>
                  </div>
                  <p className="text-sm text-gray-400">{factor.description}</p>
                </div>
              ))}
              {(!riskAssessment?.riskFactors || riskAssessment.riskFactors.length === 0) && (
                <div className="text-gray-500 italic">No adverse risk factors detected.</div>
              )}
            </CardContent>
          </Card>

          {/* Actionable Recommendations */}
          <Card className="bg-gray-900 border-gray-800">
            <CardHeader>
              <CardTitle className="flex items-center text-indigo-400">
                <Target className="w-5 h-5 mr-2" />
                Actionable Recommendations
              </CardTitle>
              <CardDescription>Targeted next steps with clear rationale</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {analytics?.recommendations && analytics.recommendations.length > 0 ? (
                analytics.recommendations.map((rec: any, idx: number) => (
                  <div key={idx} className="p-3 bg-gray-950 rounded border border-gray-800 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-semibold text-white">{rec.title}</span>
                      <Badge variant={rec.priority === 'HIGH' ? 'destructive' : 'outline'} className="text-[10px]">
                        {rec.priority}
                      </Badge>
                    </div>
                    <p className="text-xs text-gray-400">{rec.rationale}</p>
                    <p className="text-xs text-indigo-300 font-medium pt-1">Next Step: {rec.nextStep}</p>
                  </div>
                ))
              ) : (
                <div className="text-gray-500 text-sm italic">No specific recommendations needed at this time.</div>
              )}
            </CardContent>
          </Card>

          {/* Strengths & Improvement Areas */}
          <Card className="bg-gray-900 border-gray-800">
            <CardHeader>
              <CardTitle className="flex items-center text-green-400 text-sm">
                <Sparkles className="w-4 h-4 mr-2" />
                Academic Insights Summary
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-xs">
              {analytics?.strengths && analytics.strengths.length > 0 && (
                <div>
                  <span className="font-semibold text-green-400">Strengths</span>
                  <ul className="list-disc list-inside text-gray-300 space-y-1 mt-1">
                    {analytics.strengths.map((s: string, idx: number) => (
                      <li key={idx}>{s}</li>
                    ))}
                  </ul>
                </div>
              )}
              {analytics?.improvementAreas && analytics.improvementAreas.length > 0 && (
                <div className="pt-2 border-t border-gray-800">
                  <span className="font-semibold text-amber-400">Focus Areas</span>
                  <ul className="list-disc list-inside text-gray-300 space-y-1 mt-1">
                    {analytics.improvementAreas.map((ia: string, idx: number) => (
                      <li key={idx}>{ia}</li>
                    ))}
                  </ul>
                </div>
              )}
              {analytics?.upcomingConcerns && analytics.upcomingConcerns.length > 0 && (
                <div className="pt-2 border-t border-gray-800">
                  <span className="font-semibold text-red-400">Upcoming Concerns</span>
                  <ul className="list-disc list-inside text-gray-300 space-y-1 mt-1">
                    {analytics.upcomingConcerns.map((uc: string, idx: number) => (
                      <li key={idx}>{uc}</li>
                    ))}
                  </ul>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Right Column - Genuine Performance Trends & Course Breakdown */}
        <div className="lg:col-span-2 space-y-6">
          <Card className="bg-gray-900 border-gray-800">
            <CardHeader>
              <CardTitle className="flex items-center">
                <TrendingDown className="w-5 h-5 mr-2 text-gray-400" />
                Recorded Performance Trajectory
              </CardTitle>
              <CardDescription>Chronological trajectory based strictly on verified session records</CardDescription>
            </CardHeader>
            <CardContent className="h-72">
              {trendData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={trendData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                    <XAxis dataKey="name" stroke="#9ca3af" />
                    <YAxis stroke="#9ca3af" domain={[0, 100]} />
                    <Tooltip 
                      contentStyle={{ backgroundColor: '#1f2937', border: '1px solid #374151' }}
                      itemStyle={{ color: '#f3f4f6' }}
                    />
                    <Line type="monotone" dataKey="attendance" stroke="#8b5cf6" strokeWidth={3} dot={{ r: 4 }} name="Attendance %" connectNulls />
                    <Line type="monotone" dataKey="marks" stroke="#10b981" strokeWidth={3} dot={{ r: 4 }} name="Average Marks %" connectNulls />
                  </LineChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex flex-col items-center justify-center text-gray-500">
                  <Database className="w-8 h-8 mb-2 opacity-50" />
                  <p className="text-sm">Insufficient historical trend points recorded.</p>
                  <p className="text-xs text-gray-600 mt-1">Trends will appear automatically as multi-period records are ingested.</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Course-level Attendance Breakdown */}
          {analytics?.courseAttendanceBreakdown && analytics.courseAttendanceBreakdown.length > 0 && (
            <Card className="bg-gray-900 border-gray-800">
              <CardHeader>
                <CardTitle className="text-sm font-medium">Course-Level Attendance Breakdown</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full text-xs text-left text-gray-300">
                    <thead className="text-[11px] text-gray-400 bg-gray-950 uppercase">
                      <tr>
                        <th className="px-3 py-2">Course</th>
                        <th className="px-3 py-2">Attended / Total</th>
                        <th className="px-3 py-2">Percentage</th>
                        <th className="px-3 py-2">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {analytics.courseAttendanceBreakdown.map((ca: any, idx: number) => (
                        <tr key={idx} className="border-b border-gray-800">
                          <td className="px-3 py-2 font-medium text-white">{ca.courseCode} - {ca.courseName}</td>
                          <td className="px-3 py-2">{ca.presentClasses} / {ca.totalClasses}</td>
                          <td className="px-3 py-2 font-semibold">{ca.attendancePercentage != null ? `${ca.attendancePercentage}%` : 'N/A'}</td>
                          <td className="px-3 py-2">
                            <Badge variant={ca.status === 'BELOW_THRESHOLD' ? 'destructive' : 'outline'} className="text-[10px]">
                              {ca.status}
                            </Badge>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Assessment Marks Breakdown */}
          {analytics?.assessmentBreakdown && analytics.assessmentBreakdown.length > 0 && (
            <Card className="bg-gray-900 border-gray-800">
              <CardHeader>
                <CardTitle className="text-sm font-medium">Evaluated Assessments & Marks</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full text-xs text-left text-gray-300">
                    <thead className="text-[11px] text-gray-400 bg-gray-950 uppercase">
                      <tr>
                        <th className="px-3 py-2">Course</th>
                        <th className="px-3 py-2">Assessment</th>
                        <th className="px-3 py-2">Type</th>
                        <th className="px-3 py-2">Score</th>
                        <th className="px-3 py-2">Percentage</th>
                      </tr>
                    </thead>
                    <tbody>
                      {analytics.assessmentBreakdown.map((m: any, idx: number) => (
                        <tr key={idx} className="border-b border-gray-800">
                          <td className="px-3 py-2 text-white">{m.courseCode}</td>
                          <td className="px-3 py-2">{m.assessmentName}</td>
                          <td className="px-3 py-2">{m.assessmentType}</td>
                          <td className="px-3 py-2">
                            {m.marksObtained != null ? `${m.marksObtained} / ${m.maxMarks}` : <span className="text-amber-400 italic">Pending Evaluation</span>}
                          </td>
                          <td className="px-3 py-2 font-semibold">
                            {m.percentage != null ? <span className="text-green-400">{m.percentage}%</span> : <span className="text-gray-500">—</span>}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Intervention History */}
          <Card className="bg-gray-900 border-gray-800">
            <CardHeader>
              <CardTitle className="flex items-center">
                <Activity className="w-5 h-5 mr-2 text-gray-400" />
                Intervention History
              </CardTitle>
            </CardHeader>
            <CardContent>
              {interventions.length > 0 ? (
                <div className="space-y-4">
                  {interventions.map((intervention) => (
                    <div key={intervention.id} className="bg-gray-800/50 p-4 rounded border border-gray-700">
                      <div className="flex justify-between items-center mb-1">
                        <span className="font-bold text-gray-200">{intervention.type}</span>
                        <Badge variant="outline">{intervention.status}</Badge>
                      </div>
                      <p className="text-sm text-gray-400 mb-2">{intervention.description}</p>
                      {intervention.assignedFacultyName && (
                        <div className="text-xs text-gray-500">Assigned Faculty: {intervention.assignedFacultyName}</div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-6 text-gray-500 text-sm">
                  No previous interventions recorded.
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
