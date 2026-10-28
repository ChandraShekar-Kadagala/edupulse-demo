import React, { useEffect, useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { AlertTriangle, Clock, User, CheckCircle2, RefreshCw, Calendar } from 'lucide-react';
import api from '../services/api';

export default function Interventions() {
  const [interventions, setInterventions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchInterventions = async () => {
    setLoading(true);
    try {
      const res = await api.get('/interventions');
      setInterventions(res.data || []);
    } catch (err) {
      console.error("Backend failed, using fallback data:", err);
      setInterventions([
        {
          id: 1,
          type: 'Urgent Academic Mentoring',
          priority: 'HIGH',
          status: 'PLANNED',
          description: 'Severe attendance shortfall (30%) and declining assessment scores. Immediate parent-faculty counseling scheduled.',
          createdAt: new Date(Date.now() - 86400000).toISOString(),
          dueDate: '2026-11-05',
          studentName: 'Arun Kumar',
          assignedFacultyName: 'Dr. Sarah Smith'
        },
        {
          id: 2,
          type: 'Remedial Tutoring & Coursework Support',
          priority: 'MEDIUM',
          status: 'IN_PROGRESS',
          description: 'Assignment completion assistance for CSE101 to recover 4 overdue deliverables.',
          createdAt: new Date(Date.now() - 3 * 86400000).toISOString(),
          dueDate: '2026-11-10',
          studentName: 'Chirag Verma',
          assignedFacultyName: 'Dr. Sarah Smith'
        },
        {
          id: 3,
          type: 'Assignment Submission Check-in',
          priority: 'LOW',
          status: 'COMPLETED',
          description: 'Resolved submission delay for Binary Search Trees coursework. Student back on track.',
          createdAt: new Date(Date.now() - 7 * 86400000).toISOString(),
          dueDate: '2026-10-27',
          studentName: 'Bhavna Patel',
          assignedFacultyName: 'Dr. Sarah Smith'
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInterventions();
  }, []);

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold tracking-tight text-gray-900 dark:text-white">
            Active Interventions
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Faculty counseling, remedial tutoring assignments, and student support workflows
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Button variant="outline" size="sm" onClick={fetchInterventions} title="Reload">
            <RefreshCw className="h-4 w-4" />
          </Button>
          <Button>New Intervention</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {loading ? (
          <div className="col-span-3 text-center py-12 text-gray-500 dark:text-gray-400">
            <RefreshCw className="h-6 w-6 animate-spin mx-auto mb-2 text-indigo-600" />
            Loading interventions...
          </div>
        ) : interventions.length === 0 ? (
          <div className="col-span-3 text-center text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-900/50 rounded-xl border border-dashed border-gray-300 dark:border-gray-800 p-12">
            <CheckCircle2 className="h-10 w-10 text-green-500 mx-auto mb-3 opacity-70" />
            <p className="font-medium text-base">No active interventions requiring attention.</p>
            <p className="text-xs mt-1 text-gray-400">All student alerts have been successfully resolved.</p>
          </div>
        ) : (
          interventions.map((inv) => {
            const studentDisplayName = inv.studentName || inv.student?.user?.name || 'Student';
            const facultyDisplayName = inv.assignedFacultyName || inv.assignedFaculty?.user?.name || 'Assigned Faculty';
            const isHigh = inv.priority === 'HIGH';
            const isMed = inv.priority === 'MEDIUM';

            return (
              <Card
                key={inv.id}
                className="hover:border-indigo-500/50 transition-all hover:shadow-md cursor-pointer flex flex-col justify-between"
              >
                <CardHeader className="pb-3">
                  <div className="flex justify-between items-start mb-2">
                    <Badge variant={isHigh ? 'destructive' : isMed ? 'warning' : 'outline'}>
                      {inv.priority} Priority
                    </Badge>
                    <Badge
                      variant={
                        inv.status === 'COMPLETED'
                          ? 'success'
                          : inv.status === 'IN_PROGRESS'
                          ? 'warning'
                          : 'outline'
                      }
                    >
                      {inv.status}
                    </Badge>
                  </div>
                  <CardTitle className="text-base text-gray-900 dark:text-white">
                    {inv.type}
                  </CardTitle>
                  <div className="text-sm mt-3 space-y-1.5">
                    <p className="font-semibold text-gray-900 dark:text-gray-100 flex items-center">
                      <User className="h-3.5 w-3.5 text-indigo-500 mr-1.5" />
                      Student: {studentDisplayName}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-3 leading-relaxed">
                      {inv.description}
                    </p>
                  </div>
                </CardHeader>
                <CardContent className="pt-0">
                  <div className="text-xs text-gray-500 dark:text-gray-400 pt-3 border-t border-gray-100 dark:border-gray-800 flex justify-between items-center">
                    <span className="flex items-center">
                      <Calendar className="h-3 w-3 mr-1 opacity-70" />
                      Due: {inv.dueDate || 'Flexible'}
                    </span>
                    <span className="truncate max-w-[120px]" title={facultyDisplayName}>
                      {facultyDisplayName}
                    </span>
                  </div>
                </CardContent>
              </Card>
            );
          })
        )}
      </div>
    </div>
  );
}
