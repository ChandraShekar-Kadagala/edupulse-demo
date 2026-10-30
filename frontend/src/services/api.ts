import axios from 'axios';
import type { 
  StudentAcademicAnalytics, 
  TimetableSlot, 
  Faculty, 
  Student, 
  RiskAssessment, 
  Intervention 
} from '../types';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api'),
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = 'Bearer ' + token;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    let errorMsg = null;
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.dispatchEvent(new CustomEvent('auth-unauthorized'));
    } else if (error.response && error.response.status === 429) {
      errorMsg = "Too many requests. Please wait a moment before trying again.";
    } else if (!navigator.onLine) {
      errorMsg = "You are currently offline. Please check your internet connection.";
    } else if (error.response && error.response.status >= 500) {
      errorMsg = "Server is currently unavailable. Please try again later.";
    } else if (error.code === 'ERR_NETWORK') {
      errorMsg = "Unable to connect to the server. Please check your connection or try again later.";
    }
    
    if (errorMsg) {
      window.dispatchEvent(new CustomEvent('network-error', { detail: errorMsg }));
    }
    return Promise.reject(error);
  }
);

export function getSafeErrorMessage(error: any, fallback = 'An unexpected error occurred. Please try again.'): string {
  if (!error) return fallback;
  if (error.response?.data?.message && typeof error.response.data.message === 'string') {
    const msg = error.response.data.message;
    if (msg.includes('Exception') || msg.includes('at com.') || msg.includes('org.hibernate') || msg.includes('org.postgresql')) {
      return fallback;
    }
    return msg;
  }
  if (error.response?.status === 401) {
    return 'Your session has expired. Please log in again.';
  }
  if (error.response?.status === 403) {
    return 'You do not have permission to access this resource.';
  }
  if (error.response?.status === 404) {
    return 'Requested academic record was not found.';
  }
  if (error.response?.status === 429) {
    return 'Too many requests. Please wait a moment before trying again.';
  }
  if (error.response?.status >= 500) {
    return 'The server encountered an error. Please try again later.';
  }
  return fallback;
}

export const academicApi = {
  getStudentAnalytics: (studentId: number | string) => api.get<StudentAcademicAnalytics>(`/academic/student/${studentId}`),
  getStudentAnalyticsByRegNo: (regNo: string) => api.get<StudentAcademicAnalytics>(`/academic/student/number/${regNo}`),
  getTimetable: () => api.get<TimetableSlot[]>('/timetable'),
  getFacultyList: () => api.get<Faculty[]>('/faculty'),
  getFacultyById: (facultyId: number | string) => api.get<Faculty>(`/faculty/${facultyId}`),
  getDemoStatus: () => api.get<any>('/auth/demo-status'),
  getStudents: () => api.get<Student[]>('/students'),
  getRiskAssessment: (studentId: number | string) => api.get<RiskAssessment>(`/risk/student/${studentId}`),
  getInterventions: () => api.get<Intervention[]>('/interventions'),
};

export default api;
