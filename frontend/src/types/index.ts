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

export interface CourseSummary {
  courseCode: string;
  courseName: string;
  credits?: number | null;
  classroom?: string | null;
  slotDetails?: string | null;
}

export interface Faculty {
  id: number;
  username: string | null;
  name: string | null;
  email?: string | null;
  department?: string | null;
  title?: string | null;
  employeeCode?: string | null;
  assignedCourseCode?: string | null;
  assignedCourseName?: string | null;
  classroom?: string | null;
  assignedCourses: CourseSummary[];
  enrolledStudentsCount: number;
  atRiskStudentsCount: number;
}

export interface TargetScenario {
  scenarioName?: string;
  targetScore: number;
  expectedConvertedTotal?: number;
  projectedPercentage?: number;
  projectedRiskLevel?: string;
  rationale?: string;
}

export interface FutureTargetPlanner {
  courseCode: string;
  courseName: string;
  currentObtainedMarks?: number;
  currentEvaluatedMaxMarks?: number;
  currentMaxMarks?: number;
  currentConvertedMarks?: number;
  maxConvertedMarks?: number;
  totalSubjectMaxMarks?: number;
  currentPercentage?: number;
  pendingAssessmentName: string;
  pendingAssessmentMaxMarks?: number;
  pendingWeightage?: number;
  remainingAssessmentsCount?: number;
  remainingAssessmentsList?: string;
  minimumTarget?: TargetScenario;
  recommendedTarget?: TargetScenario | number;
  stretchTarget?: TargetScenario | number;
  minImprovementTarget?: number;
  riskImpactDescription?: string;
  targetType?: string;
  targetAffectsRisk?: string;
  verifiedWeightage?: boolean;
  projectionDisclaimer?: string;
}

/** Mirrors backend RiskEngineExplanationDto.SubjectRiskDto */
export interface SubjectRiskDetail {
  courseCode: string;
  courseName: string;
  attendancePercentage?: number | null;
  marksPercentage?: number | null;
  riskStatus: 'LOW' | 'MEDIUM' | 'HIGH';
  details: string;
}

/** Mirrors backend RiskEngineExplanationDto.ComponentExplanation */
export interface RiskComponentExplanation {
  componentName: string;
  actualValue?: number | null;
  thresholdValue?: number | null;
  pointsContributed: number;
  maxPossiblePoints?: number | null;
  calculationFormula: string;
  explanation: string;
  status: 'HEALTHY' | 'ATTENTION' | 'CRITICAL' | 'NO_DATA';
}

/** Mirrors backend RiskEngineExplanationDto (official rule-based score breakdown). */
export interface RiskEngineExplanation {
  overallScore: number;
  riskLevel: string;
  attendanceComponent: RiskComponentExplanation;
  marksComponent: RiskComponentExplanation;
  missingAssignmentsComponent: RiskComponentExplanation;
  subjectRisks: SubjectRiskDetail[];
  formulaDescription: string;
  formulaVersion?: string;
  recommendedIntervention: string;
  dataSourceProvenance: string;
  calculatedAt: string;
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
  /** null when the faculty member has no course assignment yet */
  courseCode: string | null;
  courseName: string | null;
  credits?: number | null;
  classroom?: string | null;
  slotDetails?: string | null;
  totalConductedSessions: number;
  courseAverageAttendance?: number | null;
  courseAverageMarks?: number | null;
  enrolledStudentsCount: number;
  highRiskCount: number;
  mediumRiskCount: number;
  lowRiskCount: number;
  students: StudentCoursePerformance[];
  assessments: AssessmentOverview[];
  activeInterventions: Intervention[];
  riskEngineSummary?: RiskEngineSummary;
}

// ---- Batch 2: live demo roster, cohort summary, intervention effectiveness ----

export interface DemoStudentAccount {
  username: string;
  regNo: string;
  name: string;
  role: 'STUDENT';
  cgpa?: number | null;
  risk?: RiskLevel | null;
  riskScore?: number | null;
  riskCalculatedAt?: string | null;
  attendance?: string | null;
  evaluatedAssessments: number;
  averageMarksPercentage?: number | null;
  subjects: number;
  provenance: string;
}

export interface DemoFacultyAccount {
  username: string;
  name: string;
  role: 'FACULTY';
  title?: string | null;
  courses: { code: string; name: string }[];
}

export interface DemoStatus {
  demoModeEnabled: boolean;
  demoMarker?: string;
  demoPassword?: string;
  students?: DemoStudentAccount[];
  facultyList?: DemoFacultyAccount[];
  disclaimer?: string;
}

export interface DashboardSummary {
  totalStudents: number;
  highRisk: number;
  mediumRisk: number;
  lowRisk: number;
  insufficientData: number;
  activeInterventions: number;
  completedInterventions: number;
  cohortAverageAttendance?: number | null;
  cohortAverageMarks?: number | null;
  recentHighRisk?: { id: number; name: string; factor: string; riskScore: number; intervention: string }[];
  cohortStrengths?: string[];
  cohortImprovementAreas?: string[];
}

export interface InterventionOutcome {
  interventionId: number;
  studentId: number | null;
  type: string | null;
  priority: string | null;
  status: string | null;
  startDate: string | null;
  baselineRisk: number | null;
  baselineLevel: string | null;
  outcomeRisk: number | null;
  outcomeLevel: string | null;
  riskChange: number | null;
  attendanceBefore: number | null;
  attendanceAfter: number | null;
  attendanceChange: number | null;
  followUpCount: number;
  evaluable: boolean;
  exclusionReason: string | null;
}

export interface EffectivenessGroup {
  group: string;
  interventions: number;
  evaluable: number;
  meanRiskChange: number | null;
  improved: number;
  unchanged: number;
  worsened: number;
  smallSample: boolean;
}

export interface EffectivenessReport {
  generatedAt: string;
  methodology: string;
  causalityNote: string;
  minGroupSize: number;
  totalInterventions: number;
  completedInterventions: number;
  completionRate: number | null;
  interventionsWithFollowUp: number;
  followUpCompletionRate: number | null;
  overall: EffectivenessGroup;
  byType: EffectivenessGroup[];
  byPriority: EffectivenessGroup[];
  byStatus: EffectivenessGroup[];
  excluded: Record<string, number>;
  marksChangeNote: string;
  subjectBreakdownNote: string;
  interventions: InterventionOutcome[];
}

export interface RiskHistory {
  studentId: number;
  snapshots: { calculatedAt: string; riskScore: number; riskLevel: RiskLevel }[];
  interventions: { id: number; type: string | null; status: string | null; startedAt: string | null }[];
  note: string;
}


export interface ImportIssue { section: string; index: number; severity: string; message: string; }
export interface ImportPreviewResponse {
  sourceLabel: string;
  live: boolean;
  acceptedAttendance: number;
  acceptedLedger: number;
  cgpa: number | null;
  rejected: number;
  warnings: number;
  issues: ImportIssue[];
}


export interface ImportConfirmResult {
  batchId: number;
  duplicate: boolean;
  acceptedAttendance: number;
  acceptedLedger: number;
  cgpa: number | null;
  riskScore: number | null;
  riskLevel: string | null;
}
