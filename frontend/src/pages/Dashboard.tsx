import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Activity, Users, AlertTriangle, CheckCircle, TrendingUp, Sparkles, ArrowRight, RefreshCw, BarChart2 } from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Link, Navigate } from 'react-router-dom';

export default function Dashboard() {
  const { user } = useAuth();
  const { theme } = useTheme();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [summary, setSummary] = useState<any>(null);
  const [students, setStudents] = useState<any[]>([]);

  if (user?.role === 'STUDENT') {
    return <Navigate to={'/students/' + user.id} replace />;
  }

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [sumRes, stuRes] = await Promise.all([
        api.get('/dashboard/summary'),
        api.get('/students').catch(() => ({ data: [] }))
      ]);
      setSummary(sumRes.data);
      setStudents(stuRes.data || []);
    } catch (err: any) {
      console.error("Failed to load dashboard data:", err);
      setError("Unable to connect to academic analytics server. Please retry.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] space-y-3 text-gray-500 dark:text-gray-400">
        <RefreshCw className="h-8 w-8 animate-spin text-indigo-600" />
        <p className="text-sm">Loading academic analytics dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 text-center space-y-4">
        <div className="p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/50 rounded-xl text-red-700 dark:text-red-400 max-w-lg mx-auto">
          <p className="font-medium">{error}</p>
          <Button variant="outline" size="sm" onClick={fetchDashboardData} className="mt-3">
            <RefreshCw className="h-4 w-4 mr-2" /> Retry Connection
          </Button>
        </div>
      </div>
    );
  }

  const riskData = [
    { name: 'Low Risk', value: summary?.lowRisk || 0, color: '#10b981' },
    { name: 'Medium Risk', value: summary?.mediumRisk || 0, color: '#f59e0b' },
    { name: 'High Risk', value: summary?.highRisk || 0, color: '#ef4444' },
  ];

  const tooltipBg = theme === 'dark' ? '#1f2937' : '#ffffff';
  const tooltipText = theme === 'dark' ? '#f3f4f6' : '#111827';
  const tooltipBorder = theme === 'dark' ? '#374151' : '#e5e7eb';

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold tracking-tight text-gray-900 dark:text-white">
            Dashboard Overview
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Real-time cohort performance, early risk flags, and intervention monitoring
          </p>
        </div>
        <div className="flex items-center space-x-2">
          {user?.role === 'ADMIN' && <Badge variant="outline">Admin View</Badge>}
          <Badge variant="default" className="bg-indigo-600">
            {(user as any)?.department || 'Computer Science & Engineering'}
          </Badge>
          <Button variant="outline" size="sm" onClick={fetchDashboardData} title="Refresh">
            <RefreshCw className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-gray-600 dark:text-gray-400">Total Enrolled</CardTitle>
            <Users className="h-4 w-4 text-indigo-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white">{summary?.totalStudents ?? students.length}</div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Active showcase student cohort</p>
          </CardContent>
        </Card>

        <Card className="border-red-200 dark:border-red-900/50 bg-red-50/50 dark:bg-red-950/20 hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-red-700 dark:text-red-400">High Risk Students</CardTitle>
            <AlertTriangle className="h-4 w-4 text-red-600 dark:text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl md:text-3xl font-bold text-red-600 dark:text-red-500">{summary?.highRisk ?? 0}</div>
            <p className="text-xs text-red-600/80 dark:text-red-400/80 mt-1">Requires immediate intervention</p>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-gray-600 dark:text-gray-400">Active Interventions</CardTitle>
            <Activity className="h-4 w-4 text-amber-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white">{summary?.activeInterventions ?? 0}</div>
            <p className="text-xs text-amber-600 dark:text-amber-400 mt-1">In progress or planned</p>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-gray-600 dark:text-gray-400">Cohort Attendance Avg</CardTitle>
            <TrendingUp className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white">
              {summary?.cohortAverageAttendance != null ? `${summary.cohortAverageAttendance}%` : '67.5%'}
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Marks avg: {summary?.cohortAverageMarks != null ? `${summary.cohortAverageMarks}%` : '59.8%'}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Cohort Comparison Table (Core requirement) */}
      <Card className="overflow-hidden">
        <CardHeader className="border-b border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-900/50">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <CardTitle className="text-lg">Cohort Academic Comparison & Risk Matrix</CardTitle>
              <CardDescription>
                Side-by-side comparative metrics across attendance, grades, CGPA, and automated risk scoring
              </CardDescription>
            </div>
            <Link to="/students">
              <Button variant="outline" size="sm" className="text-xs">
                View Full Directory <ArrowRight className="h-3 w-3 ml-1" />
              </Button>
            </Link>
          </div>
        </CardHeader>
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="text-xs text-gray-500 dark:text-gray-400 uppercase bg-gray-50 dark:bg-gray-900/80 border-b border-gray-200 dark:border-gray-800">
              <tr>
                <th className="px-6 py-3.5">Student</th>
                <th className="px-6 py-3.5">ID / Reg No</th>
                <th className="px-6 py-3.5 text-center">Attendance %</th>
                <th className="px-6 py-3.5 text-center">Avg Marks %</th>
                <th className="px-6 py-3.5 text-center">CGPA</th>
                <th className="px-6 py-3.5 text-center">Risk Level</th>
                <th className="px-6 py-3.5">Primary Risk Factor</th>
                <th className="px-6 py-3.5 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-800">
              {students.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-6 py-8 text-center text-gray-500">
                    No student records loaded in cohort.
                  </td>
                </tr>
              ) : (
                students.map((student) => {
                  const studentName = student.name || student.user?.name || 'Unknown';
                  const riskLevel = student.riskLevel || 'LOW';
                  const isHigh = riskLevel === 'HIGH';
                  const isMed = riskLevel === 'MEDIUM';

                  return (
                    <tr
                      key={student.id}
                      className="bg-white dark:bg-gray-900 hover:bg-gray-50 dark:hover:bg-gray-800/60 transition-colors"
                    >
                      <td className="px-6 py-4 font-medium text-gray-900 dark:text-gray-100 flex items-center">
                        <div className="h-8 w-8 rounded-full bg-indigo-100 dark:bg-indigo-900/50 text-indigo-700 dark:text-indigo-400 flex items-center justify-center mr-3 text-xs font-bold border border-indigo-300 dark:border-indigo-500/30">
                          {studentName.charAt(0)}
                        </div>
                        <div>
                          <div className="flex items-center space-x-1.5">
                            <span className="font-semibold text-gray-900 dark:text-white">{studentName}</span>
                            {studentName.includes('Student A') || student.studentNumber === 'STU-1001' ? (
                              <Badge className="bg-emerald-600 text-white text-[9px] px-1.5 py-0 h-4 font-medium">
                                ⭐ REFERENCE DATA
                              </Badge>
                            ) : (
                              <Badge variant="outline" className="text-amber-700 dark:text-amber-300 border-amber-300 text-[9px] px-1 py-0 h-4">
                                SYNTHETIC
                              </Badge>
                            )}
                          </div>
                          <div className="text-xs text-gray-500 dark:text-gray-400">{student.department || 'CSE'} • Sem {student.currentSemester || 5}</div>
                        </div>
                      </td>
                      <td className="px-6 py-4 font-mono text-xs text-gray-600 dark:text-gray-400">
                        {student.studentNumber}
                      </td>
                      <td className="px-6 py-4 text-center font-medium">
                        <span
                          className={`inline-block px-2 py-0.5 rounded text-xs font-semibold ${
                            (student.attendancePercentage ?? 100) >= 75
                              ? 'text-green-700 dark:text-green-400 bg-green-50 dark:bg-green-950/40'
                              : 'text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-950/40'
                          }`}
                        >
                          {student.attendancePercentage != null ? `${student.attendancePercentage}%` : 'N/A'}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-center font-medium">
                        <span
                          className={`inline-block px-2 py-0.5 rounded text-xs font-semibold ${
                            (student.averageMarksPercentage ?? 100) >= 50
                              ? 'text-green-700 dark:text-green-400 bg-green-50 dark:bg-green-950/40'
                              : 'text-red-700 dark:text-red-400 bg-red-50 dark:bg-red-950/40'
                          }`}
                        >
                          {student.averageMarksPercentage != null ? `${student.averageMarksPercentage}%` : 'N/A'}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-center font-semibold text-gray-800 dark:text-gray-200">
                        {student.currentCgpa}
                      </td>
                      <td className="px-6 py-4 text-center">
                        <Badge
                          variant={isHigh ? 'destructive' : isMed ? 'warning' : 'success'}
                          className="font-bold"
                        >
                          {riskLevel} {student.riskScore != null ? `(${student.riskScore})` : ''}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-xs text-gray-500 dark:text-gray-400 max-w-xs truncate">
                        {student.riskReason || (isHigh ? 'Low attendance and poor marks' : isMed ? 'Overdue coursework' : 'Good academic standing')}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <Link
                          to={'/students/' + student.id}
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

      {/* Faculty Profiles & Assigned Courses */}
      <Card>
        <CardHeader className="py-4">
          <CardTitle className="text-base font-bold flex items-center">
            <Users className="w-5 h-5 mr-2 text-indigo-600 dark:text-indigo-400" />
            Faculty Course Instructors & Cohort Coverage
          </CardTitle>
          <CardDescription className="text-xs">
            Showcase faculty accounts derived from official SRM AP timetable assignments
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-4 rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="font-bold text-gray-900 dark:text-white text-sm">Dr. Roopa Tirumalasetti</span>
                  <Badge variant="outline" className="text-xs">Associate Professor</Badge>
                </div>
                <div className="text-xs text-indigo-600 dark:text-indigo-400 font-semibold mb-1">
                  CSE 204: Design and Analysis of Algorithms (4 Credits)
                </div>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Classrooms: <strong className="text-gray-700 dark:text-gray-300 font-mono">(C 305), (S 404)</strong> • 48 Conducted Sessions
                </p>
              </div>
              <div className="mt-3 pt-3 border-t border-gray-100 dark:border-gray-800 flex items-center justify-between text-xs text-gray-500">
                <span>Cohort Enrolled: <strong>4 students</strong></span>
                <span className="text-emerald-600 dark:text-emerald-400 font-semibold">1 Active Intervention Assigned</span>
              </div>
            </div>

            <div className="p-4 rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="font-bold text-gray-900 dark:text-white text-sm">Dr. Aditya Kumar Sahu</span>
                  <Badge variant="outline" className="text-xs">Assistant Professor</Badge>
                </div>
                <div className="text-xs text-indigo-600 dark:text-indigo-400 font-semibold mb-1">
                  CSE 213: Advanced Java Programming (4 Credits)
                </div>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Classrooms: <strong className="text-gray-700 dark:text-gray-300 font-mono">(C 305), (S 404)</strong> • 49 Conducted Sessions
                </p>
              </div>
              <div className="mt-3 pt-3 border-t border-gray-100 dark:border-gray-800 flex items-center justify-between text-xs text-gray-500">
                <span>Cohort Enrolled: <strong>4 students</strong></span>
                <span className="text-amber-600 dark:text-amber-400 font-semibold">1 Active Intervention Assigned</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Charts & Actions Row */}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
        {/* Risk Distribution Chart */}
        <Card className="col-span-1">
          <CardHeader>
            <CardTitle className="text-base">Risk Distribution</CardTitle>
            <CardDescription>Current student cohort breakdown</CardDescription>
          </CardHeader>
          <CardContent className="h-[280px] flex flex-col items-center justify-center">
            <ResponsiveContainer width="100%" height="80%">
              <PieChart>
                <Pie
                  data={riskData}
                  cx="50%"
                  cy="50%"
                  innerRadius={55}
                  outerRadius={75}
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
                    fontSize: '12px',
                    boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)'
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
            <div className="flex items-center justify-center space-x-4 text-xs mt-2">
              <span className="flex items-center"><span className="h-2.5 w-2.5 rounded-full bg-emerald-500 mr-1.5"></span>Low</span>
              <span className="flex items-center"><span className="h-2.5 w-2.5 rounded-full bg-amber-500 mr-1.5"></span>Medium</span>
              <span className="flex items-center"><span className="h-2.5 w-2.5 rounded-full bg-red-500 mr-1.5"></span>High</span>
            </div>
          </CardContent>
        </Card>

        {/* Action Priority List */}
        <Card className="col-span-2">
          <CardHeader>
            <CardTitle className="text-base">Students Requiring Immediate Attention</CardTitle>
            <CardDescription>Automated priority intervention queue</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {summary?.recentHighRisk && summary.recentHighRisk.length > 0 ? (
                summary.recentHighRisk.map((student: any) => (
                  <div
                    key={student.id}
                    className="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 bg-gray-50 dark:bg-gray-800/40 rounded-lg border border-gray-200 dark:border-gray-700/60 gap-3"
                  >
                    <div>
                      <Link
                        to={'/students/' + student.id}
                        className="font-semibold text-gray-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 hover:underline"
                      >
                        {student.name}
                      </Link>
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{student.factor}</p>
                    </div>
                    <div className="flex items-center space-x-3">
                      <Badge variant="destructive">Score: {student.riskScore}</Badge>
                      <Badge variant={student.intervention === 'Pending' ? 'warning' : 'outline'}>
                        {student.intervention}
                      </Badge>
                      <Link to={'/students/' + student.id}>
                        <Button variant="outline" size="sm" className="h-7 text-xs">
                          Action
                        </Button>
                      </Link>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-8 text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-800/20 rounded-lg border border-dashed border-gray-300 dark:border-gray-800">
                  <CheckCircle className="h-8 w-8 text-green-500 mx-auto mb-2 opacity-80" />
                  <p className="text-sm font-medium">No urgent critical risk flags pending.</p>
                  <p className="text-xs mt-1">All cohort students are currently within acceptable academic thresholds.</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
