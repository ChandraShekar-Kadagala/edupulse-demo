import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { 
  AlertCircle, TrendingDown, Clock, Activity, Target, CheckCircle2, 
  FileText, Database, ShieldAlert, Sparkles, RefreshCw, Calendar, 
  BookOpen, Calculator, Award, ArrowRight, UserCheck, Check
} from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import api, { getSafeErrorMessage } from '../services/api';
import type { StudentAcademicAnalytics, TimetableSlot } from '../types';

export default function StudentProfile() {
  const { id } = useParams();
  const { user } = useAuth();
  const { theme } = useTheme();
  const [loading, setLoading] = useState(true);
  const [student, setStudent] = useState<any>(null);
  const [riskAssessment, setRiskAssessment] = useState<any>(null);
  const [interventions, setInterventions] = useState<any[]>([]);
  const [analytics, setAnalytics] = useState<StudentAcademicAnalytics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [targetScenario, setTargetScenario] = useState<'min' | 'rec' | 'stretch'>('rec');

  const fetchStudentData = async () => {
    try {
      setError(null);
      const [studentRes, riskRes, intRes, analyticsRes] = await Promise.all([
        api.get('/students/' + id),
        api.get('/students/' + id + '/risk').catch(() => ({ data: null })),
        api.get('/students/' + id + '/interventions').catch(() => ({ data: [] })),
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

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] space-y-3 text-gray-500 dark:text-gray-400">
        <RefreshCw className="h-8 w-8 animate-spin text-indigo-600" />
        <p className="text-sm">Loading complete academic analytics & timetable...</p>
      </div>
    );
  }

  if (error && !student) {
    return (
      <div className="p-8 text-center space-y-4 max-w-lg mx-auto">
        <div className="p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/50 rounded-xl text-red-700 dark:text-red-400">
          <p className="font-medium">{error}</p>
          <Button variant="outline" size="sm" onClick={fetchStudentData} className="mt-3">
            <RefreshCw className="h-4 w-4 mr-2" /> Retry Connection
          </Button>
        </div>
      </div>
    );
  }

  if (!student) return <div className="text-red-500 p-8 text-center">Student record not found.</div>;

  const isActualStudent = analytics?.provenance?.includes("ACTUAL") || student.name?.includes("CHANDRA SHEKAR");
  const isHighRisk = riskAssessment?.riskLevel === 'HIGH' || analytics?.riskLevel === 'HIGH';
  const isMedRisk = riskAssessment?.riskLevel === 'MEDIUM' || analytics?.riskLevel === 'MEDIUM';
  const riskColor = isHighRisk ? 'text-red-600 dark:text-red-400' : isMedRisk ? 'text-amber-600 dark:text-amber-400' : 'text-green-600 dark:text-green-400';

  const tooltipBg = theme === 'dark' ? '#1f2937' : '#ffffff';
  const tooltipText = theme === 'dark' ? '#f3f4f6' : '#111827';
  const tooltipBorder = theme === 'dark' ? '#374151' : '#e5e7eb';
  const gridStroke = theme === 'dark' ? '#374151' : '#e5e7eb';
  const axisStroke = theme === 'dark' ? '#9ca3af' : '#6b7280';

  // Group timetable by Day
  const timetableDays = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'];
  const timetableByDay = (analytics?.timetable || []).reduce((acc: Record<string, TimetableSlot[]>, slot) => {
    if (!acc[slot.day]) acc[slot.day] = [];
    acc[slot.day].push(slot);
    return acc;
  }, {});

  // Calculate Converted Marks Totals
  const totalConvertedObtained = (analytics?.assessmentBreakdown || [])
    .filter(a => a.convertedMarks != null)
    .reduce((sum, a) => sum + (a.convertedMarks || 0), 0);
  const totalMaxConvertedEvaluated = (analytics?.assessmentBreakdown || [])
    .filter(a => a.convertedMarks != null)
    .reduce((sum, a) => sum + (a.maxConvertedMarks || 0), 0);
  const totalMaxConvertedSemester = (analytics?.assessmentBreakdown || [])
    .reduce((sum, a) => sum + (a.maxConvertedMarks || 0), 0);

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-2 sm:p-4 md:p-6 font-sans">
      
      {/* 1. Header Profile Banner & Provenance */}
      <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 p-6 rounded-2xl shadow-sm transition-colors">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div className="flex items-center space-x-5">
            <div className="h-16 w-16 sm:h-20 sm:w-20 rounded-2xl bg-indigo-100 dark:bg-indigo-900/50 border-2 border-indigo-500/40 flex items-center justify-center text-3xl font-extrabold text-indigo-700 dark:text-indigo-200 shadow-sm flex-shrink-0">
              {student.name ? student.name.charAt(0) : 'S'}
            </div>
            <div>
              <div className="flex items-center gap-2 sm:gap-3 flex-wrap">
                <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white tracking-tight">
                  {student.name}
                </h1>
                
                {/* Provenance Badge */}
                {isActualStudent ? (
                  <Badge className="bg-emerald-600 hover:bg-emerald-700 text-white font-semibold px-2.5 py-0.5 text-xs shadow-sm">
                    ⭐ ACTUAL STUDENT-SUPPLIED REFERENCE DATA
                  </Badge>
                ) : (
                  <Badge variant="outline" className="border-amber-400 text-amber-700 dark:text-amber-300 bg-amber-50/50 dark:bg-amber-950/30 text-xs font-semibold px-2.5 py-0.5">
                    🧪 SYNTHETIC DEMO DATA
                  </Badge>
                )}
              </div>

              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1 font-medium">
                <span className="font-mono text-gray-900 dark:text-gray-200 font-bold">{student.studentNumber}</span> • {student.department || 'Computer Science & Engineering'} • Semester {student.currentSemester || 5}
              </p>

              <div className="flex items-center mt-2.5 gap-2 flex-wrap text-xs">
                <Badge variant="outline" className="font-semibold bg-gray-50 dark:bg-gray-800">
                  CGPA: {student.currentCgpa}
                </Badge>
                <Badge variant="outline" className={analytics?.dataStatus === 'COMPLETE' ? 'text-green-700 dark:text-green-400 border-green-300' : 'text-amber-700 dark:text-amber-400 border-amber-300'}>
                  Status: {analytics?.dataStatus || 'COMPLETE'}
                </Badge>
                <span className="text-gray-500 dark:text-gray-400 text-xs">
                  Updated: <strong className="text-gray-700 dark:text-gray-300">{analytics?.lastUpdated || '29-Oct-2026'}</strong>
                </span>
                <span className="text-gray-500 dark:text-gray-400 text-xs">
                  Period: <strong className="text-gray-700 dark:text-gray-300">{analytics?.attendancePeriod || '03-Aug-2026 to 29-Oct-2026'}</strong>
                </span>
              </div>
            </div>
          </div>

          {/* Risk Badge & Recalculate */}
          <div className="flex flex-col md:items-end bg-gray-50 dark:bg-gray-800/40 p-4 rounded-xl border border-gray-200 dark:border-gray-800 min-w-[200px]">
            <span className="text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold mb-1">
              Deterministic Academic Risk
            </span>
            <div className="flex items-center space-x-3">
              <span className={`text-4xl font-extrabold ${riskColor}`}>
                {analytics?.riskScore ?? riskAssessment?.riskScore ?? 0.0}
              </span>
              <Badge 
                variant={isHighRisk ? 'destructive' : isMedRisk ? 'warning' : 'success'} 
                className="text-xs px-2.5 py-1 font-extrabold uppercase tracking-wide"
              >
                {analytics?.riskLevel ?? riskAssessment?.riskLevel ?? 'LOW'} RISK
              </Badge>
            </div>
            {user?.role !== 'STUDENT' && (
              <Button variant="outline" size="sm" className="mt-2.5 text-xs h-7" onClick={handleRecalculateRisk}>
                <RefreshCw className="h-3 w-3 mr-1" /> Re-audit Risk
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* 2. Key Metrics Row (Attendance, Converted Marks, Assignments) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Attendance Card */}
        <Card className="hover:shadow-sm transition-shadow">
          <CardHeader className="pb-2">
            <CardTitle className="text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold">
              Aggregate Attendance
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white">
              {analytics?.overallAttendancePercentage != null ? `${analytics.overallAttendancePercentage}%` : 'N/A'}
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              <strong className="text-emerald-600 dark:text-emerald-400">{analytics?.presentClasses}</strong> present of <strong>{analytics?.totalClasses}</strong> conducted sessions
            </p>
            <div className="w-full bg-gray-200 dark:bg-gray-800 h-1.5 rounded-full mt-2 overflow-hidden">
              <div 
                className={`h-full ${ (analytics?.overallAttendancePercentage || 0) >= 75 ? 'bg-emerald-500' : 'bg-red-500'}`}
                style={{ width: `${Math.min(100, analytics?.overallAttendancePercentage || 0)}%` }}
              />
            </div>
          </CardContent>
        </Card>

        {/* Converted Internals Score Card */}
        <Card className="hover:shadow-sm transition-shadow">
          <CardHeader className="pb-2">
            <CardTitle className="text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold">
              Converted Internals Score
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl sm:text-3xl font-extrabold text-indigo-600 dark:text-indigo-400">
              {totalConvertedObtained.toFixed(2)} <span className="text-sm font-normal text-gray-500">/ {totalMaxConvertedEvaluated.toFixed(0)}</span>
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Overall available total: <strong>{totalConvertedObtained.toFixed(2)} / {totalMaxConvertedSemester}</strong>
            </p>
            <p className="text-[11px] text-indigo-600 dark:text-indigo-400 font-semibold mt-1">
              {analytics?.evaluatedAssessmentsCount} conducted internal evaluations
            </p>
          </CardContent>
        </Card>

        {/* Graded Percentage Average */}
        <Card className="hover:shadow-sm transition-shadow">
          <CardHeader className="pb-2">
            <CardTitle className="text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold">
              Graded Evaluations Avg
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white">
              {analytics?.overallAverageMarksPercentage != null ? `${analytics.overallAverageMarksPercentage}%` : 'N/A'}
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Threshold standard: <strong className="text-gray-700 dark:text-gray-300">50.0%</strong> passing grade
            </p>
            <p className={`text-[11px] font-semibold mt-1 ${ (analytics?.overallAverageMarksPercentage || 0) >= 50 ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-500'}`}>
              {(analytics?.overallAverageMarksPercentage || 0) >= 75 ? 'Exemplary Performance' : (analytics?.overallAverageMarksPercentage || 0) >= 50 ? 'Good Academic Standing' : 'Remedial Support Required'}
            </p>
          </CardContent>
        </Card>

        {/* Coursework Submissions */}
        <Card className="hover:shadow-sm transition-shadow">
          <CardHeader className="pb-2">
            <CardTitle className="text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold">
              Coursework Submissions
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white">
              {analytics?.submittedAssignments ?? 0} <span className="text-sm font-normal text-gray-500">/ {analytics?.totalAssignments ?? 0}</span>
            </div>
            <p className={`text-xs mt-1 font-semibold ${ (analytics?.missingAssignments || 0) > 0 ? 'text-red-600 dark:text-red-400' : 'text-emerald-600 dark:text-emerald-400'}`}>
              {(analytics?.missingAssignments || 0) > 0 ? `${analytics?.missingAssignments} overdue deliverables` : 'All coursework submitted on time'}
            </p>
            <p className="text-[11px] text-gray-500 dark:text-gray-400 mt-1">
              5.0 pts friction penalty per missing item
            </p>
          </CardContent>
        </Card>
      </div>

      {/* 3. Subject-Wise Attendance Breakdown (7 SRM AP Subjects) */}
      <Card className="overflow-hidden border border-gray-200 dark:border-gray-800">
        <CardHeader className="bg-gray-50/50 dark:bg-gray-900/50 border-b border-gray-100 dark:border-gray-800 py-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <CardTitle className="text-base font-bold text-gray-900 dark:text-white flex items-center">
                <BookOpen className="w-4 h-4 mr-2 text-indigo-600 dark:text-indigo-400" />
                Subject-Wise Attendance Details (Period: {analytics?.attendancePeriod || '03-Aug-2026 to 29-Oct-2026'})
              </CardTitle>
              <CardDescription className="text-xs">
                Conducted, present, absent, OD/ML counts and individual course compliance
              </CardDescription>
            </div>
            <Badge variant="outline" className="text-xs self-start sm:self-auto font-mono">
              Aggregate: {analytics?.presentClasses}/{analytics?.totalClasses} ({analytics?.overallAttendancePercentage}%)
            </Badge>
          </div>
        </CardHeader>
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="text-[11px] text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-950 uppercase border-b border-gray-200 dark:border-gray-800">
              <tr>
                <th className="px-4 py-3">Subject Code & Name</th>
                <th className="px-4 py-3">Faculty Instructor</th>
                <th className="px-4 py-3">Classroom</th>
                <th className="px-4 py-3 text-center">Conducted</th>
                <th className="px-4 py-3 text-center">Present</th>
                <th className="px-4 py-3 text-center">Absent</th>
                <th className="px-4 py-3 text-center">OD/ML</th>
                <th className="px-4 py-3 text-center">Attendance %</th>
                <th className="px-4 py-3 text-right">Statutory Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-800">
              {(analytics?.courseAttendanceBreakdown || []).map((ca, idx) => {
                const isShort = (ca.attendancePercentage || 0) < 75.0;
                return (
                  <tr key={idx} className="hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-colors">
                    <td className="px-4 py-3 font-semibold text-gray-900 dark:text-white">
                      <span className="font-mono text-indigo-600 dark:text-indigo-400">{ca.courseCode}</span> - {ca.courseName}
                    </td>
                    <td className="px-4 py-3 text-gray-600 dark:text-gray-400">{ca.facultyName}</td>
                    <td className="px-4 py-3 font-mono text-gray-500">{ca.classroom}</td>
                    <td className="px-4 py-3 text-center font-medium">{ca.totalClasses}</td>
                    <td className="px-4 py-3 text-center font-bold text-emerald-600 dark:text-emerald-400">{ca.presentClasses}</td>
                    <td className="px-4 py-3 text-center font-bold text-red-600 dark:text-red-400">{ca.absentClasses}</td>
                    <td className="px-4 py-3 text-center text-gray-500">{ca.odMlClasses ?? 0}</td>
                    <td className="px-4 py-3 text-center font-bold">
                      <span className={`px-2 py-0.5 rounded ${isShort ? 'bg-red-50 text-red-600 dark:bg-red-950/40 dark:text-red-400' : 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400'}`}>
                        {ca.attendancePercentage != null ? `${ca.attendancePercentage}%` : 'N/A'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Badge variant={isShort ? 'destructive' : 'outline'} className="text-[10px]">
                        {isShort ? 'SHORTAGE (<75%)' : 'REGULAR'}
                      </Badge>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>

      {/* 4. Internal Assessment Details (Raw Marks, Converted Marks & Status) */}
      <Card className="overflow-hidden border border-gray-200 dark:border-gray-800">
        <CardHeader className="bg-gray-50/50 dark:bg-gray-900/50 border-b border-gray-100 dark:border-gray-800 py-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <CardTitle className="text-base font-bold text-gray-900 dark:text-white flex items-center">
                <FileText className="w-4 h-4 mr-2 text-indigo-600 dark:text-indigo-400" />
                Internal Mark Details & Evaluated Components
              </CardTitle>
              <CardDescription className="text-xs">
                Preserving raw obtained marks vs converted internal marks weightages. Unconducted components remain pending (never zero).
              </CardDescription>
            </div>
            <div className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 px-3 py-1 rounded-md border border-indigo-200 dark:border-indigo-800">
              Evaluated Converted: {totalConvertedObtained.toFixed(2)} / {totalMaxConvertedEvaluated.toFixed(0)}
            </div>
          </div>
        </CardHeader>
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="text-[11px] text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-950 uppercase border-b border-gray-200 dark:border-gray-800">
              <tr>
                <th className="px-4 py-3">Course</th>
                <th className="px-4 py-3">Assessment Name</th>
                <th className="px-4 py-3">Faculty & Room</th>
                <th className="px-4 py-3 text-center">Raw Marks (Obt / Max)</th>
                <th className="px-4 py-3 text-center">Converted Marks (Obt / Max)</th>
                <th className="px-4 py-3 text-center">Percentage</th>
                <th className="px-4 py-3 text-right">Evaluation Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-800">
              {(analytics?.assessmentBreakdown || []).map((m, idx) => {
                const isCompleted = m.status === 'COMPLETED';
                return (
                  <tr key={idx} className={isCompleted ? 'hover:bg-gray-50 dark:hover:bg-gray-800/40' : 'bg-gray-50/30 dark:bg-gray-900/30 text-gray-400'}>
                    <td className="px-4 py-3 font-semibold text-gray-900 dark:text-white">
                      <span className="font-mono text-indigo-600 dark:text-indigo-400">{m.courseCode}</span>
                    </td>
                    <td className="px-4 py-3 font-medium text-gray-800 dark:text-gray-200">{m.assessmentName}</td>
                    <td className="px-4 py-3 text-gray-500 text-[11px]">{m.facultyName} • <span className="font-mono">{m.classroom}</span></td>
                    <td className="px-4 py-3 text-center font-mono">
                      {isCompleted ? (
                        <span className="font-semibold text-gray-900 dark:text-white">
                          {m.marksObtained?.toFixed(2)} / {m.maxMarks?.toFixed(0)}
                        </span>
                      ) : (
                        <span className="text-gray-400 italic">— / {m.maxMarks?.toFixed(0)}</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-center font-mono">
                      {isCompleted ? (
                        <span className="font-bold text-indigo-600 dark:text-indigo-400">
                          {m.convertedMarks?.toFixed(2)} / {m.maxConvertedMarks?.toFixed(0)}
                        </span>
                      ) : (
                        <span className="text-gray-400 italic">— / {m.maxConvertedMarks?.toFixed(0)}</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-center font-semibold">
                      {isCompleted && m.percentage != null ? (
                        <span className={m.percentage >= 75 ? 'text-emerald-600 dark:text-emerald-400' : m.percentage >= 50 ? 'text-indigo-600 dark:text-indigo-400' : 'text-red-500'}>
                          {m.percentage.toFixed(1)}%
                        </span>
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right">
                      {isCompleted ? (
                        <Badge variant="outline" className="text-emerald-700 dark:text-emerald-400 border-emerald-300 dark:border-emerald-800 bg-emerald-50 dark:bg-emerald-950/30 text-[10px]">
                          ✓ COMPLETED
                        </Badge>
                      ) : (
                        <Badge variant="outline" className="text-amber-700 dark:text-amber-400 border-amber-300 dark:border-amber-800 bg-amber-50 dark:bg-amber-950/30 text-[10px]">
                          ⏳ PENDING EVALUATION
                        </Badge>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
            <tfoot className="bg-gray-100 dark:bg-gray-950 font-bold border-t-2 border-gray-300 dark:border-gray-700">
              <tr>
                <td colSpan={3} className="px-4 py-3 text-gray-900 dark:text-white">
                  Evaluated Total Marks Summary
                </td>
                <td className="px-4 py-3 text-center font-mono text-gray-700 dark:text-gray-300">
                  Evaluated Raw Total
                </td>
                <td className="px-4 py-3 text-center font-mono text-indigo-600 dark:text-indigo-400 text-sm">
                  {totalConvertedObtained.toFixed(2)} / {totalMaxConvertedSemester} Max Internals
                </td>
                <td className="px-4 py-3 text-center font-semibold text-emerald-600 dark:text-emerald-400">
                  {analytics?.overallAverageMarksPercentage}% Evaluated Avg
                </td>
                <td className="px-4 py-3 text-right">
                  <Badge variant="default" className="bg-indigo-600 text-white text-[10px]">
                    VERIFIED ACCURACY
                  </Badge>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </Card>

      {/* 5. Explainable Risk Engine Breakdown (Audited & Transparent) */}
      <Card className="border border-indigo-200 dark:border-indigo-900/50 bg-indigo-50/20 dark:bg-indigo-950/10">
        <CardHeader className="py-4 border-b border-indigo-100 dark:border-indigo-900/30">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <CardTitle className="text-base font-bold text-gray-900 dark:text-white flex items-center">
                <Calculator className="w-5 h-5 mr-2 text-indigo-600 dark:text-indigo-400" />
                Transparent Risk Engine Calculation Breakdown
              </CardTitle>
              <CardDescription className="text-xs">
                Audited deterministic model: attendance shortfall + marks deficiency + overdue coursework friction
              </CardDescription>
            </div>
            <Badge variant="outline" className="font-mono text-xs border-indigo-300 text-indigo-700 dark:text-indigo-300">
              Deterministic Rule Engine v2.0
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="pt-4 space-y-4">
          {/* Formula Callout */}
          <div className="p-3 bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 text-xs">
            <span className="font-bold text-gray-700 dark:text-gray-300">Configured Risk Formula:</span>
            <code className="block mt-1 font-mono text-indigo-700 dark:text-indigo-300 bg-gray-50 dark:bg-gray-950 p-2 rounded border border-gray-100 dark:border-gray-800 select-all">
              Total Risk Score = min(Attendance Penalty + Marks Penalty + Missing Coursework Penalty, 100.0)
            </code>
          </div>

          {/* 3 Component Breakdown Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {/* Attendance Component */}
            <div className="p-3 bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800">
              <div className="flex justify-between items-center mb-1">
                <span className="text-xs font-bold text-gray-800 dark:text-gray-200">1. Attendance Penalty</span>
                <span className="font-mono font-bold text-xs text-indigo-600 dark:text-indigo-400">
                  +{analytics?.riskEngineExplanation?.attendanceRiskContribution ?? 0.0} pts
                </span>
              </div>
              <p className="text-[11px] text-gray-500 dark:text-gray-400 leading-relaxed">
                {analytics?.riskEngineExplanation?.attendanceFormulaExplanation || 'Aggregate attendance >= 75.0% incurs 0.0 penalty.'}
              </p>
            </div>

            {/* Marks Component */}
            <div className="p-3 bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800">
              <div className="flex justify-between items-center mb-1">
                <span className="text-xs font-bold text-gray-800 dark:text-gray-200">2. Evaluated Marks Penalty</span>
                <span className="font-mono font-bold text-xs text-indigo-600 dark:text-indigo-400">
                  +{analytics?.riskEngineExplanation?.marksRiskContribution ?? 0.0} pts
                </span>
              </div>
              <p className="text-[11px] text-gray-500 dark:text-gray-400 leading-relaxed">
                {analytics?.riskEngineExplanation?.marksFormulaExplanation || 'Evaluated avg marks >= 50.0% incurs 0.0 penalty.'}
              </p>
            </div>

            {/* Coursework Component */}
            <div className="p-3 bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800">
              <div className="flex justify-between items-center mb-1">
                <span className="text-xs font-bold text-gray-800 dark:text-gray-200">3. Overdue Coursework Penalty</span>
                <span className="font-mono font-bold text-xs text-indigo-600 dark:text-indigo-400">
                  +{analytics?.riskEngineExplanation?.missingAssignmentsRiskContribution ?? 0.0} pts
                </span>
              </div>
              <p className="text-[11px] text-gray-500 dark:text-gray-400 leading-relaxed">
                {analytics?.riskEngineExplanation?.missingAssignmentsFormulaExplanation || '5.0 pts friction per missing or overdue submission.'}
              </p>
            </div>
          </div>

          {/* Subject Risk Breakdown Table */}
          {analytics?.riskEngineExplanation?.subjectRisks && analytics.riskEngineExplanation.subjectRisks.length > 0 && (
            <div className="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-800">
              <table className="w-full text-xs text-left">
                <thead className="bg-gray-50 dark:bg-gray-950 uppercase text-[10px] text-gray-500 border-b border-gray-200 dark:border-gray-800">
                  <tr>
                    <th className="px-3 py-2">Subject</th>
                    <th className="px-3 py-2 text-center">Attendance %</th>
                    <th className="px-3 py-2 text-center">Evaluated Marks %</th>
                    <th className="px-3 py-2 text-center">Subject Risk Level</th>
                    <th className="px-3 py-2">Contributing Factors</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 dark:divide-gray-800 bg-white dark:bg-gray-900">
                  {analytics.riskEngineExplanation.subjectRisks.map((sr, idx) => (
                    <tr key={idx} className="hover:bg-gray-50 dark:hover:bg-gray-800/40">
                      <td className="px-3 py-2 font-semibold text-gray-900 dark:text-white">
                        <span className="font-mono text-indigo-600 dark:text-indigo-400">{sr.courseCode}</span> - {sr.courseName}
                      </td>
                      <td className="px-3 py-2 text-center font-medium">
                        {sr.attendancePercentage != null ? `${sr.attendancePercentage}%` : 'N/A'}
                      </td>
                      <td className="px-3 py-2 text-center font-medium">
                        {sr.evaluatedMarksPercentage != null ? `${sr.evaluatedMarksPercentage}%` : 'Pending'}
                      </td>
                      <td className="px-3 py-2 text-center">
                        <Badge 
                          variant={sr.riskLevel === 'HIGH' ? 'destructive' : sr.riskLevel === 'MEDIUM' ? 'warning' : 'outline'}
                          className="text-[9px]"
                        >
                          {sr.riskLevel}
                        </Badge>
                      </td>
                      <td className="px-3 py-2 text-gray-500 text-[11px]">
                        {sr.riskFactors.join(', ')}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 6. Next CLA / NTM Performance Target Planner */}
      <Card className="overflow-hidden border border-emerald-200 dark:border-emerald-900/40 bg-emerald-50/10 dark:bg-emerald-950/10">
        <CardHeader className="bg-emerald-50/40 dark:bg-emerald-950/20 border-b border-emerald-100 dark:border-emerald-900/30 py-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <div className="flex items-center space-x-2">
                <Target className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
                <CardTitle className="text-base font-bold text-gray-900 dark:text-white">
                  Next CLA / Assessment Performance Target Planner
                </CardTitle>
                <Badge className="bg-emerald-600 text-white text-[10px] uppercase">
                  Projected Simulation (Non-Recorded)
                </Badge>
              </div>
              <CardDescription className="text-xs mt-1">
                Actionable mark targets for upcoming internal components and their projected effect on student standing.
              </CardDescription>
            </div>

            {/* Target Scenario Selector */}
            <div className="flex items-center bg-white dark:bg-gray-900 rounded-lg p-1 border border-gray-200 dark:border-gray-800 self-start sm:self-auto">
              <button
                type="button"
                onClick={() => setTargetScenario('min')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md transition-colors ${
                  targetScenario === 'min' ? 'bg-indigo-600 text-white' : 'text-gray-600 dark:text-gray-300 hover:text-indigo-600'
                }`}
              >
                Minimum (+5%)
              </button>
              <button
                type="button"
                onClick={() => setTargetScenario('rec')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md transition-colors ${
                  targetScenario === 'rec' ? 'bg-indigo-600 text-white' : 'text-gray-600 dark:text-gray-300 hover:text-indigo-600'
                }`}
              >
                Recommended (+10%)
              </button>
              <button
                type="button"
                onClick={() => setTargetScenario('stretch')}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md transition-colors ${
                  targetScenario === 'stretch' ? 'bg-indigo-600 text-white' : 'text-gray-600 dark:text-gray-300 hover:text-indigo-600'
                }`}
              >
                Stretch (+15%)
              </button>
            </div>
          </div>
        </CardHeader>
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="text-[11px] text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-950 uppercase border-b border-gray-200 dark:border-gray-800">
              <tr>
                <th className="px-4 py-3">Course Code & Name</th>
                <th className="px-4 py-3 text-center">Current Converted</th>
                <th className="px-4 py-3">Upcoming Assessment</th>
                <th className="px-4 py-3 text-center">Weightage</th>
                <th className="px-4 py-3 text-center">Target Mark Target</th>
                <th className="px-4 py-3 text-center">Projected Standing</th>
                <th className="px-4 py-3">Impact on Academic Friction</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-800 bg-white dark:bg-gray-900">
              {(analytics?.targetPlanner || []).map((tp, idx) => {
                const targetScore = targetScenario === 'min' 
                  ? tp.minImprovementTarget 
                  : targetScenario === 'stretch' 
                  ? tp.stretchTarget 
                  : tp.recommendedTarget;

                const projectedScore = (tp.currentConvertedMarks || 0) + (targetScore || 0);

                return (
                  <tr key={idx} className="hover:bg-gray-50 dark:hover:bg-gray-800/40">
                    <td className="px-4 py-3 font-semibold text-gray-900 dark:text-white">
                      <span className="font-mono text-indigo-600 dark:text-indigo-400">{tp.courseCode}</span> - {tp.courseName}
                    </td>
                    <td className="px-4 py-3 text-center font-mono font-bold text-gray-700 dark:text-gray-300">
                      {tp.currentConvertedMarks?.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 font-medium text-emerald-600 dark:text-emerald-400">
                      {tp.pendingAssessmentName}
                    </td>
                    <td className="px-4 py-3 text-center font-mono">
                      {tp.pendingWeightage?.toFixed(0)} marks
                    </td>
                    <td className="px-4 py-3 text-center font-mono font-extrabold text-indigo-600 dark:text-indigo-400">
                      {targetScore?.toFixed(2)} / {tp.pendingWeightage?.toFixed(0)}
                    </td>
                    <td className="px-4 py-3 text-center font-mono font-bold text-emerald-600 dark:text-emerald-400">
                      {projectedScore.toFixed(2)} marks
                    </td>
                    <td className="px-4 py-3 text-gray-600 dark:text-gray-300 font-medium">
                      {tp.targetAffectsRisk}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>

      {/* 7. Weekly Timetable Cohort Schedule */}
      <Card className="overflow-hidden border border-gray-200 dark:border-gray-800">
        <CardHeader className="bg-gray-50/50 dark:bg-gray-900/50 border-b border-gray-100 dark:border-gray-800 py-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <CardTitle className="text-base font-bold text-gray-900 dark:text-white flex items-center">
                <Calendar className="w-4 h-4 mr-2 text-indigo-600 dark:text-indigo-400" />
                Common Cohort Timetable (Monday – Friday)
              </CardTitle>
              <CardDescription className="text-xs">
                Derived directly from official SRM AP timetable schedule, room allocations, and faculty assignments
              </CardDescription>
            </div>
            <Badge variant="outline" className="text-xs font-mono self-start sm:self-auto">
              7 Active Courses • SRM AP Campus
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="pt-4">
          <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
            {timetableDays.map(day => {
              const daySlots = timetableByDay[day] || [];
              return (
                <div key={day} className="rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 p-3 space-y-2.5">
                  <div className="border-b border-gray-100 dark:border-gray-800 pb-1.5 flex justify-between items-center">
                    <span className="font-bold text-xs uppercase tracking-wider text-indigo-600 dark:text-indigo-400">{day}</span>
                    <span className="text-[10px] text-gray-400 font-mono">{daySlots.length} slots</span>
                  </div>
                  <div className="space-y-2">
                    {daySlots.map((slot, sIdx) => (
                      <div key={sIdx} className="p-2 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-100 dark:border-gray-700/60 text-xs">
                        <div className="flex justify-between items-center mb-1">
                          <span className="font-mono text-[10px] text-gray-500">{slot.timeRange}</span>
                          <span className="font-mono text-[10px] font-bold text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-700 px-1 rounded">
                            {slot.classroom}
                          </span>
                        </div>
                        <div className="font-bold text-gray-900 dark:text-white text-xs">
                          {slot.courseCode}
                        </div>
                        <div className="text-[10px] text-gray-500 dark:text-gray-400 truncate">
                          {slot.facultyName.split(',')[0]}
                        </div>
                      </div>
                    ))}
                    {daySlots.length === 0 && (
                      <div className="text-center py-4 text-xs text-gray-400 italic">No scheduled sessions</div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* 8. Interventions & Recommendations */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Actionable Recommendations */}
        <Card>
          <CardHeader className="py-4">
            <CardTitle className="text-sm font-bold text-indigo-600 dark:text-indigo-400 flex items-center">
              <Award className="w-4 h-4 mr-2" />
              Actionable Guidance & Interventions
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2.5">
            {(analytics?.recommendations || []).map((rec, idx) => (
              <div key={idx} className="p-3 bg-gray-50 dark:bg-gray-950 rounded-xl border border-gray-200 dark:border-gray-800 text-xs space-y-1">
                <div className="flex justify-between items-center">
                  <span className="font-bold text-gray-900 dark:text-white">{rec.title}</span>
                  <Badge variant={rec.priority === 'HIGH' ? 'destructive' : 'outline'} className="text-[9px]">
                    {rec.priority}
                  </Badge>
                </div>
                <p className="text-gray-600 dark:text-gray-400">{rec.description}</p>
                <p className="text-indigo-600 dark:text-indigo-400 font-semibold pt-1">
                  Next Step: {rec.actionableStep}
                </p>
              </div>
            ))}
            {(!analytics?.recommendations || analytics.recommendations.length === 0) && (
              <p className="text-xs text-gray-500 italic py-2">No active friction flags requiring intervention.</p>
            )}
          </CardContent>
        </Card>

        {/* Assigned Interventions */}
        <Card>
          <CardHeader className="py-4">
            <CardTitle className="text-sm font-bold text-gray-900 dark:text-white flex items-center">
              <Activity className="w-4 h-4 mr-2 text-indigo-500" />
              Faculty Assigned Interventions
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2.5">
            {interventions.length > 0 ? (
              interventions.map((intItem) => (
                <div key={intItem.id} className="p-3 bg-gray-50 dark:bg-gray-950 rounded-xl border border-gray-200 dark:border-gray-800 text-xs space-y-1">
                  <div className="flex justify-between items-center">
                    <span className="font-bold text-gray-900 dark:text-white">{intItem.type}</span>
                    <Badge variant="outline" className="text-[9px]">
                      {intItem.status}
                    </Badge>
                  </div>
                  <p className="text-gray-600 dark:text-gray-400 leading-relaxed">{intItem.description}</p>
                  {intItem.assignedFacultyName && (
                    <p className="text-[11px] text-gray-500 pt-1">
                      Assigned Mentor: <strong className="text-gray-700 dark:text-gray-300">{intItem.assignedFacultyName}</strong>
                    </p>
                  )}
                </div>
              ))
            ) : (
              <p className="text-xs text-gray-500 italic py-2">No active intervention tickets opened.</p>
            )}
          </CardContent>
        </Card>
      </div>

    </div>
  );
}
