import React, { useEffect, useState } from 'react';
import { Card, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import api from '../services/api';

export default function Interventions() {
  const [interventions, setInterventions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchInterventions = async () => {
      try {
        const res = await api.get('/interventions');
        setInterventions(res.data);
      } catch (err) {
        console.error("Backend failed, using fallback data:", err);
        setInterventions([
          { id: 1, type: 'Mentor Meeting', priority: 'HIGH', status: 'Pending', description: 'Discuss attendance issues', createdAt: new Date().toISOString(), dueDate: '2026-09-15', student: { user: { name: 'Arun Kumar' } } },
          { id: 2, type: 'Tutor Assigned', priority: 'MEDIUM', status: 'In Progress', description: 'Remedial classes for Mathematics', createdAt: new Date().toISOString(), dueDate: '2026-09-20', student: { user: { name: 'Priya Sharma' } } }
        ]);
      } finally {
        setLoading(false);
      }
    };
    fetchInterventions();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Active Interventions</h1>
        <Button>New Intervention</Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {loading ? (
          <div className="col-span-3 text-center text-gray-400">Loading interventions...</div>
        ) : interventions.length === 0 ? (
          <div className="col-span-3 text-center text-gray-500 bg-gray-900/50 rounded-xl border border-gray-800 p-12">
            No active interventions requiring your attention.
          </div>
        ) : (
          interventions.map((inv) => (
            <Card key={inv.id} className="bg-gray-900 border-gray-800 hover:border-indigo-500/50 transition-colors cursor-pointer">
              <CardHeader>
                <div className="flex justify-between items-start mb-2">
                  <Badge variant={inv.priority === 'HIGH' ? 'destructive' : 'warning'}>{inv.priority} Priority</Badge>
                  <Badge variant="outline">{inv.status}</Badge>
                </div>
                <CardTitle className="text-lg">{inv.type}</CardTitle>
                <div className="text-sm text-gray-400 mt-2">
                  <p className="font-medium text-gray-200">Student: {inv.student?.user?.name}</p>
                  <p className="mt-1 line-clamp-2">{inv.description}</p>
                </div>
                <div className="text-xs text-gray-500 mt-4 flex justify-between">
                  <span>Created: {new Date(inv.createdAt).toLocaleDateString()}</span>
                  <span>Due: {inv.dueDate}</span>
                </div>
              </CardHeader>
            </Card>
          ))
        )}
      </div>
    </div>
  );
}
