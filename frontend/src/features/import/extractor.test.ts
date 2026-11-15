// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
// The extension's live-DOM extractor (shared logic with parseReport.ts). Synthetic data only.
import { extractAcademicExport } from '../../../../extension/extractor.js';

function doc(html: string): Document {
  return new DOMParser().parseFromString(html, 'text/html');
}

const PAGE = `
<table id="tblSubjectWiseAttendance">
  <tr><th>Subject Code</th><th>Subject Name</th><th>Classes Conducted</th><th>Present</th><th>Absent</th><th>OD/ML</th><th>Present %</th><th>OD/ML %</th><th>Attendance %</th></tr>
  <tr><td>SYN 101</td><td>Synthetic One</td><td>40</td><td>20</td><td>20</td><td>0</td><td>50.00</td><td>0</td><td>50.00</td></tr>
</table>
<table>
  <tr><td>Student Name</td><td>:</td><td>Synthetic Person</td></tr>
  <tr><td>Register No</td><td>:</td><td>SYN-0001</td></tr>
  <tr><td>D.O.B. / Gender</td><td>:</td><td>01-01-2000 / X</td></tr>
</table>
<div>Cumulative Grade Point Average (CGPA) : 7.5</div>`;

describe('extension extractor', () => {
  it('extracts academic rows and labels the export as a manual import', () => {
    const e = extractAcademicExport(doc(PAGE));
    expect(e.sourceType).toBe('STUDENT_MANUAL_IMPORT');
    expect(e.attendance).toHaveLength(1);
    expect(e.attendance[0].subjectCode).toBe('SYN 101');
    expect(e.attendance[0].present).toBe('20');
    expect(e.cgpa).toBe('7.5');
    expect(e.profile.registerNo).toBe('SYN-0001');
  });

  it('removes name, date of birth and gender', () => {
    const s = JSON.stringify(extractAcademicExport(doc(PAGE)));
    expect(s).not.toContain('Synthetic Person');
    expect(s).not.toContain('01-01-2000');
    expect(s.toLowerCase()).not.toContain('gender');
  });

  it('returns an empty export (not an error) for a page with no report', () => {
    const e = extractAcademicExport(doc('<h1>Login</h1>'));
    expect(e.attendance).toEqual([]);
    expect(e.ledger).toEqual([]);
    expect(e.cgpa).toBeNull();
  });
});
