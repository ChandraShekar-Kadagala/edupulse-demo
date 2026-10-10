export type Role = 'ADMIN' | 'FACULTY' | 'STUDENT';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type InterventionStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface User {
  id: number;
  username: string;
  role: Role;
  name: string;
  email: string;
}

export interface Student {
  id: number;
  user: User;
  studentNumber: string;
  department: string;
  currentSemester: number;
  currentCgpa: number;
}

export interface RiskFactor {
  id: number;
  factorName: string;
  factorValue: string;
  impactLevel: string;
  description: string;
}

export interface RiskAssessment {
  id: number;
  studentId: number;
  calculatedAt: string;
  riskScore: number;
  riskLevel: RiskLevel;
  riskFactors: RiskFactor[];
}

export interface Intervention {
  id: number;
  student: Student;
  assignedFacultyId: number;
  type: string;
  description: string;
  status: InterventionStatus;
  priority: string;
  startDate: string;
  dueDate: string;
}

export interface FollowUp {
  id: number;
  interventionId: number;
  facultyId: number;
  notes: string;
  outcome: string;
  previousRiskScore: number;
  newRiskScore: number;
  date: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  username: string;
  email: string;
  roles: string[];
}
