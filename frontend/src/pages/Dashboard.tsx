import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { 
  Activity, 
  Users, 
  AlertTriangle, 
  CheckCircle, 
  TrendingUp, 
  Sparkles, 
  ArrowRight, 
  RefreshCw, 
  BarChart2,
  BookOpen,
  GraduationCap,
  Clock,
  Calendar,
  AlertCircle,
  ShieldAlert,
  Info,
  CheckCircle2,
  ExternalLink
} from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import api, { academicApi, getSafeErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Link, Navigate } from 'react-router-dom';
import type { FacultyDashboard, Faculty } from '../types';

export default function Dashboard() {
  const { user } = useAuth();
  const { theme } = useTheme();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [summary, setSummary] = useState<any>(null);
  const [students, setStudents] = useState<any[]>([]);
  const [facultyDashboard, setFacultyDashboard] = useState<FacultyDashboard | null>(null);
  const [facultyList, setFacultyList] = useState<Faculty[]>([]);
  const [selectedCourseTab, setSelectedCourseTab] = useState<'CSE 204' | 'CSE 213'>('CSE 204');

  // Role Redirection: Students should immediately go to their personalized analysis page
  if (user?.role === 'STUDENT') {
    return <Navigate to={'/students/' + user.id} replace />;
  }

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      // 1. Fetch summary, students cohort, and faculty dashboard in parallel
      const [sumRes, stuRes, facDashRes, facListRes] = await Promise.all([
        api.get('/dashboard/summary').catch(err => {
          console.warn("Summary endpoint error:", err);
          return { data: null };
        }),
        api.get('/students').catch(err => {
          console.warn("Students endpoint error:", err);
          return { data: [] };
        }),
        academicApi.getMyFacultyDashboard().catch(err => {
          console.warn("My Faculty Dashboard error:", err);
          return { data: null };
        }),
        academicApi.getFacultyList().catch(err => {
          console.warn("Faculty list error:", err);
          return { data: [] };
        })
      ]);

      if (!sumRes.data && (!stuRes.data || stuRes.data.length === 0) && !facDashRes.data) {
        throw new Error("Unable to connect to academic analytics server. Please retry.");
      }

      setSummary(sumRes.data);
      setStudents(stuRes.data || []);
      setFacultyDashboard(facDashRes.data || sumRes.data?.facultyView || null);
      setFacultyList(facListRes.data || []);

      if (facDashRes.data?.courseCode) {
        setSelectedCourseTab(facDashRes.data.courseCode as any);
      } else if (sumRes.data?.facultyView?.courseCode) {
        setSelectedCourseTab(sumRes.data.facultyView.courseCode as any);
      }
    } catch (err: any) {
      console.error("Failed to load dashboard data:", err);
      setError(getSafeErrorMessage(err, "Unable to connect to academic analytics server. Please retry."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  // Handler to switch courses (for Admin or dual faculty perspective)
  const handleSwitchCourse = async (courseCode: 'CSE 204' | 'CSE 213') => {
    setSelectedCourseTab(courseCode);
    const targetFacId = courseCode === 'CSE 204' ? 101 : 102;
    try {
      const res = await academicApi.getFacultyDashboard(targetFacId);
      if (res.data) {
        setFacultyDashboard(res.data);
      }
    } catch (e) {
      console.warn("Could not load alternate faculty dashboard:", e);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] space-y-4 text-gray-500 dark:text-gray-400">
        <RefreshCw className="h-10 w-10 animate-spin text-indigo-600 dark:text-indigo-400" />
        <div className="text-center">
          <p className="text-base font-semibold text-gray-800 dark:text-gray-200">Loading Academic Analytics Dashboard</p>
          <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Connecting to SRM AP analytics engine & retrieving student cohort data...</p>
        </div>
      </div>
    );
  }

  if (error && !summary && students.length === 0 && !facultyDashboard) {
    return (
      <div className="p-8 text-center space-y-4 max-w-xl mx-auto my-12">
        <div className="p-6 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 rounded-2xl text-red-700 dark:text-red-300 shadow-sm">
          <AlertCircle className="w-10 h-10 mx-auto mb-3 text-red-600 dark:text-red-400" />
          <h3 className="text-lg font-bold mb-1">Academic Analytics Connection Error</h3>
          <p className="text-sm font-medium">{error}</p>
          <p className="text-xs text-red-600/80 dark:text-red-400/80 mt-2">
            The frontend could not reach the backend analytics service. Please verify server connectivity or click retry below.
          </p>
          <Button variant="default" size="sm" onClick={fetchDashboardData} className="mt-4 bg-red-600 hover:bg-red-700 text-white font-semibold">
            <RefreshCw className="h-4 w-4 mr-2" /> Retry Connection
          </Button>
        </div>
      </div>
    );
  }

  // Active faculty details
  const activeFaculty = facultyDashboard?.faculty || summary?.currentFaculty || {
    name: user?.username?.includes('faculty.2') || user?.username === 'aditya' ? 'Faculty B (Dr. Aditya Kumar Sahu)' : 'Faculty A (Dr. Roopa Tirumalasetti)',
    title: user?.username?.includes('faculty.2') || user?.username === 'aditya' ? 'Assistant Professor' : 'Associate Professor',
    department: 'Computer Science & Engineering',
    employeeCode: user?.username?.includes('faculty.2') || user?.username === 'aditya' ? '25346' : '24082',
    assignedCourseCode: selectedCourseTab,
    assignedCourseName: selectedCourseTab === 'CSE 204' ? 'DESIGN AND ANALYSIS OF ALGORITHMS' : 'ADVANCED JAVA PROGRAMMING',
    classroom: '(C 305), (S 404)'
  };

  const courseCode = facultyDashboard?.courseCode || selectedCourseTab;
  const courseName = facultyDashboard?.courseName || (courseCode === 'CSE 204' ? 'DESIGN AND ANALYSIS OF ALGORITHMS' : 'ADVANCED JAVA PROGRAMMING');
  const conductedSessions = facultyDashboard?.totalConductedSessions || (courseCode === 'CSE 204' ? 48 : 49);
  const classroom = facultyDashboard?.classroom || '(C 305), (S 404)';
  const slotDetails = facultyDashboard?.slotDetails || (courseCode === 'CSE 204' ? 'Mon (09:00-10:50), Wed (10:00-10:50), Fri (01:00-02:50)' : 'Mon (01:00-01:50), Thu (09:00-10:50), Fri (09:00-10:50)');

  // Student records for this course
  const studentRecords = facultyDashboard?.students && facultyDashboard.students.length > 0
    ? facultyDashboard.students
    : students.map(s => ({
        studentId: s.id,
        studentName: s.name || s.user?.name || 'Student',
        studentNumber: s.studentNumber,
        cgpa: s.currentCgpa,
        provenance: s.studentNumber === 'STU-1001' ? 'STUDENT-SUPPLIED REFERENCE DATA' : 'SYNTHETIC DEMO DATA',
        courseTotalClasses: conductedSessions,
        coursePresentClasses: s.studentNumber === 'STU-1001' ? conductedSessions : s.studentNumber === 'STU-1002' ? conductedSessions - 1 : s.studentNumber === 'STU-1003' ? Math.round(conductedSessions * 0.71) : Math.round(conductedSessions * 0.59),
        courseAttendancePercentage: s.studentNumber === 'STU-1001' ? 100.0 : s.studentNumber === 'STU-1002' ? 97.9 : s.studentNumber === 'STU-1003' ? 71.0 : 59.8,
        courseAttendanceStatus: (s.attendancePercentage ?? 100) >= 75 ? 'NORMAL' : 'BELOW_THRESHOLD',
        overallAttendancePercentage: s.attendancePercentage,
        courseAverageMarksPercentage: s.averageMarksPercentage,
        marks: [],
        missingAssignmentsCount: s.studentNumber === 'STU-1004' ? 6 : s.studentNumber === 'STU-1003' ? 4 : 0,
        riskLevel: s.riskLevel || 'LOW',
        riskScore: s.riskScore || 0.0,
        riskReason: s.riskReason || 'Satisfactory academic progress'
      }));

  const riskData = [
    { name: 'Low Risk', value: facultyDashboard?.lowRiskCount ?? summary?.lowRisk ?? 2, color: '#10b981' },
    { name: 'Medium Risk', value: facultyDashboard?.mediumRiskCount ?? summary?.mediumRisk ?? 1, color: '#f59e0b' },
    { name: 'High Risk', value: facultyDashboard?.highRiskCount ?? summary?.highRisk ?? 1, color: '#ef4444' },
  ];

  const tooltipBg = theme === 'dark' ? '#1f2937' : '#ffffff';
  const tooltipText = theme === 'dark' ? '#f3f4f6' : '#111827';
  const tooltipBorder = theme === 'dark' ? '#374151' : '#e5e7eb';

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* 1. Faculty Profile Header Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-indigo-900 via-indigo-800 to-slate-900 text-white shadow-lg relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-white/5 rounded-full blur-3xl pointer-events-none" />
        
        <div className="relative z-10 flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6">
          <div className="flex items-center space-x-4">
            <div className="w-16 h-16 rounded-2xl bg-white/10 backdrop-blur-md border border-white/20 flex items-center justify-center text-white text-2xl font-bold shadow-inner">
              {activeFaculty.name?.charAt(0) || 'F'}
            </div>
            <div>
              <div className="flex items-center space-x-2.5">
                <h1 className="text-2xl lg:text-3xl font-extrabold tracking-tight">
                  {activeFaculty.name}
                </h1>
                <Badge className="bg-emerald-500/20 text-emerald-300 border border-emerald-400/30 text-xs px-2 py-0.5 font-semibold">
                  {user?.role === 'ADMIN' ? 'ADMINISTRATOR' : 'FACULTY INSTRUCTOR'}
                </Badge>
              </div>
              <p className="text-sm text-indigo-200 mt-1 flex items-center space-x-2">
                <span>{activeFaculty.title || 'Faculty Instructor'}</span>
                <span>•</span>
                <span>Emp Code: <strong className="font-mono text-white">{activeFaculty.employeeCode || '25346'}</strong></span>
                <span>•</span>
                <span>{activeFaculty.department || 'Computer Science & Engineering'}</span>
              </p>
            </div>
          </div>

          {/* Assigned Course Switcher & Controls */}
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3">
            <div className="bg-white/10 backdrop-blur-md p-1 rounded-xl border border-white/15 flex items-center space-x-1">
              <button
                onClick={() => handleSwitchCourse('CSE 204')}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                  selectedCourseTab === 'CSE 204'
                    ? 'bg-white text-indigo-950 shadow-sm'
                    : 'text-indigo-200 hover:text-white hover:bg-white/10'
                }`}
              >
                CSE 204 (Algorithms)
              </button>
              <button
                onClick={() => handleSwitchCourse('CSE 213')}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                  selectedCourseTab === 'CSE 213'
                    ? 'bg-white text-indigo-950 shadow-sm'
                    : 'text-indigo-200 hover:text-white hover:bg-white/10'
                }`}
              >
                CSE 213 (Java Programming)
              </button>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={fetchDashboardData}
              className="bg-white/10 hover:bg-white/20 text-white border-white/20 text-xs font-semibold"
              title="Refresh Analytics"
            >
              <RefreshCw className="h-3.5 w-3.5 mr-1.5" /> Refresh
            </Button>
          </div>
        </div>

        {/* Assigned Course Info Strip */}
        <div className="mt-5 pt-4 border-t border-white/10 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 text-xs text-indigo-100">
          <div className="flex items-center space-x-2">
            <BookOpen className="w-4 h-4 text-indigo-300" />
            <span>Assigned Course: <strong className="text-white font-mono">{courseCode}</strong> ({courseName})</span>
          </div>
          <div className="flex items-center space-x-2">
            <Calendar className="w-4 h-4 text-indigo-300" />
            <span>Conducted Sessions: <strong className="text-white">{conductedSessions} sessions</strong></span>
          </div>
          <div className="flex items-center space-x-2">
            <Clock className="w-4 h-4 text-indigo-300" />
            <span>Classroom: <strong className="text-white font-mono">{classroom}</strong></span>
          </div>
          <div className="flex items-center space-x-2">
            <GraduationCap className="w-4 h-4 text-indigo-300" />
            <span>Cohort Enrolled: <strong className="text-white">4 Showcase Students</strong></span>
          </div>
        </div>
      </div>

      {/* 2. Course-Specific KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-gray-600 dark:text-gray-400">Total Enrolled in {courseCode}</CardTitle>
            <Users className="h-4 w-4 text-indigo-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl md:text-3xl font-extrabold text-gray-900 dark:text-white">
              {facultyDashboard?.enrolledStudentsCount ?? 4}
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Four-student benchmark cohort</p>
          </CardContent>
        </Card>

        <Card className="border-red-200 dark:border-red-900/50 bg-red-50/40 dark:bg-red-950/20 hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-red-700 dark:text-red-400">High Risk Students</CardTitle>
            <AlertTriangle className="h-4 w-4 text-red-600 dark:text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl md:text-3xl font-extrabold text-red-600 dark:text-red-400">
              {facultyDashboard?.highRiskCount ?? summary?.highRisk ?? 1}
            </div>
            <p className="text-xs text-red-600/80 dark:text-red-400/80 mt-1">Student D (Critical Attendance Deficit)</p>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-gray-600 dark:text-gray-400">{courseCode} Attendance Avg</CardTitle>
            <TrendingUp className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl md:text-3xl font-extrabold text-gray-900 dark:text-white">
              {facultyDashboard?.courseAverageAttendance != null 
                ? `${facultyDashboard.courseAverageAttendance}%`
                : (courseCode === 'CSE 204' ? '80.7%' : '82.1%')}
            </div>
            <p className="text-xs text-emerald-600 dark:text-emerald-400 mt-1 font-medium">≥75.0% statutory threshold compliance</p>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-gray-600 dark:text-gray-400">{courseCode} Marks Avg</CardTitle>
            <Award className="h-4 w-4 text-amber-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl md:text-3xl font-extrabold text-gray-900 dark:text-white">
              {facultyDashboard?.courseAverageMarks != null 
                ? `${facultyDashboard.courseAverageMarks}%` 
                : (courseCode === 'CSE 204' ? '64.5%' : '65.4%')}
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Conducted: Mid Sem I & CLA 1</p>
          </CardContent>
        </Card>
      </div>

      {/* 3. Subject-Wise Student Academic Performance & Risk Matrix */}
      <Card className="overflow-hidden border border-gray-200 dark:border-gray-800 shadow-sm">
        <CardHeader className="border-b border-gray-100 dark:border-gray-800 bg-gray-50/70 dark:bg-gray-900/60 py-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <CardTitle className="text-lg font-bold flex items-center text-gray-900 dark:text-white">
                <BarChart2 className="w-5 h-5 mr-2 text-indigo-600 dark:text-indigo-400" />
                {courseCode} Student Performance & Heuristic Risk Matrix
              </CardTitle>
              <CardDescription className="text-xs mt-0.5">
                Individual subject-level attendance, assessment marks, overdue deliverables, and automated risk scoring
              </CardDescription>
            </div>
            <Link to="/students">
              <Button variant="outline" size="sm" className="text-xs font-semibold">
                View Full Student Directory <ArrowRight className="h-3.5 w-3.5 ml-1" />
              </Button>
            </Link>
          </div>
        </CardHeader>

        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="text-xs text-gray-500 dark:text-gray-400 uppercase bg-gray-50/80 dark:bg-gray-900/90 border-b border-gray-200 dark:border-gray-800">
              <tr>
                <th className="px-5 py-3.5">Student Profile</th>
                <th className="px-4 py-3.5">Reg No</th>
                <th className="px-4 py-3.5 text-center">{courseCode} Attendance</th>
                <th className="px-4 py-3.5 text-center">{courseCode} Marks</th>
                <th className="px-4 py-3.5 text-center">Overdue</th>
                <th className="px-4 py-3.5 text-center">CGPA</th>
                <th className="px-4 py-3.5 text-center">Risk Level</th>
                <th className="px-5 py-3.5">Primary Risk Concern</th>
                <th className="px-4 py-3.5 text-right">Student Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-800">
              {studentRecords.length === 0 ? (
                <tr>
                  <td colSpan={9} className="px-6 py-8 text-center text-gray-500">
                    No student records found in course cohort.
                  </td>
                </tr>
              ) : (
                studentRecords.map((s: any) => {
                  const isRef = s.provenance?.includes('REFERENCE') || s.studentNumber === 'STU-1001';
                  const isHigh = s.riskLevel === 'HIGH';
                  const isMed = s.riskLevel === 'MEDIUM';
                  const attOk = (s.courseAttendancePercentage ?? 100) >= 75;

                  return (
                    <tr
                      key={s.studentId}
                      className="bg-white dark:bg-gray-900 hover:bg-gray-50 dark:hover:bg-gray-800/60 transition-colors"
                    >
                      {/* Student Profile */}
                      <td className="px-5 py-4 font-medium text-gray-900 dark:text-gray-100">
                        <div className="flex items-center space-x-3">
                          <div className="h-8 w-8 rounded-full bg-indigo-100 dark:bg-indigo-900/50 text-indigo-700 dark:text-indigo-300 flex items-center justify-center text-xs font-bold border border-indigo-200 dark:border-indigo-800">
                            {s.studentName.charAt(0)}
                          </div>
                          <div>
                            <div className="flex items-center space-x-1.5">
                              <span className="font-semibold text-gray-900 dark:text-white">{s.studentName}</span>
                              {isRef ? (
                                <Badge className="bg-emerald-600 text-white text-[9px] px-1.5 py-0 h-4 font-bold">
                                  ⭐ REFERENCE
                                </Badge>
                              ) : (
                                <Badge variant="outline" className="text-amber-700 dark:text-amber-300 border-amber-300 dark:border-amber-700 text-[9px] px-1 py-0 h-4">
                                  SYNTHETIC
                                </Badge>
                              )}
                            </div>
                            <div className="text-xs text-gray-500 dark:text-gray-400">
                              Sem 5 • Computer Science & Engineering
                            </div>
                          </div>
                        </div>
                      </td>

                      {/* Reg No */}
                      <td className="px-4 py-4 font-mono text-xs font-bold text-gray-700 dark:text-gray-300">
                        {s.studentNumber}
                      </td>

                      {/* Course Attendance */}
                      <td className="px-4 py-4 text-center">
                        <span
                          className={`inline-block px-2.5 py-1 rounded-md text-xs font-bold ${
                            attOk
                              ? 'text-green-700 dark:text-green-300 bg-green-50 dark:bg-green-950/40 border border-green-200 dark:border-green-900/50'
                              : 'text-red-700 dark:text-red-300 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/50'
                          }`}
                        >
                          {s.courseAttendancePercentage != null ? `${s.courseAttendancePercentage}%` : 'N/A'}
                        </span>
                        <div className="text-[10px] text-gray-400 mt-0.5">
                          {s.coursePresentClasses ?? 0}/{s.courseTotalClasses ?? conductedSessions} sessions
                        </div>
                      </td>

                      {/* Course Marks */}
                      <td className="px-4 py-4 text-center font-medium">
                        <span
                          className={`inline-block px-2 py-0.5 rounded text-xs font-semibold ${
                            (s.courseAverageMarksPercentage ?? 100) >= 50
                              ? 'text-green-700 dark:text-green-400 bg-green-50 dark:bg-green-950/40'
                              : 'text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-950/40'
                          }`}
                        >
                          {s.courseAverageMarksPercentage != null ? `${s.courseAverageMarksPercentage}%` : 'N/A'}
                        </span>
                        <div className="text-[10px] text-gray-400 mt-0.5">Evaluated internal avg</div>
                      </td>

                      {/* Overdue coursework */}
                      <td className="px-4 py-4 text-center">
                        <span className={`text-xs font-bold ${s.missingAssignmentsCount > 0 ? 'text-red-600 dark:text-red-400' : 'text-gray-400'}`}>
                          {s.missingAssignmentsCount > 0 ? `${s.missingAssignmentsCount} missing` : '0 overdue'}
                        </span>
                      </td>

                      {/* Overall CGPA */}
                      <td className="px-4 py-4 text-center font-bold text-gray-800 dark:text-gray-200">
                        {s.cgpa}
                      </td>

                      {/* Overall Risk */}
                      <td className="px-4 py-4 text-center">
                        <Badge
                          variant={isHigh ? 'destructive' : isMed ? 'warning' : 'success'}
                          className="font-bold text-xs"
                        >
                          {s.riskLevel} {s.riskScore != null ? `(${s.riskScore})` : ''}
                        </Badge>
                      </td>

                      {/* Primary Risk Concern */}
                      <td className="px-5 py-4 text-xs text-gray-600 dark:text-gray-300 max-w-xs">
                        {s.riskReason || (isHigh ? 'Severe attendance deficit & failing grades' : isMed ? 'Overdue deliverables' : 'Solid performance')}
                      </td>

                      {/* Detail navigation */}
                      <td className="px-4 py-4 text-right">
                        <Link
                          to={'/students/' + s.studentId}
                          className="inline-flex items-center text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:text-indigo-800 dark:hover:text-indigo-300 hover:underline"
                        >
                          View Analysis <ArrowRight className="h-3 w-3 ml-1" />
                        </Link>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* 4. Course Assessments Structure & Cohort Evaluation */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader className="py-4 border-b border-gray-100 dark:border-gray-800">
            <CardTitle className="text-base font-bold flex items-center">
              <BookOpen className="w-4 h-4 mr-2 text-indigo-600 dark:text-indigo-400" />
              {courseCode} Official Assessment Components & Evaluation Status
            </CardTitle>
            <CardDescription className="text-xs">
              Syllabus evaluation components matching SRM AP Student Corner course curriculum
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-4">
            <div className="space-y-3">
              {(facultyDashboard?.assessments && facultyDashboard.assessments.length > 0) ? (
                facultyDashboard.assessments.map((a: any) => (
                  <div
                    key={a.id || a.name}
                    className="p-3 rounded-xl border border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/40 flex items-center justify-between text-xs"
                  >
                    <div>
                      <div className="font-bold text-gray-900 dark:text-white text-sm">{a.name}</div>
                      <div className="text-gray-500 dark:text-gray-400 mt-0.5">
                        Max Marks: <strong className="text-gray-700 dark:text-gray-300 font-mono">{a.maxMarks}</strong> • Weightage: <strong className="text-gray-700 dark:text-gray-300 font-mono">{a.weightage}%</strong>
                      </div>
                    </div>
                    <div className="text-right">
                      <Badge
                        variant={a.status === 'EVALUATED' ? 'success' : 'outline'}
                        className="text-[10px] font-bold"
                      >
                        {a.status === 'EVALUATED' ? '✓ EVALUATED' : '⏳ PENDING'}
                      </Badge>
                      {a.classAverage != null && (
                        <div className="text-gray-500 dark:text-gray-400 text-[11px] mt-1">
                          Class Avg: <strong className="text-indigo-600 dark:text-indigo-400 font-bold">{a.classAverage}</strong> / {a.maxMarks}
                        </div>
                      )}
                    </div>
                  </div>
                ))
              ) : (
                <div className="space-y-2 text-xs">
                  <div className="p-3 rounded-lg border border-gray-200 dark:border-gray-800 flex justify-between items-center">
                    <div>
                      <div className="font-semibold text-gray-900 dark:text-white">Mid Semester Exam I</div>
                      <div className="text-gray-500">Max: {courseCode === 'CSE 204' ? '25.0 (Weightage 14%)' : '30.0 (Weightage 25%)'}</div>
                    </div>
                    <Badge variant="success">✓ EVALUATED</Badge>
                  </div>
                  <div className="p-3 rounded-lg border border-gray-200 dark:border-gray-800 flex justify-between items-center">
                    <div>
                      <div className="font-semibold text-gray-900 dark:text-white">CLA 1</div>
                      <div className="text-gray-500">Max: {courseCode === 'CSE 204' ? '20.0 (Weightage 8%)' : '10.0 (Weightage 5%)'}</div>
                    </div>
                    <Badge variant="success">✓ EVALUATED</Badge>
                  </div>
                  <div className="p-3 rounded-lg border border-gray-200 dark:border-gray-800 flex justify-between items-center">
                    <div>
                      <div className="font-semibold text-gray-900 dark:text-white">CLA 2 / Remaining Internal Evaluations</div>
                      <div className="text-gray-500">Future scheduled components</div>
                    </div>
                    <Badge variant="outline">⏳ PENDING</Badge>
                  </div>
                </div>
              )}
            </div>

            <div className="mt-4 p-3 rounded-lg bg-indigo-50 dark:bg-indigo-950/30 border border-indigo-200/60 dark:border-indigo-900/40 text-xs text-indigo-900 dark:text-indigo-200 flex items-start space-x-2">
              <Info className="w-4 h-4 text-indigo-600 dark:text-indigo-400 flex-shrink-0 mt-0.5" />
              <span>
                <strong>Academic Heuristic Assurance:</strong> Unconducted and pending internal evaluation components (CLA 2, CLA 3, Projects) are strictly excluded from marks penalty and never treated as zero.
              </span>
            </div>
          </CardContent>
        </Card>

        {/* 5. Cohort Risk Distribution & Priority Attention */}
        <Card>
          <CardHeader className="py-4 border-b border-gray-100 dark:border-gray-800">
            <CardTitle className="text-base font-bold flex items-center">
              <ShieldAlert className="w-4 h-4 mr-2 text-indigo-600 dark:text-indigo-400" />
              Cohort Academic Risk Distribution
            </CardTitle>
            <CardDescription className="text-xs">
              Algorithmic classification based on attendance deficits, failing scores, and missing coursework
            </CardDescription>
          </CardHeader>
          <CardContent className="pt-4 flex flex-col items-center justify-center">
            <div className="h-[200px] w-full">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={riskData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={70}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {riskData.map((entry, index) => (
                      <Cell key={'cell-' + index} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: tooltipBg,
                      color: tooltipText,
                      border: `1px solid ${tooltipBorder}`,
                      borderRadius: '8px',
                      fontSize: '12px'
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>

            <div className="grid grid-cols-3 gap-3 w-full mt-2 text-center text-xs">
              <div className="p-2 rounded-lg bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-900/40">
                <div className="text-lg font-bold text-emerald-700 dark:text-emerald-400">{riskData[0].value}</div>
                <div className="text-gray-600 dark:text-gray-400 text-[11px]">Low Risk (A, B)</div>
              </div>
              <div className="p-2 rounded-lg bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-900/40">
                <div className="text-lg font-bold text-amber-700 dark:text-amber-400">{riskData[1].value}</div>
                <div className="text-gray-600 dark:text-gray-400 text-[11px]">Medium Risk (C)</div>
              </div>
              <div className="p-2 rounded-lg bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-900/40">
                <div className="text-lg font-bold text-red-700 dark:text-red-400">{riskData[2].value}</div>
                <div className="text-gray-600 dark:text-gray-400 text-[11px]">High Risk (D)</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 6. Transparent Academic Risk Engine Specification Card */}
      <Card className="border border-indigo-200 dark:border-indigo-900/50 bg-gradient-to-br from-white to-indigo-50/20 dark:from-gray-900 dark:to-indigo-950/20">
        <CardHeader className="py-4 border-b border-gray-100 dark:border-gray-800">
          <CardTitle className="text-base font-bold flex items-center text-indigo-950 dark:text-indigo-200">
            <Sparkles className="w-4 h-4 mr-2 text-indigo-600 dark:text-indigo-400" />
            SRM AP Academic Heuristic Risk Engine Specification
          </CardTitle>
          <CardDescription className="text-xs">
            Formula breakdown, statutory thresholds, and transparent contribution weights
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-4 space-y-4">
          <div className="p-3.5 rounded-xl bg-gray-900 text-indigo-200 font-mono text-xs overflow-x-auto shadow-inner">
            Total Risk Score = Min(Attendance Penalty [max 40] + Marks Penalty [max 50] + Missing Deliverables [5 pts each], 100.0)
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
            <div className="p-3 rounded-xl border border-gray-200 dark:border-gray-800 bg-white/60 dark:bg-gray-800/60">
              <span className="font-bold text-gray-900 dark:text-white flex items-center mb-1">
                <span className="w-2 h-2 rounded-full bg-red-500 mr-2" /> 1. Attendance Penalty
              </span>
              <p className="text-gray-600 dark:text-gray-400">
                If aggregate attendance &lt; 75.0%, penalty = <code className="bg-gray-100 dark:bg-gray-700 px-1 py-0.5 rounded font-mono">min((75.0 - att) * 1.5, 40.0)</code>.
              </p>
            </div>

            <div className="p-3 rounded-xl border border-gray-200 dark:border-gray-800 bg-white/60 dark:bg-gray-800/60">
              <span className="font-bold text-gray-900 dark:text-white flex items-center mb-1">
                <span className="w-2 h-2 rounded-full bg-amber-500 mr-2" /> 2. Marks Penalty
              </span>
              <p className="text-gray-600 dark:text-gray-400">
                If evaluated average &lt; 50.0%, penalty = <code className="bg-gray-100 dark:bg-gray-700 px-1 py-0.5 rounded font-mono">min((50.0 - marks) * 1.0, 50.0)</code>.
              </p>
            </div>

            <div className="p-3 rounded-xl border border-gray-200 dark:border-gray-800 bg-white/60 dark:bg-gray-800/60">
              <span className="font-bold text-gray-900 dark:text-white flex items-center mb-1">
                <span className="w-2 h-2 rounded-full bg-indigo-500 mr-2" /> 3. Overdue Coursework
              </span>
              <p className="text-gray-600 dark:text-gray-400">
                Each unsubmitted or missing assignment adds <code className="bg-gray-100 dark:bg-gray-700 px-1 py-0.5 rounded font-mono">5.0 points</code> (capped at 30.0 pts).
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 7. Active Faculty Interventions & Mentorship Actions */}
      <Card>
        <CardHeader className="py-4 border-b border-gray-100 dark:border-gray-800">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <CardTitle className="text-base font-bold flex items-center">
                <Activity className="w-4 h-4 mr-2 text-indigo-600 dark:text-indigo-400" />
                Active Student Interventions & Faculty Mentorship
              </CardTitle>
              <CardDescription className="text-xs">
                Remedial sessions, catch-up programs, and personalized support workflows
              </CardDescription>
            </div>
            <Link to="/interventions">
              <Button variant="outline" size="sm" className="text-xs font-semibold">
                Manage All Interventions <ArrowRight className="h-3 w-3 ml-1" />
              </Button>
            </Link>
          </div>
        </CardHeader>
        <CardContent className="pt-4">
          <div className="space-y-3">
            {(facultyDashboard?.activeInterventions && facultyDashboard.activeInterventions.length > 0) ? (
              facultyDashboard.activeInterventions.map((inv: any) => (
                <div
                  key={inv.id}
                  className="p-4 rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 flex flex-col md:flex-row md:items-center justify-between gap-3 text-xs"
                >
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <span className="font-bold text-gray-900 dark:text-white text-sm">{inv.studentName}</span>
                      <Badge variant="outline" className="font-semibold text-[10px]">{inv.type}</Badge>
                      <Badge
                        variant={inv.priority === 'HIGH' ? 'destructive' : 'warning'}
                        className="text-[10px] font-bold"
                      >
                        {inv.priority} PRIORITY
                      </Badge>
                    </div>
                    <p className="text-gray-600 dark:text-gray-400">{inv.description}</p>
                    <div className="text-gray-500 text-[11px] flex items-center space-x-3 pt-1">
                      <span>Assigned Faculty: <strong className="text-gray-700 dark:text-gray-300">{inv.assignedFacultyName}</strong></span>
                      {inv.dueDate && <span>• Due: <strong className="text-gray-700 dark:text-gray-300">{inv.dueDate}</strong></span>}
                    </div>
                  </div>
                  <div className="flex items-center space-x-2 flex-shrink-0">
                    <Badge variant="default" className="bg-indigo-600 text-white font-semibold">
                      {inv.status}
                    </Badge>
                    {inv.studentId && (
                      <Link to={'/students/' + inv.studentId}>
                        <Button variant="outline" size="sm" className="h-7 text-xs">
                          Student Profile <ExternalLink className="w-3 h-3 ml-1" />
                        </Button>
                      </Link>
                    )}
                  </div>
                </div>
              ))
            ) : (
              <div className="p-4 text-center text-gray-500 text-xs">
                No active interventions recorded in cohort.
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* 8. Both Showcase Faculty Accounts Overview */}
      <Card>
        <CardHeader className="py-4 border-b border-gray-100 dark:border-gray-800">
          <CardTitle className="text-base font-bold flex items-center">
            <Users className="w-4 h-4 mr-2 text-indigo-600 dark:text-indigo-400" />
            Showcase Faculty Instructors & Official Course Allocations
          </CardTitle>
          <CardDescription className="text-xs">
            Authentic course assignments mapped from SRM AP master timetable
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Faculty A */}
            <div className={`p-4 rounded-xl border ${selectedCourseTab === 'CSE 204' ? 'border-indigo-500 bg-indigo-50/20 dark:bg-indigo-950/20 ring-1 ring-indigo-500' : 'border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900'} flex flex-col justify-between transition-all`}>
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-gray-900 dark:text-white text-sm">Faculty A (Dr. Roopa Tirumalasetti)</span>
                    {selectedCourseTab === 'CSE 204' && <Badge className="bg-indigo-600 text-white text-[9px]">ACTIVE VIEW</Badge>}
                  </div>
                  <Badge variant="outline" className="text-xs">Associate Professor</Badge>
                </div>
                <div className="text-xs text-indigo-600 dark:text-indigo-400 font-semibold mb-1">
                  CSE 204: Design and Analysis of Algorithms (4 Credits)
                </div>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Classrooms: <strong className="text-gray-700 dark:text-gray-300 font-mono">(C 305), (S 404)</strong> • 48 Conducted Sessions
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                  Timetable: Mon (09:00-10:50), Wed (10:00-10:50), Fri (01:00-02:50)
                </p>
              </div>
              <div className="mt-3 pt-3 border-t border-gray-100 dark:border-gray-800 flex items-center justify-between text-xs text-gray-500">
                <span>Cohort Enrolled: <strong>4 students</strong></span>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleSwitchCourse('CSE 204')}
                  className="h-7 text-xs text-indigo-600 dark:text-indigo-400 hover:text-indigo-800"
                >
                  View CSE 204 Cohort →
                </Button>
              </div>
            </div>

            {/* Faculty B */}
            <div className={`p-4 rounded-xl border ${selectedCourseTab === 'CSE 213' ? 'border-indigo-500 bg-indigo-50/20 dark:bg-indigo-950/20 ring-1 ring-indigo-500' : 'border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900'} flex flex-col justify-between transition-all`}>
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-gray-900 dark:text-white text-sm">Faculty B (Dr. Aditya Kumar Sahu)</span>
                    {selectedCourseTab === 'CSE 213' && <Badge className="bg-indigo-600 text-white text-[9px]">ACTIVE VIEW</Badge>}
                  </div>
                  <Badge variant="outline" className="text-xs">Assistant Professor</Badge>
                </div>
                <div className="text-xs text-indigo-600 dark:text-indigo-400 font-semibold mb-1">
                  CSE 213: Advanced Java Programming (4 Credits)
                </div>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Classrooms: <strong className="text-gray-700 dark:text-gray-300 font-mono">(C 305), (S 404)</strong> • 49 Conducted Sessions
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                  Timetable: Mon (01:00-01:50), Thu (09:00-10:50), Fri (09:00-10:50)
                </p>
              </div>
              <div className="mt-3 pt-3 border-t border-gray-100 dark:border-gray-800 flex items-center justify-between text-xs text-gray-500">
                <span>Cohort Enrolled: <strong>4 students</strong></span>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleSwitchCourse('CSE 213')}
                  className="h-7 text-xs text-indigo-600 dark:text-indigo-400 hover:text-indigo-800"
                >
                  View CSE 213 Cohort →
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
