import { describe, it, expect, beforeEach } from 'vitest';

describe('Theme and Cohort Showcase Tests', () => {
  const storage: Record<string, string> = {};
  const mockLocalStorage = {
    getItem: (key: string) => storage[key] ?? null,
    setItem: (key: string, val: string) => { storage[key] = val; },
    clear: () => { Object.keys(storage).forEach(k => delete storage[k]); }
  };

  const classList = new Set<string>();
  const mockDocument = {
    documentElement: {
      classList: {
        add: (cls: string) => classList.add(cls),
        remove: (cls: string) => classList.delete(cls),
        contains: (cls: string) => classList.has(cls),
      }
    }
  };

  beforeEach(() => {
    mockLocalStorage.clear();
    classList.clear();
  });

  describe('Theme Management and Persistence', () => {
    it('applies dark theme class to document root and stores in localStorage', () => {
      const applyTheme = (theme: 'light' | 'dark') => {
        if (theme === 'dark') {
          mockDocument.documentElement.classList.add('dark');
        } else {
          mockDocument.documentElement.classList.remove('dark');
        }
        mockLocalStorage.setItem('theme', theme);
      };

      applyTheme('dark');
      expect(mockDocument.documentElement.classList.contains('dark')).toBe(true);
      expect(mockLocalStorage.getItem('theme')).toBe('dark');

      applyTheme('light');
      expect(mockDocument.documentElement.classList.contains('dark')).toBe(false);
      expect(mockLocalStorage.getItem('theme')).toBe('light');
    });

    it('recovers stored theme from localStorage', () => {
      mockLocalStorage.setItem('theme', 'dark');
      const stored = mockLocalStorage.getItem('theme');
      expect(stored).toBe('dark');
    });
  });

  describe('4-Student Showcase Cohort Integrity', () => {
    const showcaseCohort = [
      { id: 1, name: 'Aarav Sharma', studentNumber: 'STU-1001', attendancePercentage: 95.0, averageMarksPercentage: 92.0, currentCgpa: 9.4, riskLevel: 'LOW', riskScore: 0.0 },
      { id: 2, name: 'Bhavna Patel', studentNumber: 'STU-1002', attendancePercentage: 80.0, averageMarksPercentage: 79.0, currentCgpa: 7.9, riskLevel: 'LOW', riskScore: 5.0 },
      { id: 3, name: 'Chirag Verma', studentNumber: 'STU-1003', attendancePercentage: 65.0, averageMarksPercentage: 44.5, currentCgpa: 6.2, riskLevel: 'MEDIUM', riskScore: 40.5 },
      { id: 4, name: 'Arun Kumar', studentNumber: 'STU-1004', attendancePercentage: 30.0, averageMarksPercentage: 24.5, currentCgpa: 5.1, riskLevel: 'HIGH', riskScore: 75.5 }
    ];

    it('contains all 4 distinct risk categories across the cohort', () => {
      const lowRisk = showcaseCohort.filter(s => s.riskLevel === 'LOW');
      const medRisk = showcaseCohort.filter(s => s.riskLevel === 'MEDIUM');
      const highRisk = showcaseCohort.filter(s => s.riskLevel === 'HIGH');

      expect(lowRisk.length).toBe(2);
      expect(medRisk.length).toBe(1);
      expect(highRisk.length).toBe(1);
      expect(showcaseCohort.length).toBe(4);
    });

    it('safely extracts student name regardless of flat or nested structure', () => {
      const flatStudent = { id: 1, name: 'Aarav Sharma' };
      const nestedStudent = { id: 2, user: { name: 'Bhavna Patel' } };
      const emptyStudent = { id: 3 };

      const extractName = (s: any) => s.name || s.user?.name || 'Unknown';

      expect(extractName(flatStudent)).toBe('Aarav Sharma');
      expect(extractName(nestedStudent)).toBe('Bhavna Patel');
      expect(extractName(emptyStudent)).toBe('Unknown');
    });

    it('safely extracts intervention student name regardless of flat or nested structure', () => {
      const flatIntervention = { id: 10, studentName: 'Arun Kumar' };
      const nestedIntervention = { id: 11, student: { user: { name: 'Chirag Verma' } } };
      const emptyIntervention = { id: 12 };

      const extractStudentName = (inv: any) => inv.studentName || inv.student?.user?.name || 'Student';

      expect(extractStudentName(flatIntervention)).toBe('Arun Kumar');
      expect(extractStudentName(nestedIntervention)).toBe('Chirag Verma');
      expect(extractStudentName(emptyIntervention)).toBe('Student');
    });
  });
});
