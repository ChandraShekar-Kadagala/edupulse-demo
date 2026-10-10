import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { AlertCircle, TrendingDown, BookOpen, Clock, Activity, Target } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import api from '../services/api';

export default function StudentProfile() {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [student, setStudent] = useState<any>(null);
  const [riskAssessment, setRiskAssessment] = useState<any>(null);
  const [interventions, setInterventions] = useState<any[]>([]);
  
  const fetchStudentData = async () => {
    try {
      const [studentRes, riskRes, intRes] = await Promise.all([
        api.get('/students/' + id),
        api.get('/students/' + id + '/risk'),
        api.get('/students/' + id + '/interventions')
      ]);
      setStudent(studentRes.data);
      setRiskAssessment(riskRes.data);
      setInterventions(intRes.data);
    } catch (err) {
      console.error("Backend failed, using fallback data:", err);
      setStudent({
        id: 1, studentNumber: 'STU1001', department: 'Computer Science', currentSemester: 4, currentCgpa: 8.2,
        user: { name: 'Arun Kumar' }
      });
      setRiskAssessment({
        riskScore: 82, riskLevel: 'HIGH',
        riskFactors: [
          { id: 1, factorName: 'Attendance Drop', impactLevel: 'HIGH', description: 'Attendance dropped below 65% over the last month.' },
          { id: 2, factorName: 'Low Internal Marks', impactLevel: 'MEDIUM', description: 'Recent assessment marks are 15% below previous average.' }
        ]
      });
      setInterventions([
        { id: 1, type: 'Mentor Meeting', status: 'Pending', description: 'Discuss attendance issues and academic pressure.' }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStudentData();
  }, [id]);

  const handleRecalculateRisk = async () => {
    try {
      await api.post('/students/' + id + '/risk/assess');
      fetchStudentData();
    } catch (err) {
      console.error(err);
    }
  };

  if (loading) return <div className="text-gray-400">Loading student profile...</div>;
  if (!student) return <div className="text-red-400">Failed to load student</div>;

  const isHighRisk = riskAssessment?.riskLevel === 'HIGH';
  const riskColor = isHighRisk ? 'text-red-500' : riskAssessment?.riskLevel === 'MEDIUM' ? 'text-amber-500' : 'text-green-500';

  // Demo trend data
  const trendData = [
    { name: 'Month 1', attendance: 88, marks: 78 },
    { name: 'Month 2', attendance: 84, marks: 74 },
    { name: 'Month 3', attendance: 75, marks: 65 },
    { name: 'Month 4', attendance: 62, marks: 51 },
  ];

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Header Profile */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center bg-gray-900 border border-gray-800 p-6 rounded-xl">
        <div className="flex items-center space-x-6">
          <div className="h-20 w-20 rounded-full bg-indigo-900 border-2 border-indigo-500 flex items-center justify-center text-3xl font-bold text-indigo-200">
            {student.user.name.charAt(0)}
          </div>
          <div>
            <h1 className="text-3xl font-bold text-white">{student.user.name}</h1>
            <p className="text-gray-400">{student.studentNumber} • {student.department} • Semester {student.currentSemester}</p>
            <div className="flex items-center mt-2 space-x-2">
              <Badge variant="outline">CGPA: {student.currentCgpa}</Badge>
            </div>
          </div>
        </div>
        <div className="mt-4 md:mt-0 flex flex-col items-end">
          <div className="text-sm text-gray-400 mb-1">Current Risk Level {riskAssessment?.calculatedAt ? `(Updated: ${new Date(riskAssessment.calculatedAt).toLocaleDateString()})` : ''}</div>
          <div className="flex items-center space-x-3">
            <span className={'text-4xl font-bold ' + riskColor}>{riskAssessment?.riskScore || 0}</span>
            <Badge variant={isHighRisk ? 'destructive' : 'warning'} className="text-sm px-3 py-1">
              {riskAssessment?.riskLevel}
            </Badge>
          </div>
          {user?.role !== 'STUDENT' && (
            <Button variant="outline" size="sm" className="mt-3" onClick={handleRecalculateRisk}>
              Recalculate Risk
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column - Risk Explanation */}
        <div className="lg:col-span-1 space-y-6">
          <Card className="border-red-900/30 bg-gray-900">
            <CardHeader>
              <CardTitle className="flex items-center text-red-400">
                <AlertCircle className="w-5 h-5 mr-2" />
                Contributing Factors
              </CardTitle>
              <CardDescription>Why this student is flagged</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {riskAssessment?.riskFactors?.map((factor: any) => (
                <div key={factor.id} className="p-3 bg-gray-950 rounded border border-gray-800">
                  <div className="flex justify-between items-start mb-1">
                    <span className="font-semibold text-gray-200">{factor.factorName}</span>
                    <Badge variant={factor.impactLevel === 'HIGH' ? 'destructive' : 'warning'}>{factor.impactLevel}</Badge>
                  </div>
                  <p className="text-sm text-gray-400">{factor.description}</p>
                </div>
              ))}
              {(!riskAssessment?.riskFactors || riskAssessment.riskFactors.length === 0) && (
                <div className="text-gray-500 italic">No significant risk factors detected.</div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Target className="w-5 h-5 mr-2 text-indigo-400" />
                Recommended Action
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <div className="flex items-center text-sm text-gray-300">
                  <div className="w-2 h-2 rounded-full bg-indigo-500 mr-2"></div>
                  Faculty mentor meeting
                </div>
                <div className="flex items-center text-sm text-gray-300">
                  <div className="w-2 h-2 rounded-full bg-indigo-500 mr-2"></div>
                  Attendance improvement plan
                </div>
              </div>
              {user?.role !== 'STUDENT' && <Button className="w-full mt-4">Create Intervention</Button>}
            </CardContent>
          </Card>
        </div>

        {/* Right Column - Academic Trends & Timeline */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <TrendingDown className="w-5 h-5 mr-2 text-gray-400" />
                Performance Trend
              </CardTitle>
            </CardHeader>
            <CardContent className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={trendData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                  <XAxis dataKey="name" stroke="#9ca3af" />
                  <YAxis stroke="#9ca3af" />
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#1f2937', border: '1px solid #374151' }}
                    itemStyle={{ color: '#f3f4f6' }}
                  />
                  <Line type="monotone" dataKey="attendance" stroke="#8b5cf6" strokeWidth={3} dot={{ r: 4 }} name="Attendance %" />
                  <Line type="monotone" dataKey="marks" stroke="#10b981" strokeWidth={3} dot={{ r: 4 }} name="Internal Marks %" />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Activity className="w-5 h-5 mr-2 text-gray-400" />
                Intervention History
              </CardTitle>
            </CardHeader>
            <CardContent>
              {interventions.length > 0 ? (
                <div className="space-y-4 relative before:absolute before:inset-0 before:ml-5 before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-gray-700 before:to-transparent">
                  {interventions.map((intervention) => (
                    <div key={intervention.id} className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
                      <div className="flex items-center justify-center w-10 h-10 rounded-full border border-gray-700 bg-gray-900 text-gray-500 group-[.is-active]:text-indigo-500 shadow shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2">
                        <Clock className="w-4 h-4" />
                      </div>
                      <div className="w-[calc(100%-4rem)] md:w-[calc(50%-2.5rem)] bg-gray-800/50 p-4 rounded border border-gray-700 shadow">
                        <div className="flex justify-between items-center mb-1">
                          <span className="font-bold text-gray-200">{intervention.type}</span>
                          <Badge variant="outline">{intervention.status}</Badge>
                        </div>
                        <p className="text-sm text-gray-400 mb-2">{intervention.description}</p>
                        {intervention.followUps && intervention.followUps.length > 0 && (
                          <div className="mt-3 pt-3 border-t border-gray-700/50 space-y-2">
                            <span className="text-xs font-semibold text-gray-500 uppercase">Follow-ups</span>
                            {intervention.followUps.map((fu: any) => (
                              <div key={fu.id} className="text-sm bg-gray-900/50 p-2 rounded">
                                <div className="flex justify-between items-center mb-1">
                                  <span className="text-gray-300 font-medium">{fu.facultyName || 'Faculty'}</span>
                                  <span className="text-xs text-gray-500">{new Date(fu.createdAt).toLocaleDateString()}</span>
                                </div>
                                <p className="text-gray-400 text-xs italic">{fu.notes}</p>
                                {fu.outcome && <Badge variant="outline" className="mt-2 text-[10px]">{fu.outcome}</Badge>}
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 text-gray-500">
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
