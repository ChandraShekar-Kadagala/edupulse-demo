import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Layout from './components/layout/Layout';
import Dashboard from './pages/Dashboard';
import StudentDirectory from './pages/StudentDirectory';
import StudentProfile from './pages/StudentProfile';
import Interventions from './pages/Interventions';
import DataImport from './pages/DataImport';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import AccountLinking from './pages/AccountLinking';

function ProtectedRoute({ children, allowedRoles }: { children: React.ReactNode, allowedRoles?: string[] }) {
  const { isAuthenticated, user } = useAuth();
  
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  
  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }
  
  return children;
}

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        
        <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          
          <Route path="dashboard" element={<Dashboard />} />
          
          <Route path="students" element={
            <ProtectedRoute allowedRoles={['ADMIN', 'FACULTY']}>
              <StudentDirectory />
            </ProtectedRoute>
          } />
          
          <Route path="students/:id" element={<StudentProfile />} />
          
          <Route path="interventions" element={
            <ProtectedRoute allowedRoles={['ADMIN', 'FACULTY']}>
              <Interventions />
            </ProtectedRoute>
          } />

          <Route path="import" element={
            <ProtectedRoute allowedRoles={['ADMIN', 'FACULTY']}>
              <DataImport />
            </ProtectedRoute>
          } />

          <Route path="integration" element={
            <ProtectedRoute allowedRoles={['STUDENT', 'ADMIN', 'FACULTY']}>
              <AccountLinking />
            </ProtectedRoute>
          } />
        </Route>
        
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Router>
  );
}
