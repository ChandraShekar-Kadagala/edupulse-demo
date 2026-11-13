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
import { ErrorBoundary } from './components/common/ErrorBoundary';

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
      <ErrorBoundary fallbackTitle="Application Error">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          
          <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            
            <Route path="dashboard" element={
              <ErrorBoundary fallbackTitle="Dashboard Error">
                <Dashboard />
              </ErrorBoundary>
            } />
            
            <Route path="students" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'FACULTY']}>
                <ErrorBoundary fallbackTitle="Directory Error">
                  <StudentDirectory />
                </ErrorBoundary>
              </ProtectedRoute>
            } />
            
            <Route path="students/:id" element={
              <ErrorBoundary fallbackTitle="Student Profile Error">
                <StudentProfile />
              </ErrorBoundary>
            } />
            
            <Route path="interventions" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'FACULTY']}>
                <ErrorBoundary fallbackTitle="Interventions Error">
                  <Interventions />
                </ErrorBoundary>
              </ProtectedRoute>
            } />

            <Route path="import" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'FACULTY']}>
                <ErrorBoundary fallbackTitle="Import Error">
                  <DataImport />
                </ErrorBoundary>
              </ProtectedRoute>
            } />

            <Route path="integration" element={
              <ProtectedRoute allowedRoles={['STUDENT', 'ADMIN', 'FACULTY']}>
                <ErrorBoundary fallbackTitle="Integration Error">
                  <AccountLinking />
                </ErrorBoundary>
              </ProtectedRoute>
            } />
          </Route>
          
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </ErrorBoundary>
    </Router>
  );
}
