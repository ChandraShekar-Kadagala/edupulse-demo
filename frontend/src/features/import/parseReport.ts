/**
 * Student-side academic report parser for the EduPulse export tool.
 *
 * The STUDENT runs this on a report page they themselves opened while logged into the SRM AP portal
 * (their own session, their own CAPTCHA, their own password — none of which EduPulse ever sees).
 * It extracts the academic rows and strips sensitive personal fields BEFORE anything leaves the
 * student's machine, producing a clean JSON the student uploads to EduPulse.
 *
 * This file only EXTRACTS and MINIMIZES. It is deliberately NOT the authority on whether a value is
 * valid — the backend `AcademicImportValidator` is the single source of truth, exactly like the risk
 * engine. Raw values stay as strings so the server decides what to accept.
 */

export interface RawAttendanceRow {
  subjectCode: string;
  subjectName: string;
  classesConducted: string;
  present: string;
  absent: string;
  odMlTaken: string;
  presentPercentage: string;
  odMlPercentage: string;
  attendancePercentage: string;
}

export interface RawLedgerRow {
  semester: string;
  monthYear: string;
  subjectCode: string;
  subjectDescription: string;
  credit: string;
  grade: string;
  gradePoints: string;
  result: string;
  attempt: string;
}

export interface AcademicExport {
  /** Always a manual import. Never presented as a live/official connection. */
  sourceType: 'STUDENT_MANUAL_IMPORT';
  /** When this tool produced the export (ISO). Not the institution's update time. */
  exportedAt: string;
  /** Date the student says the report was generated; null if not provided. */
  reportDate: string | null;
  attendance: RawAttendanceRow[];
  ledger: RawLedgerRow[];
  cgpa: string | null;
  /** Allowlisted academic profile fields only (see PROFILE_ALLOWLIST). */
  profile: Record<string, string>;
}

/** Academic profile fields EduPulse keeps. Everything else (name, DOB, gender, photo) is dropped here. */
export const PROFILE_ALLOWLIST = ['registerNo', 'institution', 'semester', 'program', 'section', 'specialization'] as const;

function text(el: Element | null | undefined): string {
  return (el?.textContent ?? '').replace(/\s+/g, ' ').trim();
}

/** Subject-wise attendance: SRM `table#tblSubjectWiseAttendance`, data rows have 9 `<td>`. */
export function parseAttendance(doc: Document): RawAttendanceRow[] {
  const table = doc.querySelector('#tblSubjectWiseAttendance, #attendanceTable');
  if (!table) return [];
  const rows: RawAttendanceRow[] = [];
  table.querySelectorAll('tr').forEach((tr) => {
    const td = tr.querySelectorAll('td');
    if (td.length !== 9) return; // header rows use <th>; skip anything that isn't a 9-column data row
    rows.push({
      subjectCode: text(td[0]),
      subjectName: text(td[1]),
      classesConducted: text(td[2]),
      present: text(td[3]),
      absent: text(td[4]),
      odMlTaken: text(td[5]),
      presentPercentage: text(td[6]),
      odMlPercentage: text(td[7]),
      attendancePercentage: text(td[8]),
    });
  });
  return rows;
}

/** Exam ledger / semester results: SRM `div.subTable table`, data rows have >= 9 `<td>`. */
export function parseExamLedger(doc: Document): RawLedgerRow[] {
  const rows: RawLedgerRow[] = [];
  doc.querySelectorAll('div.subTable table tr, #examLedger tr').forEach((tr) => {
    const td = tr.querySelectorAll('td');
    if (td.length < 9) return;
    rows.push({
      semester: text(td[0]),
      monthYear: text(td[1]),
      subjectCode: text(td[2]),
      subjectDescription: text(td[3]),
      credit: text(td[4]),
      grade: text(td[5]),
      gradePoints: text(td[6]),
      result: text(td[7]),
      attempt: text(td[8]),
    });
  });
  return rows;
}

/** CGPA from the exam ledger, e.g. "Cumulative Grade Point Average (CGPA) : 8.85". Null if absent. */
export function parseCgpa(doc: Document): string | null {
  const whole = text(doc.body);
  // Allow text between the label and the value, e.g. "(CGPA) : 8.85".
  const m = whole.match(/CGPA[^0-9]{0,8}([0-9]+(?:\.[0-9]+)?)/i);
  return m ? m[1] : null;
}

/**
 * Profile: keep only allowlisted academic fields. Student name, D.O.B./Gender and the photo are never
 * read into the output, even when present in the page.
 */
export function stripProfile(doc: Document): Record<string, string> {
  const out: Record<string, string> = {};
  doc.querySelectorAll('table tr').forEach((tr) => {
    const td = tr.querySelectorAll('td');
    if (td.length !== 3) return; // SRM profile rows are label / separator / value
    const key = text(td[0]);
    const val = text(td[2]);
    if (!val) return;
    if (/Register\s*No/i.test(key)) out.registerNo = val;
    else if (/Institution/i.test(key)) out.institution = val;
    else if (/^Semester$/i.test(key)) out.semester = val;
    else if (/Program\s*\/\s*Section/i.test(key)) {
      const [program, section] = val.split('/').map((v) => v.trim());
      if (program) out.program = program;
      if (section) out.section = section.replace(/['"]+/g, '');
    } else if (/Specialization/i.test(key)) out.specialization = val;
    // Deliberately NOT handled: Student Name, D.O.B. / Gender, and the profile photo.
  });
  // Belt and braces: never let a disallowed key through.
  for (const k of Object.keys(out)) {
    if (!(PROFILE_ALLOWLIST as readonly string[]).includes(k)) delete out[k];
  }
  return out;
}

/** Parses one saved report page into a clean, minimized export. Each section is best-effort. */
export function buildExport(html: string, options: { reportDate?: string | null } = {}): AcademicExport {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const safe = <T>(fn: () => T, fallback: T): T => {
    try {
      return fn();
    } catch {
      return fallback;
    }
  };
  return {
    sourceType: 'STUDENT_MANUAL_IMPORT',
    exportedAt: new Date().toISOString(),
    reportDate: options.reportDate ?? null,
    attendance: safe(() => parseAttendance(doc), []),
    ledger: safe(() => parseExamLedger(doc), []),
    cgpa: safe(() => parseCgpa(doc), null),
    profile: safe(() => stripProfile(doc), {}),
  };
}

/** True when nothing academic was found — the UI shows a "couldn't read this page" state instead of a false success. */
export function isEmptyExport(e: AcademicExport): boolean {
  return e.attendance.length === 0 && e.ledger.length === 0 && e.cgpa === null && Object.keys(e.profile).length === 0;
}
