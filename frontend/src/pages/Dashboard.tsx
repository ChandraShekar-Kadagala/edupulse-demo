import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Activity, Users, AlertTriangle, CheckCircle, TrendingUp } from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip } from 'recharts';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Link, Navigate } from 'react-router-dom';

export default function Dashboard() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState<any>(null);

  if (user?.role === 'STUDENT') {
    return <Navigate to={'/students/' + user.id} replace />;
  }

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const res = await api.get('/dashboard/summary');
        setSummary(res.data);
      } catch (err) {
        console.error("Failed to load dashboard data:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) return <div className="text-gray-400">Loading dashboard...</div>;

  const riskData = [
    { name: 'Low Risk', value: summary?.lowRisk || 0, color: '#10b981' },
    { name: 'Medium Risk', value: summary?.mediumRisk || 0, color: '#f59e0b' },
    { name: 'High Risk', value: summary?.highRisk || 0, color: '#ef4444' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Dashboard Overview</h1>
        <div className="flex space-x-2">
          {user?.role === 'ADMIN' && <Badge variant="outline">Admin View</Badge>}
          <Badge variant="default">{(user as any)?.department || 'Computer Science'}</Badge>
        </div>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Students</CardTitle>
            <Users className="h-4 w-4 text-gray-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{summary?.totalStudents}</div>
          </CardContent>
        </Card>
        <Card className="border-red-900/50 bg-red-950/20">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-red-400">High Risk</CardTitle>
            <AlertTriangle className="h-4 w-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-500">{summary?.highRisk}</div>
            <p className="text-xs text-red-400/80">Requires immediate attention</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Interventions</CardTitle>
            <Activity className="h-4 w-4 text-indigo-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{summary?.activeInterventions}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Completed</CardTitle>
            <CheckCircle className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{summary?.completedInterventions}</div>
            <p className="text-xs text-gray-400">Resolved interventions</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {/* Risk Distribution Chart */}
        <Card className="col-span-1">
          <CardHeader>
            <CardTitle>Risk Distribution</CardTitle>
            <CardDescription>Current student population</CardDescription>
          </CardHeader>
          <CardContent className="h-[300px] flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={riskData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {riskData.map((entry, index) => (
                    <Cell key={'cell-' + index} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                  itemStyle={{ color: '#f3f4f6' }}
                />
              </PieChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Action List */}
        <Card className="col-span-2">
          <CardHeader>
            <CardTitle>Students Requiring Attention</CardTitle>
            <CardDescription>High risk students requiring intervention</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {summary?.recentHighRisk?.map((student: any) => (
                <div key={student.id} className="flex items-center justify-between p-4 bg-gray-900/50 rounded-lg border border-gray-800">
                  <div>
                    <Link to={'/students/' + student.id} className="font-semibold text-white hover:text-indigo-400 hover:underline">
                      {student.name}
                    </Link>
                    <p className="text-sm text-gray-400">{student.factor}</p>
                  </div>
                  <div className="flex items-center space-x-4">
                    <Badge variant="destructive">Score: {student.riskScore}</Badge>
                    <Badge variant={student.intervention === 'Pending' ? 'warning' : 'outline'}>
                      {student.intervention}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
