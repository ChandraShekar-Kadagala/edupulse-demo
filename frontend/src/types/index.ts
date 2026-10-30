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
  provenance?: string;
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

export interface TimetableSlot {
  day: string;
  slotNumber: number;
  timeRange: string;
  courseCode: string;
  courseName: string;
  classroom: string;
  facultyName: string;
}

export interface Faculty {
  id: number;
  username: string;
  name: string;
  email: string;
  department: string;
  title: string;
  assignedCourses: string[];
  classroom?: string;
  enrolledStudentCount: number;
  averageAttendance?: number;
  highRiskCount?: number;
}

export interface FutureTargetPlanner {
  courseCode: string;
  courseName: string;
  currentObtainedMarks: number;
  currentMaxMarks: number;
  currentConvertedMarks: number;
  maxConvertedMarks: number;
  currentPercentage: number;
  pendingAssessmentName: string;
  pendingWeightage: number;
  minImprovementTarget: number;
  recommendedTarget: number;
  stretchTarget: number;
  targetType: string;
  targetAffectsRisk: string;
}

export interface SubjectRiskDetail {
  courseCode: string;
  courseName: string;
  attendancePercentage?: number;
  attendanceStatus?: string;
  evaluatedMarksPercentage?: number;
  marksStatus?: string;
  riskContribution: number;
  riskLevel: string;
  riskFactors: string[];
}

export interface RiskEngineExplanation {
  overallRiskScore: number;
  riskLevel: string;
  attendancePercentage?: number;
  attendanceRiskContribution: number;
  attendanceFormulaExplanation: string;
  marksPercentage?: number;
  marksRiskContribution: number;
  marksFormulaExplanation: string;
  missingAssignmentsCount: number;
  missingAssignmentsRiskContribution: number;
  missingAssignmentsFormulaExplanation: string;
  calculationFormula: string;
  calculationTimestamp: string;
  studentName: string;
  studentNumber: string;
  subjectRisks: SubjectRiskDetail[];
}

export interface CourseAttendance {
  courseId: number;
  courseCode: string;
  courseName: string;
  totalClasses: number;
  presentClasses: number;
  absentClasses: number;
  odMlClasses: number;
  attendancePercentage?: number;
  status: string;
  period: string;
  facultyName: string;
  classroom: string;
}

export interface AssessmentMark {
  markId?: number;
  assessmentId: number;
  courseCode: string;
  courseName: string;
  assessmentName: string;
  assessmentType: string;
  marksObtained?: number;
  maxMarks: number;
  convertedMarks?: number;
  maxConvertedMarks: number;
  percentage?: number;
  weightage: number;
  status: string;
  facultyName: string;
  classroom: string;
}

export interface AcademicRecommendation {
  type: string;
  title: string;
  description: string;
  priority: string;
  actionableStep: string;
}

export interface PerformanceTrendPoint {
  date: string;
  label: string;
  attendanceCumulative?: number;
  internalMarkCumulative?: number;
}

export interface StudentAcademicAnalytics {
  studentId: number;
  studentNumber: string;
  name: string;
  department: string;
  currentSemester: number;
  currentCgpa: number;
  provenance: string;
  dataStatus: string;
  lastActivityDate?: string;
  isStale: boolean;
  attendancePeriod: string;
  lastUpdated: string;
  disclaimer: string;
  hasAttendanceData: boolean;
  totalClasses: number;
  presentClasses: number;
  absentClasses: number;
  overallAttendancePercentage?: number;
  courseAttendanceBreakdown: CourseAttendance[];
  hasMarksData: boolean;
  evaluatedAssessmentsCount: number;
  overallAverageMarksPercentage?: number;
  assessmentBreakdown: AssessmentMark[];
  hasAssignmentData: boolean;
  totalAssignments: number;
  submittedAssignments: number;
  missingAssignments: number;
  riskScore: number;
  riskLevel: string;
  riskReason: string;
  riskCategory: string;
  riskExplanation?: string;
  recommendations: AcademicRecommendation[];
  targetPlanner: FutureTargetPlanner[];
  riskEngineExplanation?: RiskEngineExplanation;
  timetable: TimetableSlot[];
  strengths: string[];
  improvementAreas: string[];
  upcomingConcerns: string[];
  performanceTrends: PerformanceTrendPoint[];
}

export interface StudentCoursePerformance {
  studentId: number;
  studentName: string;
  studentNumber: string;
  cgpa: number;
  provenance: string;
  courseTotalClasses: number;
  coursePresentClasses: number;
  courseAbsentClasses: number;
  courseAttendancePercentage?: number;
  courseAttendanceStatus: 'NORMAL' | 'BELOW_THRESHOLD' | string;
  overallAttendancePercentage?: number;
  courseAverageMarksPercentage?: number;
  marks: AssessmentMark[];
  missingAssignmentsCount: number;
  riskLevel: RiskLevel | string;
  riskScore: number;
  riskReason: string;
}

export interface AssessmentOverview {
  id: number;
  name: string;
  type: string;
  maxMarks: number;
  weightage: number;
  status: 'EVALUATED' | 'PENDING' | string;
  classAverage?: number;
}

export interface RiskEngineSummary {
  formula: string;
  attendanceThresholdRule: string;
  marksThresholdRule: string;
  missingAssignmentRule: string;
  pendingAssessmentRule: string;
}

export interface FacultyDashboard {
  faculty: Faculty;
  courseCode: string;
  courseName: string;
  credits: number;
  classroom: string;
  slotDetails: string;
  totalConductedSessions: number;
  courseAverageAttendance?: number;
  courseAverageMarks?: number;
  enrolledStudentsCount: number;
  highRiskCount: number;
  mediumRiskCount: number;
  lowRiskCount: number;
  students: StudentCoursePerformance[];
  assessments: AssessmentOverview[];
  activeInterventions: Intervention[];
  riskEngineSummary?: RiskEngineSummary;
}
