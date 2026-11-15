import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import {
  Activity,
  Target,
  FileText,
  RefreshCw,
  Calendar,
  BookOpen,
  Calculator,
  Award,
  ArrowRight,
  UserCheck,
  ArrowLeft,
  Search,
  Lock,
  UserX
} from 'lucide-react';
import api, { getSafeErrorMessage } from '../services/api';
import { fmt, riskBadgeVariant } from '../components/common/StateViews';
import type { StudentAcademicAnalytics, TimetableSlot } from '../types';

export default function StudentProfile() {
  const { id } = useParams();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [student, setStudent] = useState<any>(null);
  const [riskAssessment, setRiskAssessment] = useState<any>(null);
  const [interventions, setInterventions] = useState<any[]>([]);
  const [analytics, setAnalytics] = useState<StudentAcademicAnalytics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isNotFound, setIsNotFound] = useState(false);
  const [isForbidden, setIsForbidden] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [targetScenario, setTargetScenario] = useState<'min' | 'rec' | 'stretch'>('rec');

  const fetchStudentData = async () => {
    try {
      setLoading(true);
      setError(null);
      setIsNotFound(false);
      setIsForbidden(false);

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
      const status = err.response?.status;
      if (status === 404) {
        setIsNotFound(true);
      } else if (status === 403) {
        setIsForbidden(true);
      } else {
        setError(getSafeErrorMessage(err, "Failed to load student academic analytics from server."));
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStudentData();
  }, [id]);

  const handleRecalculateRisk = async () => {
    setActionError(null);
    try {
      await api.post('/students/' + id + '/risk/recalculate');
      await fetchStudentData();
    } catch (err) {
      setActionError(getSafeErrorMessage(err, 'Risk recalculation failed. The previous official result is unchanged.'));
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] space-y-3 text-gray-500 dark:text-gray-400">
        <RefreshCw className="h-8 w-8 animate-spin text-indigo-600" />
        <p className="text-sm font-medium">Loading complete academic analytics & timetable...</p>
      </div>
    );
  }

  // 404 Not Found UI
  if (isNotFound) {
    return (
      <div className="p-6 md:p-12 max-w-xl mx-auto text-center space-y-6">
        <div className="w-16 h-16 bg-amber-100 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 rounded-3xl mx-auto flex items-center justify-center shadow-inner">
          <UserX className="w-8 h-8" />
        </div>
        <div className="space-y-2">
          <Badge variant="outline" className="border-amber-400 text-amber-700 dark:text-amber-300">
            HTTP 404 — Record Not Found
          </Badge>
          <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white">
            Student Profile Not Found
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 max-w-md mx-auto">
            The requested student ID (<span className="font-mono font-bold text-gray-800 dark:text-gray-200">ID: {id}</span>) does not exist in the EduPulse database.
          </p>
        </div>

        <div className="bg-gray-50 dark:bg-gray-900/50 border border-gray-200 dark:border-gray-800 rounded-2xl p-4 text-xs text-left space-y-2">
          <span className="font-bold text-gray-700 dark:text-gray-300">Available Demo Showcase Profiles:</span>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 font-mono">
            <Link to="/students/53" className="p-2 rounded bg-white dark:bg-gray-800 border hover:border-indigo-500 text-indigo-600 dark:text-indigo-400 flex items-center justify-between">
              <span>Student A (STU-1001)</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
            <Link to="/students/55" className="p-2 rounded bg-white dark:bg-gray-800 border hover:border-indigo-500 text-indigo-600 dark:text-indigo-400 flex items-center justify-between">
              <span>Student B (STU-1002)</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
            <Link to="/students/57" className="p-2 rounded bg-white dark:bg-gray-800 border hover:border-indigo-500 text-indigo-600 dark:text-indigo-400 flex items-center justify-between">
              <span>Student C (STU-1003)</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
            <Link to="/students/3" className="p-2 rounded bg-white dark:bg-gray-800 border hover:border-indigo-500 text-indigo-600 dark:text-indigo-400 flex items-center justify-between">
              <span>Student D (STU-1004)</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
        </div>

        <div className="flex items-center justify-center gap-3">
          <Link to="/dashboard">
            <Button variant="outline" size="sm">
              <ArrowLeft className="w-4 h-4 mr-1.5" /> Return to Dashboard
            </Button>
          </Link>
          {user?.role !== 'STUDENT' && (
            <Link to="/students">
              <Button size="sm">
                <Search className="w-4 h-4 mr-1.5" /> Student Directory
              </Button>
            </Link>
          )}
        </div>
      </div>
    );
  }

  // 403 Forbidden UI
  if (isForbidden) {
    return (
      <div className="p-6 md:p-12 max-w-xl mx-auto text-center space-y-6">
        <div className="w-16 h-16 bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400 rounded-3xl mx-auto flex items-center justify-center shadow-inner">
          <Lock className="w-8 h-8" />
        </div>
        <div className="space-y-2">
          <Badge variant="destructive">
            HTTP 403 — Access Restricted
          </Badge>
          <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white">
            Access to Cohort Record Restricted
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 max-w-md mx-auto">
            Student accounts are authorized only to inspect their own verified academic record.
          </p>
        </div>

        <div className="flex items-center justify-center gap-3">
          {user?.id && (
            <Link to={'/students/' + user.id}>
              <Button size="sm">
                <UserCheck className="w-4 h-4 mr-1.5" /> Open My Academic Profile
              </Button>
            </Link>
          )}
          <Link to="/dashboard">
            <Button variant="outline" size="sm">
              <ArrowLeft className="w-4 h-4 mr-1.5" /> Dashboard
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  // Error with retry UI
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

  const isActualStudent = analytics?.provenance?.includes("REFERENCE") || analytics?.provenance?.includes("ACTUAL") || student.name?.includes("Student A") || student.studentNumber === "STU-1001";
  const isHighRisk = riskAssessment?.riskLevel === 'HIGH' || analytics?.riskLevel === 'HIGH';
  const isMedRisk = riskAssessment?.riskLevel === 'MEDIUM' || analytics?.riskLevel === 'MEDIUM';
  const riskColor = isHighRisk ? 'text-red-600 dark:text-red-400' : isMedRisk ? 'text-amber-600 dark:text-amber-400' : 'text-green-600 dark:text-green-400';


  // Group timetable by Day safely
  const timetableDays = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'];
  const timetableByDay = (analytics?.timetable || []).reduce((acc: Record<string, TimetableSlot[]>, slot) => {
    if (slot && slot.day) {
      if (!acc[slot.day]) acc[slot.day] = [];
      acc[slot.day].push(slot);
    }
    return acc;
  }, {});

  // Calculate Converted Marks Totals safely
  const totalConvertedObtained = (analytics?.assessmentBreakdown || [])
    .filter(a => typeof a.convertedMarks === 'number')
    .reduce((sum, a) => sum + (a.convertedMarks || 0), 0);
  const totalMaxConvertedEvaluated = (analytics?.assessmentBreakdown || [])
    .filter(a => typeof a.convertedMarks === 'number')
    .reduce((sum, a) => sum + (a.maxConvertedMarks || 0), 0);
  const totalMaxConvertedSemester = (analytics?.assessmentBreakdown || [])
    .reduce((sum, a) => sum + (typeof a.maxConvertedMarks === 'number' ? a.maxConvertedMarks : 0), 0) || 50;

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
                    ⭐ STUDENT-SUPPLIED REFERENCE DATA
                  </Badge>
                ) : (
                  <Badge variant="outline" className="border-amber-400 text-amber-700 dark:text-amber-300 bg-amber-50/50 dark:bg-amber-950/30 text-xs font-semibold px-2.5 py-0.5">
                    🧪 SYNTHETIC DEMO DATA
                  </Badge>
                )}
              </div>

              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1 font-medium">
                <span className="font-mono text-gray-900 dark:text-gray-200 font-bold">{student.studentNumber}</span> • {student.department || 'Computer Science & Engineering'} • Semester {student.currentSemester || 3}
              </p>

              <div className="flex items-center mt-2.5 gap-2 flex-wrap text-xs">
                <Badge variant="outline" className="font-semibold bg-gray-50 dark:bg-gray-800">
                  CGPA: {student.currentCgpa ?? student.cgpa ?? 'N/A'}
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
              Academic Risk Score
            </span>
            <div className="flex items-center space-x-3">
              <span className={`text-4xl font-extrabold ${riskColor}`}>
                {fmt(analytics?.riskScore ?? riskAssessment?.riskScore)}
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
            {actionError && <p role="alert" className="mt-1 text-[11px] text-red-600 dark:text-red-400">{actionError}</p>}
          </div>
        </div>
      </div>

      {/* 2. Key Metrics Row (Attendance, Converted Marks, Assignments) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Attendance Card */}
        <Card className="hover:shadow-sm transition-shadow">
          <CardHeader className="pb-2">
            <CardTitle className="text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold">
              Overall Attendance
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white">
              {analytics?.overallAttendancePercentage != null ? `${analytics.overallAttendancePercentage}%` : 'N/A'}
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              <strong className="text-emerald-600 dark:text-emerald-400">{analytics?.presentClasses}</strong> present out of <strong>{analytics?.totalClasses}</strong> classes held
            </p>
            <div className="w-full bg-gray-200 dark:bg-gray-800 h-1.5 rounded-full mt-2 overflow-hidden">
              <div 
                className={`h-full ${ (analytics?.overallAttendancePercentage || 0) >= 75 ? 'bg-emerald-500' : 'bg-red-500'}`}
                style={{ width: `${Math.min(100, analytics?.overallAttendancePercentage || 0)}%` }}
              />
            </div>
          </CardContent>
        </Card>

        {/* Internal Marks Card */}
        <Card className="hover:shadow-sm transition-shadow">
          <CardHeader className="pb-2">
            <CardTitle className="text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold">
              Internal Marks (so far)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl sm:text-3xl font-extrabold text-indigo-600 dark:text-indigo-400">
              {totalConvertedObtained.toFixed(1)} <span className="text-sm font-normal text-gray-500">/ {totalMaxConvertedEvaluated.toFixed(0)}</span>
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {totalMaxConvertedEvaluated > 0
                ? <><strong className="text-indigo-600 dark:text-indigo-400">{((totalConvertedObtained / totalMaxConvertedEvaluated) * 100).toFixed(0)}%</strong> of what&apos;s been graded</>
                : 'Nothing graded yet'}
            </p>
            <p className="text-[11px] text-gray-500 dark:text-gray-400 mt-1">
              Full semester total: <strong>{totalMaxConvertedSemester}</strong> · {analytics?.evaluatedAssessmentsCount} components graded
            </p>
          </CardContent>
        </Card>

        {/* Graded Percentage Average */}
        <Card className="hover:shadow-sm transition-shadow">
          <CardHeader className="pb-2">
            <CardTitle className="text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold">
              Average Mark
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white">
              {analytics?.overallAverageMarksPercentage != null ? `${analytics.overallAverageMarksPercentage}%` : 'N/A'}
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Pass mark: <strong className="text-gray-700 dark:text-gray-300">50%</strong>
            </p>
            <p className={`text-[11px] font-semibold mt-1 ${ (analytics?.overallAverageMarksPercentage || 0) >= 50 ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-500'}`}>
              {analytics?.overallAverageMarksPercentage == null ? 'No marks yet' : analytics.overallAverageMarksPercentage >= 75 ? 'Doing great' : analytics.overallAverageMarksPercentage >= 50 ? 'On track' : 'Needs support'}
            </p>
          </CardContent>
        </Card>

        {/* Coursework Submissions */}
        <Card className="hover:shadow-sm transition-shadow">
          <CardHeader className="pb-2">
            <CardTitle className="text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold">
              Assignments
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white">
              {analytics?.submittedAssignments ?? 0} <span className="text-sm font-normal text-gray-500">/ {analytics?.totalAssignments ?? 0}</span>
            </div>
            <p className={`text-xs mt-1 font-semibold ${ (analytics?.missingAssignments || 0) > 0 ? 'text-red-600 dark:text-red-400' : 'text-emerald-600 dark:text-emerald-400'}`}>
              {(analytics?.missingAssignments || 0) > 0 ? `${analytics?.missingAssignments} still to submit` : 'All submitted on time'}
            </p>
            <p className="text-[11px] text-gray-500 dark:text-gray-400 mt-1">
              Each missing one adds 5 pts to your risk
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
                Attendance — by Subject ({analytics?.attendancePeriod || '03-Aug-2026 to 29-Oct-2026'})
              </CardTitle>
              <CardDescription className="text-xs">
                Classes held, present, absent and OD/ML for each subject. 75% is the minimum required.
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
                <th className="px-4 py-3">Subject Code &amp; Name</th>
                <th className="px-4 py-3">Faculty</th>
                <th className="px-4 py-3">Room</th>
                <th className="px-4 py-3 text-center">Held</th>
                <th className="px-4 py-3 text-center">Present</th>
                <th className="px-4 py-3 text-center">Absent</th>
                <th className="px-4 py-3 text-center">OD/ML</th>
                <th className="px-4 py-3 text-center">Attendance %</th>
                <th className="px-4 py-3 text-right">Status</th>
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
                        {isShort ? 'Below 75%' : 'OK'}
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
                Internal Marks — by Subject & Component
              </CardTitle>
              <CardDescription className="text-xs">
                Your marks in each graded component. Components not yet conducted show as pending — never counted as zero.
              </CardDescription>
            </div>
            <div className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 px-3 py-1 rounded-md border border-indigo-200 dark:border-indigo-800">
              Graded so far: {totalConvertedObtained.toFixed(1)} / {totalMaxConvertedEvaluated.toFixed(0)}
            </div>
          </div>
        </CardHeader>
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="text-[11px] text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-950 uppercase border-b border-gray-200 dark:border-gray-800">
              <tr>
                <th className="px-4 py-3">Course</th>
                <th className="px-4 py-3">Component</th>
                <th className="px-4 py-3">Faculty &amp; Room</th>
                <th className="px-4 py-3 text-center">Marks obtained</th>
                <th className="px-4 py-3 text-center">Counts toward total</th>
                <th className="px-4 py-3 text-center">%</th>
                <th className="px-4 py-3 text-right">Status</th>
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
                  Totals (graded components)
                </td>
                <td className="px-4 py-3 text-center font-mono text-gray-500 dark:text-gray-400 text-[11px]">
                  {analytics?.evaluatedAssessmentsCount} graded
                </td>
                <td className="px-4 py-3 text-center font-mono text-indigo-600 dark:text-indigo-400 text-sm">
                  {totalConvertedObtained.toFixed(1)} <span className="text-gray-500">/ {totalMaxConvertedSemester} this sem</span>
                </td>
                <td className="px-4 py-3 text-center font-semibold text-emerald-600 dark:text-emerald-400">
                  {analytics?.overallAverageMarksPercentage}% avg
                </td>
                <td className="px-4 py-3 text-right">
                  <Badge variant="default" className="bg-indigo-600 text-white text-[10px]">
                    GRADED SO FAR
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
                How your risk score is worked out
              </CardTitle>
              <CardDescription className="text-xs">
                Three things add up: low attendance, low marks, and assignments you haven&apos;t submitted.
              </CardDescription>
            </div>
            <Badge variant="outline" className="font-mono text-xs border-indigo-300 text-indigo-700 dark:text-indigo-300">
              Rule-based · v2.0
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="pt-4 space-y-4">
          {/* Formula Callout */}
          <div className="p-3 bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800 text-xs">
            <span className="font-bold text-gray-700 dark:text-gray-300">How it adds up:</span>
            <code className="block mt-1 font-mono text-indigo-700 dark:text-indigo-300 bg-gray-50 dark:bg-gray-950 p-2 rounded border border-gray-100 dark:border-gray-800 select-all">
              Risk = Low attendance + Low marks + Missing assignments (capped at 100)
            </code>
          </div>

          {/* 3 Component Breakdown Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {/* Attendance Component */}
            <div className="p-3 bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800">
              <div className="flex justify-between items-center mb-1">
                <span className="text-xs font-bold text-gray-800 dark:text-gray-200">1. Low attendance</span>
                <span className="font-mono font-bold text-xs text-indigo-600 dark:text-indigo-400">
                  +{fmt(analytics?.riskEngineExplanation?.attendanceComponent?.pointsContributed)} pts
                </span>
              </div>
              <p className="text-[11px] text-gray-500 dark:text-gray-400 leading-relaxed">
                {analytics?.riskEngineExplanation?.attendanceComponent?.explanation ?? 'Explanation unavailable.'}
              </p>
            </div>

            {/* Marks Component */}
            <div className="p-3 bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800">
              <div className="flex justify-between items-center mb-1">
                <span className="text-xs font-bold text-gray-800 dark:text-gray-200">2. Low marks</span>
                <span className="font-mono font-bold text-xs text-indigo-600 dark:text-indigo-400">
                  +{fmt(analytics?.riskEngineExplanation?.marksComponent?.pointsContributed)} pts
                </span>
              </div>
              <p className="text-[11px] text-gray-500 dark:text-gray-400 leading-relaxed">
                {analytics?.riskEngineExplanation?.marksComponent?.explanation ?? 'Explanation unavailable.'}
              </p>
            </div>

            {/* Coursework Component */}
            <div className="p-3 bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800">
              <div className="flex justify-between items-center mb-1">
                <span className="text-xs font-bold text-gray-800 dark:text-gray-200">3. Missing assignments</span>
                <span className="font-mono font-bold text-xs text-indigo-600 dark:text-indigo-400">
                  +{fmt(analytics?.riskEngineExplanation?.missingAssignmentsComponent?.pointsContributed)} pts
                </span>
              </div>
              <p className="text-[11px] text-gray-500 dark:text-gray-400 leading-relaxed">
                {analytics?.riskEngineExplanation?.missingAssignmentsComponent?.explanation ?? 'Explanation unavailable.'}
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
                    <th className="px-3 py-2 text-center">Marks %</th>
                    <th className="px-3 py-2 text-center">Risk</th>
                    <th className="px-3 py-2">Why</th>
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
                        {sr.marksPercentage != null ? `${sr.marksPercentage}%` : 'Pending'}
                      </td>
                      <td className="px-3 py-2 text-center">
                        <Badge 
                          variant={riskBadgeVariant(sr.riskStatus)}
                          className="text-[9px]"
                        >
                          {sr.riskStatus}
                        </Badge>
                      </td>
                      <td className="px-3 py-2 text-gray-500 text-[11px]">
                        {sr.details}
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
                const scenarioObj = targetScenario === 'min' 
                  ? (tp.minimumTarget || (typeof tp.minImprovementTarget === 'number' ? { targetScore: tp.minImprovementTarget } : undefined))
                  : targetScenario === 'stretch' 
                  ? (typeof tp.stretchTarget === 'object' && tp.stretchTarget !== null ? tp.stretchTarget : (typeof tp.stretchTarget === 'number' ? { targetScore: tp.stretchTarget } : undefined))
                  : (typeof tp.recommendedTarget === 'object' && tp.recommendedTarget !== null ? tp.recommendedTarget : (typeof tp.recommendedTarget === 'number' ? { targetScore: tp.recommendedTarget } : undefined));

                const targetScore = typeof scenarioObj?.targetScore === 'number' 
                  ? scenarioObj.targetScore 
                  : 0;

                const currentConv = typeof tp.currentConvertedMarks === 'number' 
                  ? tp.currentConvertedMarks 
                  : 0;

                const expectedTotal = typeof scenarioObj?.expectedConvertedTotal === 'number'
                  ? scenarioObj.expectedConvertedTotal
                  : (currentConv + targetScore);

                const pendingMax = typeof tp.pendingAssessmentMaxMarks === 'number'
                  ? tp.pendingAssessmentMaxMarks
                  : (typeof tp.pendingWeightage === 'number' ? tp.pendingWeightage : 15);

                const impactText = tp.riskImpactDescription || tp.targetAffectsRisk || scenarioObj?.rationale || 'Maintains academic buffer for end-semester exams.';

                return (
                  <tr key={idx} className="hover:bg-gray-50 dark:hover:bg-gray-800/40">
                    <td className="px-4 py-3 font-semibold text-gray-900 dark:text-white">
                      <span className="font-mono text-indigo-600 dark:text-indigo-400">{tp.courseCode}</span> - {tp.courseName}
                    </td>
                    <td className="px-4 py-3 text-center font-mono font-bold text-gray-700 dark:text-gray-300">
                      {currentConv.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 font-medium text-emerald-600 dark:text-emerald-400">
                      {tp.pendingAssessmentName}
                    </td>
                    <td className="px-4 py-3 text-center font-mono">
                      {pendingMax.toFixed(0)} marks
                    </td>
                    <td className="px-4 py-3 text-center font-mono font-extrabold text-indigo-600 dark:text-indigo-400">
                      {targetScore.toFixed(2)} / {pendingMax.toFixed(0)}
                    </td>
                    <td className="px-4 py-3 text-center font-mono font-bold text-emerald-600 dark:text-emerald-400">
                      {expectedTotal.toFixed(2)} marks
                    </td>
                    <td className="px-4 py-3 text-gray-600 dark:text-gray-300 font-medium">
                      {impactText}
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
