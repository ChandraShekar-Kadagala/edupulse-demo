// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { buildExport, isEmptyExport, parseAttendance, parseCgpa, parseExamLedger, parseInternals, stripProfile } from './parseReport';

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

  it('reads the real SRM table with a nested 2-row header, by column position', () => {
    const REAL = `
      <table>
        <tr><th rowspan="2">Subject Code</th><th rowspan="2">Subject Description</th><th rowspan="2">Classes Conducted</th>
            <th colspan="2">Attendance Entered (Slots)</th><th rowspan="2">OD/ML Taken</th><th rowspan="2">Present %</th>
            <th rowspan="2">OD ML % approved</th><th rowspan="2">Attendance %</th></tr>
        <tr><th>Present(P)</th><th>Absent(A)</th></tr>
        <tr><td>CSE 204</td><td>DESIGN AND ANALYSIS OF ALGORITHMS</td><td>61</td><td>57</td><td>4</td><td>0</td><td>93.44</td><td>0.00</td><td>93.44</td></tr>
        <tr><td colspan="9">For any discrepancy contact the course teacher.</td></tr>
      </table>`;
    const rows = parseAttendance(doc(REAL));
    expect(rows).toHaveLength(1);
    expect(rows[0].subjectCode).toBe('CSE 204');
    expect(rows[0].classesConducted).toBe('61');
    expect(rows[0].present).toBe('57');
    expect(rows[0].absent).toBe('4');
    expect(rows[0].attendancePercentage).toBe('93.44');
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

describe('internal marks (expandable subject totals + CLA/Mid breakdown)', () => {
  const INTERNALS = `
    <table class="table table-striped table-bordered">
      <tr><th>Subject Code</th><th>Subject Description</th><th>Marks Obtained</th><th>Max.Marks</th></tr>
      <tr><td>CSE 203</td><td>DISCRETE MATHEMATICS</td><td>38.25</td><td>50</td></tr>
      <tr><td colspan="4">
        <table>
          <tr><th>Name</th><th>Mark Secured(Conducted)</th><th>Mark Secured(Converted)</th></tr>
          <tr><td>Mid Semester Exam I</td><td>18.25 / 20</td><td>18.25 / 20</td></tr>
          <tr><td>CLA 1</td><td>10.00 / 10</td><td>10.00 / 10</td></tr>
          <tr><td>CLA 3</td><td></td><td></td></tr>
        </table>
      </td></tr>
      <tr><td>SEC 203</td><td>APTITUDE FOR ENGINEERS - III</td><td>8.40</td><td>40</td></tr>
    </table>`;

  it('reads each subject total and its components, with per-subject max marks', () => {
    const subs = parseInternals(doc(INTERNALS));
    expect(subs).toHaveLength(2);
    expect(subs[0].subjectCode).toBe('CSE 203');
    expect(subs[0].marksObtained).toBe('38.25');
    expect(subs[0].maxMarks).toBe('50');
    expect(subs[0].components).toHaveLength(3);
    expect(subs[0].components[0].name).toBe('Mid Semester Exam I');
    expect(subs[0].components[0].conductedObtained).toBe('18.25');
    expect(subs[0].components[0].conductedMax).toBe('20');
    expect(subs[0].components[2].name).toBe('CLA 3'); // pending: empty marks kept
    expect(subs[0].components[2].conductedObtained).toBe('');
    // Max marks are NOT assumed to be 50 — SEC 203 is out of 40.
    expect(subs[1].subjectCode).toBe('SEC 203');
    expect(subs[1].maxMarks).toBe('40');
  });

  it('returns nothing when there is no internal-marks table', () => {
    expect(parseInternals(doc('<table><tr><th>Other</th></tr><tr><td>x</td></tr></table>'))).toEqual([]);
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
