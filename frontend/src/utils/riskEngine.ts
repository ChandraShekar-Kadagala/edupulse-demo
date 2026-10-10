export enum AttendanceStatus {
  PRESENT = 'PRESENT',
  ABSENT = 'ABSENT'
}

export enum RiskLevel {
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW'
}

export interface Attendance {
  status: AttendanceStatus;
}

export interface Assessment {
  maxMarks: number;
}

export interface Mark {
  marksObtained: number;
  assessment: Assessment;
}

export interface Assignment {
  dueDate: string; // ISO 8601 string
}

export interface AssignmentSubmission {
  submittedAt: string | null; // ISO 8601 string, null if missing
  assignment: Assignment;
}

export interface RiskFactor {
  factorName: string;
  impactLevel: string;
  description: string;
}

export interface RiskAssessmentResult {
  riskScore: number;
  riskLevel: RiskLevel;
  riskFactors: RiskFactor[];
}

export function calculateRisk(
  attendances: Attendance[],
  marks: Mark[],
  submissions: AssignmentSubmission[]
): RiskAssessmentResult | null {
  if (attendances.length === 0 && marks.length === 0 && submissions.length === 0) {
    return null; // INSUFFICIENT_DATA
  }

  let totalRiskScore = 0.0;
  const riskFactors: RiskFactor[] = [];

  // 1. Attendance Risk
  const totalClasses = attendances.length;
  if (totalClasses > 0) {
    const presentClasses = attendances.filter(a => a.status === AttendanceStatus.PRESENT).length;
    const attendancePercentage = (presentClasses / totalClasses) * 100;
    if (attendancePercentage < 75.0) {
      const risk = (75.0 - attendancePercentage) * 1.5;
      totalRiskScore += risk;
      riskFactors.push({
        factorName: 'Low Attendance',
        impactLevel: 'MEDIUM',
        description: `Attendance is ${attendancePercentage.toFixed(1)}%`
      });
    }
  }

  // 2. Marks Risk
  if (marks.length > 0) {
    // Validate marks
    const validMarks = marks.filter(m => m.assessment && m.assessment.maxMarks > 0);
    if (validMarks.length > 0) {
      let sumPercentages = 0.0;
      for (const m of validMarks) {
        sumPercentages += (m.marksObtained / m.assessment.maxMarks) * 100;
      }
      const avgMarks = sumPercentages / validMarks.length;
      if (avgMarks < 50.0) {
        const risk = (50.0 - avgMarks) * 1.0;
        totalRiskScore += risk;
        riskFactors.push({
          factorName: 'Low Grades',
          impactLevel: 'HIGH',
          description: `Average marks are ${avgMarks.toFixed(1)}%`
        });
      }
    }
  }

  // 3. Missing Assignments Risk
  const lateOrMissing = submissions.filter(s => {
    if (!s.submittedAt) return true;
    return new Date(s.submittedAt) > new Date(s.assignment.dueDate);
  }).length;
  
  if (lateOrMissing > 0) {
    const risk = lateOrMissing * 5.0;
    totalRiskScore += risk;
    riskFactors.push({
      factorName: 'Missing Assignments',
      impactLevel: 'MEDIUM',
      description: `${lateOrMissing} assignments missing or late`
    });
  }

  totalRiskScore = Math.min(totalRiskScore, 100.0);
  
  let riskLevel = RiskLevel.LOW;
  if (totalRiskScore >= 65) {
    riskLevel = RiskLevel.HIGH;
  } else if (totalRiskScore >= 35) {
    riskLevel = RiskLevel.MEDIUM;
  }

  return {
    riskScore: totalRiskScore,
    riskLevel,
    riskFactors
  };
}
