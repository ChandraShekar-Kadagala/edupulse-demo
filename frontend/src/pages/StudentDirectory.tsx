import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Search, Filter, ArrowRight, RefreshCw, UserCheck } from 'lucide-react';
import { Link } from 'react-router-dom';
import api from '../services/api';

export default function StudentDirectory() {
  const [students, setStudents] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [riskFilter, setRiskFilter] = useState<'ALL' | 'HIGH' | 'MEDIUM' | 'LOW'>('ALL');

  const fetchStudents = async () => {
    setLoading(true);
    try {
      const res = await api.get('/students');
      setStudents(res.data || []);
    } catch (err) {
      console.error("Backend failed, using fallback data:", err);
      setStudents([
        { id: 1, name: 'Aarav Sharma', studentNumber: 'STU-1001', department: 'Computer Science & Engineering', currentCgpa: 9.4, attendancePercentage: 95.0, averageMarksPercentage: 92.0, riskLevel: 'LOW', riskScore: 0.0, user: { name: 'Aarav Sharma' } },
        { id: 2, name: 'Bhavna Patel', studentNumber: 'STU-1002', department: 'Computer Science & Engineering', currentCgpa: 7.9, attendancePercentage: 80.0, averageMarksPercentage: 79.0, riskLevel: 'LOW', riskScore: 5.0, user: { name: 'Bhavna Patel' } },
        { id: 3, name: 'Chirag Verma', studentNumber: 'STU-1003', department: 'Computer Science & Engineering', currentCgpa: 6.2, attendancePercentage: 65.0, averageMarksPercentage: 44.5, riskLevel: 'MEDIUM', riskScore: 40.5, user: { name: 'Chirag Verma' } },
        { id: 4, name: 'Arun Kumar', studentNumber: 'STU-1004', department: 'Computer Science & Engineering', currentCgpa: 5.1, attendancePercentage: 30.0, averageMarksPercentage: 24.5, riskLevel: 'HIGH', riskScore: 75.5, user: { name: 'Arun Kumar' } }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStudents();
  }, []);

  const filteredStudents = students.filter(s => {
    const name = (s.name || s.user?.name || '').toLowerCase();
    const number = (s.studentNumber || '').toLowerCase();
    const dept = (s.department || '').toLowerCase();
    const query = searchTerm.toLowerCase();

    const matchesSearch = name.includes(query) || number.includes(query) || dept.includes(query);
    const matchesRisk = riskFilter === 'ALL' || (s.riskLevel?.toUpperCase() === riskFilter);

    return matchesSearch && matchesRisk;
  });

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold tracking-tight text-gray-900 dark:text-white">
            Student Directory
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Complete student cohort roster, individual academic tracking, and risk profiles
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-3 w-full md:w-auto">
          {/* Risk Level Filter */}
          <div className="inline-flex rounded-lg border border-gray-200 dark:border-gray-700 p-1 bg-white dark:bg-gray-800 text-xs font-medium w-full sm:w-auto">
            {(['ALL', 'HIGH', 'MEDIUM', 'LOW'] as const).map((level) => (
              <button
                key={level}
                type="button"
                onClick={() => setRiskFilter(level)}
                className={`px-3 py-1.5 rounded-md transition-colors ${
                  riskFilter === level
                    ? 'bg-indigo-600 text-white font-semibold shadow-sm'
                    : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
                }`}
              >
                {level === 'ALL' ? 'All Risks' : level}
              </button>
            ))}
          </div>

          {/* Search box */}
          <div className="relative w-full sm:w-64">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search className="h-4 w-4 text-gray-400" />
            </div>
            <input
              type="text"
              className="block w-full pl-9 pr-3 py-2 border border-gray-300 dark:border-gray-700 rounded-lg leading-5 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm transition-colors"
              placeholder="Search by name or ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <Button variant="outline" size="sm" onClick={fetchStudents} title="Reload">
            <RefreshCw className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="text-xs text-gray-500 dark:text-gray-400 uppercase bg-gray-50 dark:bg-gray-900/80 border-b border-gray-200 dark:border-gray-800">
              <tr>
                <th className="px-6 py-4">Student</th>
                <th className="px-6 py-4">Student Number</th>
                <th className="px-6 py-4">Department</th>
                <th className="px-6 py-4 text-center">Attendance</th>
                <th className="px-6 py-4 text-center">Marks Avg</th>
                <th className="px-6 py-4 text-center">CGPA</th>
                <th className="px-6 py-4 text-center">Risk Level</th>
                <th className="px-6 py-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-800">
              {loading ? (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                    <RefreshCw className="h-6 w-6 animate-spin mx-auto mb-2 text-indigo-600" />
                    Loading student roster...
                  </td>
                </tr>
              ) : filteredStudents.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                    No students match the selected search and filter criteria.
                  </td>
                </tr>
              ) : (
                filteredStudents.map((student) => {
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
                        <div className="h-9 w-9 rounded-full bg-indigo-100 dark:bg-indigo-900/50 text-indigo-700 dark:text-indigo-400 flex items-center justify-center mr-3 text-sm font-bold border border-indigo-300 dark:border-indigo-500/30">
                          {studentName.charAt(0)}
                        </div>
                        <div>
                          <div className="font-semibold text-gray-900 dark:text-white">{studentName}</div>
                          <div className="text-xs text-gray-500 dark:text-gray-400">{student.email || student.user?.email || 'student@edupulse.local'}</div>
                        </div>
                      </td>
                      <td className="px-6 py-4 font-mono text-xs text-gray-700 dark:text-gray-300 font-medium">
                        {student.studentNumber}
                      </td>
                      <td className="px-6 py-4 text-gray-600 dark:text-gray-400 text-xs">
                        {student.department}
                      </td>
                      <td className="px-6 py-4 text-center">
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
                      <td className="px-6 py-4 text-center">
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
                          className="font-bold text-xs"
                        >
                          {riskLevel}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <Link
                          to={'/students/' + student.id}
                          className="inline-flex items-center text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:text-indigo-800 dark:hover:text-indigo-300 hover:underline"
                        >
                          View Profile <ArrowRight className="h-3 w-3 ml-1" />
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
    </div>
  );
}
