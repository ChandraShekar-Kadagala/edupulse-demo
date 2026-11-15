import { useEffect, useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { User, CheckCircle2, RefreshCw, Calendar, Plus, X } from 'lucide-react';
import { academicApi, getSafeErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { EmptyState, ErrorState, LoadingState } from '../components/common/StateViews';
import type { Intervention, Student, Faculty, User } from '../types';

const INTERVENTION_TYPES = [
  'Mentoring Meeting',
  'Attendance Counselling',
  'Remedial Tutoring',
  'Assignment Support',
  'Parent-Faculty Meeting',
  'Wellbeing Check-in',
];

export default function Interventions() {
  const { user } = useAuth();
  const [interventions, setInterventions] = useState<Intervention[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);

  const fetchInterventions = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await academicApi.getInterventions();
      setInterventions(res.data || []);
    } catch (err) {
      setError(getSafeErrorMessage(err, 'Unable to load interventions.'));
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
            Interventions
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Plan meetings and support for students who need it, and track how they progress.
          </p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <Button variant="outline" size="sm" onClick={fetchInterventions} title="Reload" aria-label="Reload">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
          <Button onClick={() => setShowForm(true)}>
            <Plus className="h-4 w-4 mr-1.5" /> New Intervention
          </Button>
        </div>
      </div>

      {error && !loading && (
        <ErrorState title="Interventions unavailable" message={error} onRetry={fetchInterventions} />
      )}

      {loading ? (
        <LoadingState title="Loading interventions" detail="Fetching the latest student support workflows." />
      ) : !error && interventions.length === 0 ? (
        <EmptyState title="No interventions yet">
          When a student needs support, create an intervention to plan a meeting and track it here.
        </EmptyState>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
          {interventions.map((inv) => {
            const studentDisplayName = inv.studentName || inv.student?.user?.name || 'Student';
            const facultyDisplayName = inv.assignedFacultyName || 'Assigned faculty';
            const isHigh = inv.priority === 'HIGH';
            const isMed = inv.priority === 'MEDIUM';

            return (
              <Card key={inv.id} className="transition-all hover:border-indigo-500/50 hover:shadow-md flex flex-col justify-between">
                <CardHeader className="pb-3">
                  <div className="flex justify-between items-start mb-2 gap-2">
                    <Badge variant={isHigh ? 'destructive' : isMed ? 'warning' : 'outline'}>
                      {inv.priority || 'NORMAL'} priority
                    </Badge>
                    <Badge variant={inv.status === 'COMPLETED' ? 'success' : inv.status === 'IN_PROGRESS' ? 'warning' : 'outline'}>
                      {inv.status}
                    </Badge>
                  </div>
                  <CardTitle className="text-base text-gray-900 dark:text-white">{inv.type}</CardTitle>
                  <div className="text-sm mt-3 space-y-1.5">
                    <p className="font-semibold text-gray-900 dark:text-gray-100 flex items-center">
                      <User className="h-3.5 w-3.5 text-indigo-500 mr-1.5 shrink-0" />
                      {studentDisplayName}
                    </p>
                    {inv.description && (
                      <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-3 leading-relaxed">{inv.description}</p>
                    )}
                  </div>
                </CardHeader>
                <CardContent className="pt-0">
                  <div className="text-xs text-gray-500 dark:text-gray-400 pt-3 border-t border-gray-100 dark:border-gray-800 flex justify-between items-center gap-2">
                    <span className="flex items-center">
                      <Calendar className="h-3 w-3 mr-1 opacity-70" />
                      {inv.dueDate ? `Due ${inv.dueDate}` : 'Flexible'}
                    </span>
                    <span className="truncate max-w-[120px]" title={facultyDisplayName}>{facultyDisplayName}</span>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {showForm && (
        <NewInterventionModal
          user={user}
          onClose={() => setShowForm(false)}
          onCreated={() => {
            setShowForm(false);
            fetchInterventions();
          }}
        />
      )}
    </div>
  );
}

function NewInterventionModal({
  user,
  onClose,
  onCreated,
}: {
  user: User | null;
  onClose: () => void;
  onCreated: () => void;
}) {
  const isAdmin = user?.role === 'ADMIN';
  const [students, setStudents] = useState<Student[]>([]);
  const [faculty, setFaculty] = useState<Faculty[]>([]);
  const [loadingLists, setLoadingLists] = useState(true);
  const [listError, setListError] = useState<string | null>(null);

  const [studentId, setStudentId] = useState('');
  const [facultyId, setFacultyId] = useState(isAdmin ? '' : String(user?.id ?? ''));
  const [type, setType] = useState(INTERVENTION_TYPES[0]);
  const [priority, setPriority] = useState('MEDIUM');
  const [dueDate, setDueDate] = useState('');
  const [description, setDescription] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      setLoadingLists(true);
      setListError(null);
      try {
        const [s, f] = await Promise.all([
          academicApi.getStudents(),
          isAdmin ? academicApi.getFacultyList() : Promise.resolve({ data: [] as Faculty[] }),
        ]);
        setStudents(s.data || []);
        setFaculty(f.data || []);
      } catch (e) {
        setListError(getSafeErrorMessage(e, 'Could not load the student list.'));
      } finally {
        setLoadingLists(false);
      }
    })();
  }, [isAdmin]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);
    if (!studentId) return setSubmitError('Choose a student.');
    if (!facultyId) return setSubmitError('Choose the faculty mentor.');
    if (!type.trim()) return setSubmitError('Choose an intervention type.');
    setSubmitting(true);
    try {
      await academicApi.createIntervention({
        studentId: Number(studentId),
        assignedFacultyId: Number(facultyId),
        type: type.trim(),
        description: description.trim(),
        priority,
        dueDate: dueDate || null,
      });
      onCreated();
    } catch (err) {
      setSubmitError(getSafeErrorMessage(err, 'Could not create the intervention.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 p-0 sm:p-4" role="dialog" aria-modal="true" aria-label="New intervention">
      <div className="bg-white dark:bg-gray-900 w-full sm:max-w-lg sm:rounded-2xl rounded-t-2xl shadow-xl border border-gray-200 dark:border-gray-800 max-h-[92vh] overflow-y-auto">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100 dark:border-gray-800 sticky top-0 bg-white dark:bg-gray-900">
          <h2 className="font-bold text-gray-900 dark:text-white">New intervention</h2>
          <button onClick={onClose} aria-label="Close" className="p-1 rounded-md text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={submit} className="p-5 space-y-4">
          {listError && <p role="alert" className="text-sm text-red-600 dark:text-red-400">{listError}</p>}

          <label className="block text-sm">
            <span className="text-gray-700 dark:text-gray-300 font-medium">Student</span>
            <select
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
              disabled={loadingLists}
              className="mt-1 w-full rounded-md border border-gray-300 dark:border-gray-700 bg-transparent px-3 py-2 text-sm"
            >
              <option value="">{loadingLists ? 'Loading students…' : 'Select a student'}</option>
              {students.map((s) => (
                <option key={s.id} value={s.id}>{(s.name || s.studentNumber)}{s.studentNumber ? ` · ${s.studentNumber}` : ''}</option>
              ))}
            </select>
          </label>

          {isAdmin && (
            <label className="block text-sm">
              <span className="text-gray-700 dark:text-gray-300 font-medium">Assign to faculty</span>
              <select
                value={facultyId}
                onChange={(e) => setFacultyId(e.target.value)}
                disabled={loadingLists}
                className="mt-1 w-full rounded-md border border-gray-300 dark:border-gray-700 bg-transparent px-3 py-2 text-sm"
              >
                <option value="">Select faculty</option>
                {faculty.map((f) => (
                  <option key={f.id} value={f.id}>{f.name || f.username}</option>
                ))}
              </select>
            </label>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <label className="block text-sm">
              <span className="text-gray-700 dark:text-gray-300 font-medium">Type</span>
              <select value={type} onChange={(e) => setType(e.target.value)} className="mt-1 w-full rounded-md border border-gray-300 dark:border-gray-700 bg-transparent px-3 py-2 text-sm">
                {INTERVENTION_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </label>
            <label className="block text-sm">
              <span className="text-gray-700 dark:text-gray-300 font-medium">Priority</span>
              <select value={priority} onChange={(e) => setPriority(e.target.value)} className="mt-1 w-full rounded-md border border-gray-300 dark:border-gray-700 bg-transparent px-3 py-2 text-sm">
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </label>
          </div>

          <label className="block text-sm">
            <span className="text-gray-700 dark:text-gray-300 font-medium">Meeting / due date</span>
            <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)}
              className="mt-1 w-full rounded-md border border-gray-300 dark:border-gray-700 bg-transparent px-3 py-2 text-sm" />
          </label>

          <label className="block text-sm">
            <span className="text-gray-700 dark:text-gray-300 font-medium">Notes for the student</span>
            <textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)}
              placeholder="What will you cover in this meeting / how will you support them?"
              className="mt-1 w-full rounded-md border border-gray-300 dark:border-gray-700 bg-transparent px-3 py-2 text-sm" />
          </label>

          {submitError && <p role="alert" className="text-sm text-red-600 dark:text-red-400">{submitError}</p>}

          <div className="flex justify-end gap-2 pt-1">
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={submitting || loadingLists}>
              {submitting ? 'Creating…' : 'Create intervention'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
