import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { FacultyDashboardView } from '../components/dashboard/FacultyDashboardView';
import { CohortOverview } from '../components/dashboard/CohortOverview';

/**
 * Role router. Every view renders only backend data; failed requests show an error with retry and
 * missing values show "—" — nothing is substituted with invented numbers.
 */
export default function Dashboard() {
  const { user } = useAuth();
  if (!user) return null;
  if (user.role === 'STUDENT') return <Navigate to={'/students/' + user.id} replace />;
  if (user.role === 'FACULTY') return <FacultyDashboardView />;
  return <CohortOverview />;
}
