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

const COURSE_CODE_RE = /\b[A-Z]{2,5}\s?\d{3}[A-Z]?\b/;

/** Lower-cased header labels of a table (the first row that has <th>, else the first row). */
function headerLabels(table: Element): string[] {
  const headerRow = table.querySelector('tr:has(th)') ?? table.querySelector('tr');
  if (!headerRow) return [];
  return Array.from(headerRow.querySelectorAll('th, td')).map((c) => text(c).toLowerCase());
}

/** Index of the first header matching any pattern, or -1. */
function col(headers: string[], ...patterns: RegExp[]): number {
  return headers.findIndex((h) => patterns.some((p) => p.test(h)));
}

/** Data rows of a table: <tr> that contain <td> (skips header-only rows). */
function dataRows(table: Element): Element[] {
  return Array.from(table.querySelectorAll('tr')).filter((tr) => tr.querySelector('td'));
}

/**
 * Subject-wise attendance. Finds the attendance table by its HEADERS (not a fixed id or column count),
 * so it works across portal layouts, and maps the columns it needs by header position. Missing columns
 * are derived where safe (absent = conducted - present) or left blank for the server to judge.
 */
export function parseAttendance(doc: Document): RawAttendanceRow[] {
  for (const table of Array.from(doc.querySelectorAll('table'))) {
    const h = headerLabels(table);
    const codeIdx = col(h, /subject\s*code/, /course\s*code/, /sub\s*code/, /^code$/);
    const presentIdx = col(h, /present/, /attended/);
    if (codeIdx < 0 || presentIdx < 0) continue; // not an attendance table

    const conductedIdx = col(h, /conducted/, /total.*(class|hour|conducted)/, /^total$/);
    const absentIdx = col(h, /absent/);
    const odIdx = col(h, /od.?ml/, /od\s*\/\s*ml/);
    const nameIdx = col(h, /subject\s*name/, /course\s*(title|name)/, /title/);
    const presentPctIdx = col(h, /present\s*%/, /present percentage/);
    const attPctIdx = col(h, /attendance\s*%/, /total\s*%/, /percentage/);

    const cell = (tr: Element, i: number) => (i >= 0 ? text(tr.querySelectorAll('td')[i]) : '');
    const rows: RawAttendanceRow[] = [];
    for (const tr of dataRows(table)) {
      const code = cell(tr, codeIdx);
      if (!COURSE_CODE_RE.test(code)) continue; // skip totals / stray rows
      const conducted = cell(tr, conductedIdx);
      const present = cell(tr, presentIdx);
      let absent = cell(tr, absentIdx);
      if (!absent && conducted && present) {
        const d = Number(conducted) - Number(present);
        if (Number.isFinite(d)) absent = String(d);
      }
      rows.push({
        subjectCode: code,
        subjectName: cell(tr, nameIdx),
        classesConducted: conducted,
        present,
        absent,
        odMlTaken: cell(tr, odIdx),
        presentPercentage: cell(tr, presentPctIdx),
        odMlPercentage: '',
        attendancePercentage: cell(tr, attPctIdx),
      });
    }
    if (rows.length > 0) return rows;
  }
  // Fallback for the known SRM table with no usable header row: fixed 9-column order.
  const known = doc.querySelector('#tblSubjectWiseAttendance, #attendanceTable');
  if (known) {
    const rows: RawAttendanceRow[] = [];
    for (const tr of dataRows(known)) {
      const td = tr.querySelectorAll('td');
      if (td.length !== 9 || !COURSE_CODE_RE.test(text(td[0]))) continue;
      rows.push({
        subjectCode: text(td[0]), subjectName: text(td[1]), classesConducted: text(td[2]),
        present: text(td[3]), absent: text(td[4]), odMlTaken: text(td[5]),
        presentPercentage: text(td[6]), odMlPercentage: text(td[7]), attendancePercentage: text(td[8]),
      });
    }
    if (rows.length > 0) return rows;
  }
  return [];
}

/** Exam ledger / semester results. Found by headers (grade + credit/semester), columns mapped by header. */
export function parseExamLedger(doc: Document): RawLedgerRow[] {
  for (const table of Array.from(doc.querySelectorAll('table'))) {
    const h = headerLabels(table);
    const codeIdx = col(h, /subject\s*code/, /course\s*code/, /^code$/);
    const gradeIdx = col(h, /^grade$/, /grade$/);
    if (codeIdx < 0 || gradeIdx < 0) continue;

    const semIdx = col(h, /sem/);
    const myIdx = col(h, /month.*year/, /month\s*\/\s*year/);
    const descIdx = col(h, /description/, /subject\s*(name|title)/, /title/);
    const creditIdx = col(h, /credit/);
    const gpIdx = col(h, /grade\s*point/, /^gp$/);
    const resultIdx = col(h, /result/, /status/);
    const attemptIdx = col(h, /attempt/);

    const cell = (tr: Element, i: number) => (i >= 0 ? text(tr.querySelectorAll('td')[i]) : '');
    const rows: RawLedgerRow[] = [];
    for (const tr of dataRows(table)) {
      const code = cell(tr, codeIdx);
      if (!COURSE_CODE_RE.test(code)) continue;
      rows.push({
        semester: cell(tr, semIdx),
        monthYear: cell(tr, myIdx),
        subjectCode: code,
        subjectDescription: cell(tr, descIdx),
        credit: cell(tr, creditIdx),
        grade: cell(tr, gradeIdx),
        gradePoints: cell(tr, gpIdx),
        result: cell(tr, resultIdx),
        attempt: cell(tr, attemptIdx),
      });
    }
    if (rows.length > 0) return rows;
  }
  return [];
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
