// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { buildExport, isEmptyExport, parseAttendance, parseCgpa, parseExamLedger, stripProfile } from './parseReport';

// All HTML below is synthetic (SYN* codes, placeholder names). No real student or portal data.

function doc(html: string): Document {
  return new DOMParser().parseFromString(html, 'text/html');
}

const ATTENDANCE_HTML = `
<table id="tblSubjectWiseAttendance">
  <tr><th>Subject Code</th><th>Subject Name</th><th>Classes Conducted</th><th>Present</th><th>Absent</th><th>OD/ML</th><th>Present %</th><th>OD/ML %</th><th>Attendance %</th></tr>
  <tr><td>SYN 101</td><td>Synthetic One</td><td>40</td><td>34</td><td>6</td><td>0</td><td>85.00</td><td>0.00</td><td>85.00</td></tr>
  <tr><td>SYN 102</td><td>Synthetic Two</td><td>30</td><td>15</td><td>15</td><td>2</td><td>50.00</td><td>6.67</td><td>56.67</td></tr>
</table>`;

const LEDGER_HTML = `
<div class="subTable"><table>
  <tr><th>Semester</th><th>Month/Year</th><th>Subject Code</th><th>Subject Description</th><th>Credit</th><th>Grade</th><th>Grade Points</th><th>Result</th><th>Attempt</th></tr>
  <tr><td>Semester 1</td><td>DEC-2099</td><td>SYN 101</td><td>Synthetic One</td><td>4.0</td><td>A+</td><td>10.0</td><td>PASS</td><td>1</td></tr>
</table></div>
<div>Cumulative Grade Point Average (CGPA) : 8.85</div>`;

// A different real-world layout: no table id, columns in a different order, no explicit Absent column.
const ALT_ATTENDANCE_HTML = `
<table class="report">
  <tr><th>Attendance %</th><th>Present</th><th>Total Conducted</th><th>Course Code</th><th>Course Title</th></tr>
  <tr><td>66.67</td><td>20</td><td>30</td><td>SYN 201</td><td>Alt Course</td></tr>
  <tr><td colspan="5">Grand Total</td></tr>
</table>`;

const PROFILE_HTML = `
<table>
  <tr><td>Student Name</td><td>:</td><td>Synthetic Person</td></tr>
  <tr><td>Register No</td><td>:</td><td>SYN-0001</td></tr>
  <tr><td>Program / Section</td><td>:</td><td>B.Tech CSE / 'Z'</td></tr>
  <tr><td>Specialization</td><td>:</td><td>AI</td></tr>
  <tr><td>D.O.B. / Gender</td><td>:</td><td>01-01-2000 / X</td></tr>
</table>`;

describe('attendance extraction', () => {
  it('reads 9-column data rows and leaves values as strings', () => {
    const rows = parseAttendance(doc(ATTENDANCE_HTML));
    expect(rows).toHaveLength(2);
    expect(rows[0].subjectCode).toBe('SYN 101');
    expect(rows[0].present).toBe('34');
    expect(rows[0].attendancePercentage).toBe('85.00');
  });

  it('returns nothing when the attendance table is absent', () => {
    expect(parseAttendance(doc('<table id="other"><tr><th>Name</th></tr><tr><td>x</td></tr></table>'))).toEqual([]);
  });

  it('reads a different layout (no id, reordered columns, derived absent) and skips total rows', () => {
    const rows = parseAttendance(doc(ALT_ATTENDANCE_HTML));
    expect(rows).toHaveLength(1);
    expect(rows[0].subjectCode).toBe('SYN 201');
    expect(rows[0].present).toBe('20');
    expect(rows[0].classesConducted).toBe('30');
    expect(rows[0].absent).toBe('10'); // derived: conducted - present
    expect(rows[0].attendancePercentage).toBe('66.67');
  });
});

describe('exam ledger extraction', () => {
  it('reads ledger rows and the CGPA', () => {
    const d = doc(LEDGER_HTML);
    const rows = parseExamLedger(d);
    expect(rows).toHaveLength(1);
    expect(rows[0].subjectCode).toBe('SYN 101');
    expect(rows[0].grade).toBe('A+');
    expect(parseCgpa(d)).toBe('8.85');
  });

  it('CGPA is null when the page does not contain it', () => {
    expect(parseCgpa(doc('<div>no grades here</div>'))).toBeNull();
  });
});

describe('profile minimization (privacy)', () => {
  it('keeps only allowlisted academic fields', () => {
    const p = stripProfile(doc(PROFILE_HTML));
    expect(p).toEqual({ registerNo: 'SYN-0001', program: 'B.Tech CSE', section: 'Z', specialization: 'AI' });
  });

  it('never emits name, date of birth, gender or photo', () => {
    const serialized = JSON.stringify(buildExport(PROFILE_HTML));
    expect(serialized).not.toContain('Synthetic Person');
    expect(serialized).not.toContain('01-01-2000');
    expect(serialized.toLowerCase()).not.toContain('gender');
  });
});

describe('buildExport', () => {
  it('combines sections and labels the source as a manual import, never live', () => {
    const e = buildExport(ATTENDANCE_HTML + LEDGER_HTML + PROFILE_HTML, { reportDate: '2099-01-15' });
    expect(e.sourceType).toBe('STUDENT_MANUAL_IMPORT');
    expect(e.reportDate).toBe('2099-01-15');
    expect(e.attendance).toHaveLength(2);
    expect(e.ledger).toHaveLength(1);
    expect(e.cgpa).toBe('8.85');
    expect(e.profile.registerNo).toBe('SYN-0001');
    expect(JSON.stringify(e).toLowerCase()).not.toContain('"live"');
  });

  it('flags an unreadable page instead of reporting false success', () => {
    const e = buildExport('<html><body><h1>Login page</h1></body></html>');
    expect(isEmptyExport(e)).toBe(true);
  });

  it('a malformed section does not break the others', () => {
    const e = buildExport('<table id="tblSubjectWiseAttendance"><tr><td>SYN 101</td><td>x</td><td>40</td><td>34</td><td>6</td><td>0</td><td>85</td><td>0</td><td>85</td></tr></table><div class="subTable"><table><tr><td>broken</td></tr></table></div>');
    expect(e.attendance).toHaveLength(1);
    expect(e.ledger).toEqual([]);
  });
});
