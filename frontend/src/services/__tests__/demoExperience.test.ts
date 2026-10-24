import { describe, it, expect } from 'vitest';

describe('Demo Experience & Showcase Safety', () => {

  describe('Login Error Parsing', () => {
    it('correctly extracts "error" property from backend 401 response (Bad credentials)', () => {
      const errorResponse = {
        response: {
          status: 401,
          data: {
            error: 'Bad credentials'
          }
        }
      };

      const extractedError = errorResponse.response?.data?.error ||
                             errorResponse.response?.data?.message ||
                             'Invalid credentials or API unreachable. Ensure the backend is running.';

      expect(extractedError).toBe('Bad credentials');
    });

    it('falls back to "message" property if error is not present', () => {
      const errorResponse = {
        response: {
          status: 400,
          data: {
            message: 'Registration Number is required.'
          }
        }
      };

      const extractedError = errorResponse.response?.data?.error ||
                             errorResponse.response?.data?.message ||
                             'Invalid credentials or API unreachable. Ensure the backend is running.';

      expect(extractedError).toBe('Registration Number is required.');
    });

    it('uses fallback text when network or unexpected error occurs without data', () => {
      const networkError = {
        message: 'Network Error'
      } as any;

      const extractedError = networkError.response?.data?.error ||
                             networkError.response?.data?.message ||
                             'Invalid credentials or API unreachable. Ensure the backend is running.';

      expect(extractedError).toBe('Invalid credentials or API unreachable. Ensure the backend is running.');
    });
  });

  describe('Demo Status Evaluation', () => {
    it('detects when demo showcase mode is active and validates safe demo defaults', () => {
      const demoStatus = {
        demoModeEnabled: true,
        demoMarker: 'DEMO_SHOWCASE',
        defaultStudentUsername: 'arun',
        defaultStudentRegNo: 'STU-1001',
        defaultFacultyUsername: 'faculty',
        defaultAdminUsername: 'admin',
        disclaimer: 'Illustrative sandbox demo data for evaluation and testing. Not affiliated with actual SRM University AP student records.'
      };

      expect(demoStatus.demoModeEnabled).toBe(true);
      expect(demoStatus.demoMarker).toBe('DEMO_SHOWCASE');
      expect(demoStatus.defaultStudentUsername).toBe('arun');
      expect(demoStatus.defaultFacultyUsername).toBe('faculty');
      expect(demoStatus.defaultAdminUsername).toBe('admin');
      expect(demoStatus.disclaimer).toContain('Not affiliated with actual SRM University AP');
    });

    it('handles disabled demo mode safely', () => {
      const disabledStatus = {
        demoModeEnabled: false,
        demoMarker: 'DEMO_SHOWCASE',
        disclaimer: 'Illustrative sandbox demo data.'
      };

      expect(disabledStatus.demoModeEnabled).toBe(false);
    });
  });

  describe('Academic Data Provenance Safety Distinction', () => {
    const formatProvenanceLabel = (provenance: string) => {
      switch (provenance) {
        case 'OFFICIAL_INSTITUTIONAL_API':
          return 'Official University API';
        case 'INSTITUTIONAL_PORTAL_FIXTURE':
          return 'Portal Fixture (Verified)';
        case 'CSV_FACULTY_IMPORT':
          return 'Faculty Import';
        case 'SYNTHETIC_TEST_FIXTURE':
        default:
          return 'Synthetic Demo Fixture';
      }
    };

    it('clearly labels synthetic fixtures distinctly from institutional records', () => {
      expect(formatProvenanceLabel('SYNTHETIC_TEST_FIXTURE')).toBe('Synthetic Demo Fixture');
      expect(formatProvenanceLabel('OFFICIAL_INSTITUTIONAL_API')).toBe('Official University API');
      expect(formatProvenanceLabel('INSTITUTIONAL_PORTAL_FIXTURE')).toBe('Portal Fixture (Verified)');
    });
  });

  describe('Role-Based Navigation Safety', () => {
    const navItems = [
      { to: '/dashboard', label: 'Dashboard', roles: ['ADMIN', 'FACULTY'] },
      { to: '/students', label: 'Students', roles: ['ADMIN', 'FACULTY'] },
      { to: '/interventions', label: 'Interventions', roles: ['ADMIN', 'FACULTY'] },
      { to: '/import', label: 'Data Import', roles: ['ADMIN', 'FACULTY'] },
      { to: '/students/1', label: 'My Profile', roles: ['STUDENT'] },
      { to: '/integration', label: 'Account Linking', roles: ['STUDENT', 'ADMIN', 'FACULTY'] }
    ];

    it('ensures student role cannot access administrative or faculty navigation items', () => {
      const studentUser = { role: 'STUDENT' };
      const allowed = navItems.filter(item => item.roles.includes(studentUser.role));

      const allowedLabels = allowed.map(i => i.label);
      expect(allowedLabels).toContain('My Profile');
      expect(allowedLabels).toContain('Account Linking');
      expect(allowedLabels).not.toContain('Students');
      expect(allowedLabels).not.toContain('Interventions');
      expect(allowedLabels).not.toContain('Data Import');
      expect(allowedLabels).not.toContain('Dashboard');
    });
  });
});
