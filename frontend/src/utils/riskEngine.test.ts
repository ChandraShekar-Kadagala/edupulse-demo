import { describe, it, expect } from 'vitest';
import { calculateRisk, AttendanceStatus, RiskLevel } from './riskEngine';

describe('RuleBasedRiskEngine TypeScript Parity', () => {
  it('handles empty inputs (INSUFFICIENT_DATA)', () => {
    const result = calculateRisk([], [], []);
    expect(result).toBeNull();
  });

  it('calculates low risk correctly', () => {
    const result = calculateRisk(
      [{ status: AttendanceStatus.PRESENT }, { status: AttendanceStatus.PRESENT }, { status: AttendanceStatus.PRESENT }, { status: AttendanceStatus.PRESENT }], // 100%
      [{ marksObtained: 90, assessment: { maxMarks: 100 } }],
      []
    );
    expect(result?.riskScore).toBe(0);
    expect(result?.riskLevel).toBe(RiskLevel.LOW);
    expect(result?.riskFactors.length).toBe(0);
  });

  it('calculates medium risk correctly (attendance only)', () => {
    // 2 present, 2 absent = 50% attendance
    // Risk = (75 - 50) * 1.5 = 37.5
    const result = calculateRisk(
      [
        { status: AttendanceStatus.PRESENT }, { status: AttendanceStatus.PRESENT },
        { status: AttendanceStatus.ABSENT }, { status: AttendanceStatus.ABSENT }
      ],
      [{ marksObtained: 80, assessment: { maxMarks: 100 } }],
      []
    );
    expect(result?.riskScore).toBe(37.5);
    expect(result?.riskLevel).toBe(RiskLevel.MEDIUM);
    expect(result?.riskFactors[0].factorName).toBe('Low Attendance');
    expect(result?.riskFactors[0].impactLevel).toBe('MEDIUM');
  });

  it('calculates high risk correctly (marks + missing assignments)', () => {
    // 100% attendance, but 40% marks and 2 missing assignments
    // Marks risk: (50 - 40) * 1.0 = 10.0
    // Missing risk: 2 * 5.0 = 10.0
    // Total risk: 20 -> Wait, 20 is LOW. Let's make it HIGH (>65).
    // Marks: 0% -> (50-0)*1 = 50.0
    // Missing: 4 -> 20.0
    // Total: 70 -> HIGH
    const result = calculateRisk(
      [{ status: AttendanceStatus.PRESENT }],
      [{ marksObtained: 0, assessment: { maxMarks: 100 } }],
      [
        { submittedAt: null, assignment: { dueDate: '2025-01-01T00:00:00Z' } },
        { submittedAt: null, assignment: { dueDate: '2025-01-01T00:00:00Z' } },
        { submittedAt: null, assignment: { dueDate: '2025-01-01T00:00:00Z' } },
        { submittedAt: null, assignment: { dueDate: '2025-01-01T00:00:00Z' } }
      ]
    );
    expect(result?.riskScore).toBe(70);
    expect(result?.riskLevel).toBe(RiskLevel.HIGH);
    expect(result?.riskFactors.length).toBe(2);
  });
});
