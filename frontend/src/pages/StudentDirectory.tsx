import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Search } from 'lucide-react';
import { Link } from 'react-router-dom';
import api from '../services/api';

export default function StudentDirectory() {
  const [students, setStudents] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    const fetchStudents = async () => {
      try {
        const res = await api.get('/students');
        setStudents(res.data);
      } catch (err) {
        console.error("Backend failed, using fallback data:", err);
        setStudents([
          { id: 1, studentNumber: 'STU1001', department: 'Computer Science', currentCgpa: 8.2, user: { name: 'Arun Kumar' } },
          { id: 2, studentNumber: 'STU1002', department: 'Information Tech', currentCgpa: 7.5, user: { name: 'Priya Sharma' } },
          { id: 3, studentNumber: 'STU1003', department: 'Computer Science', currentCgpa: 9.1, user: { name: 'Rahul Desai' } }
        ]);
      } finally {
        setLoading(false);
      }
    };
    fetchStudents();
  }, []);

  const filteredStudents = students.filter(s => 
    s.user.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    s.studentNumber.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Student Directory</h1>
        
        <div className="relative mt-4 sm:mt-0 w-full sm:w-64">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search className="h-4 w-4 text-gray-400" />
          </div>
          <input
            type="text"
            className="block w-full pl-10 pr-3 py-2 border border-gray-700 rounded-md leading-5 bg-gray-900 text-gray-300 placeholder-gray-400 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            placeholder="Search students..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left text-gray-400">
            <thead className="text-xs text-gray-400 uppercase bg-gray-900/50 border-b border-gray-800">
              <tr>
                <th className="px-6 py-4">Student</th>
                <th className="px-6 py-4">ID</th>
                <th className="px-6 py-4">Department</th>
                <th className="px-6 py-4">CGPA</th>
                <th className="px-6 py-4">Action</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-6 py-4 text-center">Loading students...</td>
                </tr>
              ) : filteredStudents.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-4 text-center">No students found.</td>
                </tr>
              ) : (
                filteredStudents.map((student) => (
                  <tr key={student.id} className="bg-gray-950 border-b border-gray-800 hover:bg-gray-900/80 transition-colors">
                    <td className="px-6 py-4 font-medium text-gray-100 flex items-center">
                      <div className="h-8 w-8 rounded-full bg-indigo-900/50 text-indigo-400 flex items-center justify-center mr-3 text-xs font-bold border border-indigo-500/30">
                        {student.user.name.charAt(0)}
                      </div>
                      {student.user.name}
                    </td>
                    <td className="px-6 py-4">{student.studentNumber}</td>
                    <td className="px-6 py-4">{student.department}</td>
                    <td className="px-6 py-4">{student.currentCgpa}</td>
                    <td className="px-6 py-4">
                      <Link to={'/students/' + student.id} className="font-medium text-indigo-400 hover:underline">
                        View Profile
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
