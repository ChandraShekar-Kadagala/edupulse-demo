export type Role = 'ADMIN' | 'FACULTY' | 'STUDENT';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type InterventionStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface User {
  id: number;
  username: string;
  role: Role;
  name: string;
  email: string;
  authProvider?: string;
}

export interface Student {
  id: number;
  name?: string;
  email?: string;
  user?: User;
  studentNumber: string;
  department: string;
  currentSemester: number;
  currentCgpa: number;
  riskLevel?: RiskLevel | string;
  riskScore?: number;
  attendancePercentage?: number;
  averageMarksPercentage?: number;
  riskReason?: string;
  totalClasses?: number;
  presentClasses?: number;
}

export interface RiskFactor {
  id?: number;
  factorName?: string;
  factorType?: string;
  factorValue?: string;
  impactLevel?: string;
  impactScore?: number;
  description: string;
}

export interface RiskAssessment {
  id: number;
  studentId: number;
  studentName?: string;
  calculatedAt: string;
  riskScore: number;
  riskLevel: RiskLevel;
  riskFactors: RiskFactor[];
}

export interface Intervention {
  id: number;
  studentId?: number;
  studentName?: string;
  student?: Student;
  assignedFacultyId?: number;
  assignedFacultyName?: string;
  type: string;
  description: string;
  status: InterventionStatus;
  priority: string;
  startDate?: string;
  dueDate?: string;
  createdAt?: string;
  followUps?: FollowUp[];
}

export interface FollowUp {
  id: number;
  interventionId: number;
  facultyId: number;
  facultyName?: string;
  notes: string;
  outcome: string;
  previousRiskScore?: number;
  newRiskScore?: number;
  createdAt?: string;
  date?: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  username: string;
  email?: string;
  role?: string;
  roles?: string[];
  authProvider?: string;
}
