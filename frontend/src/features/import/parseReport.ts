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

/** One internal-assessment component of a subject (e.g. "CLA 1", "Mid Semester Exam I"). */
export interface RawInternalComponent {
  name: string;
  conductedObtained: string;
  conductedMax: string;
  convertedObtained: string;
  convertedMax: string;
}

/** A subject's internal marks: the total (obtained / max) plus the component breakdown. */
export interface RawInternalSubject {
  subjectCode: string;
  subjectName: string;
  marksObtained: string;
  maxMarks: string;
  components: RawInternalComponent[];
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
  internals: RawInternalSubject[];
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

/** SRM attendance data rows have 9 columns in a fixed order, course code first. */
function positionalAttendance(tr: Element): RawAttendanceRow {
  const td = tr.querySelectorAll('td');
  return {
    subjectCode: text(td[0]), subjectName: text(td[1]), classesConducted: text(td[2]),
    present: text(td[3]), absent: text(td[4]), odMlTaken: text(td[5]),
    presentPercentage: text(td[6]), odMlPercentage: text(td[7]), attendancePercentage: text(td[8]),
  };
}

/**
 * Subject-wise attendance. The real SRM table has a nested 2-row header (Present/Absent under
 * "Attendance Entered (Slots)"), so header-by-name mapping is unreliable; its columns are a fixed
 * 9-wide order (code, name, conducted, present, absent, OD/ML, present%, OD/ML%, attendance%) with the
 * course code first — which also distinguishes it from the exam ledger (code is not its first column).
 * Falls back to header-based mapping for other layouts.
 */
export function parseAttendance(doc: Document): RawAttendanceRow[] {
  // 1. Fixed 9-column SRM layout (robust to nested headers).
  for (const table of Array.from(doc.querySelectorAll('table'))) {
    const rows = dataRows(table).filter(
      (tr) => tr.querySelectorAll('td').length === 9 && COURSE_CODE_RE.test(text(tr.querySelectorAll('td')[0])));
    if (rows.length > 0) return rows.map(positionalAttendance);
  }
  // 2. Other layouts: detect columns by header.
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

function splitPair(raw: string): { left: string; right: string } {
  const [left, right] = raw.split('/').map((x) => x.trim());
  return { left: left ?? '', right: right ?? '' };
}

/**
 * Internal assessment marks. The real "Internal Mark Details" page is a subject row (code, description,
 * Marks Obtained, Max.Marks) followed by an expandable detail table of components (Name,
 * Mark Secured(Conducted) "a / b", Mark Secured(Converted) "a / b"). The detail rows live in the DOM even
 * when collapsed, so this reads them. Max marks are taken from the page (they vary, e.g. 40 vs 50).
 */
export function parseInternals(doc: Document): RawInternalSubject[] {
  const table = Array.from(doc.querySelectorAll('table')).find((t) => {
    const txt = text(t).toLowerCase();
    return txt.includes('mark secured') || /max\.?\s*marks/.test(headerLabels(t).join('|'));
  });
  if (!table) return [];

  const subjects: RawInternalSubject[] = [];
  let current: RawInternalSubject | null = null;
  for (const tr of Array.from(table.querySelectorAll('tr'))) {
    const td = tr.querySelectorAll('td');
    if (td.length === 4 && COURSE_CODE_RE.test(text(td[0]))) {
      current = { subjectCode: text(td[0]), subjectName: text(td[1]), marksObtained: text(td[2]), maxMarks: text(td[3]), components: [] };
      subjects.push(current);
    } else if (td.length === 3 && current) {
      const name = text(td[0]);
      if (!name || /^name$/i.test(name)) continue;
      const cond = splitPair(text(td[1]));
      const conv = splitPair(text(td[2]));
      current.components.push({ name, conductedObtained: cond.left, conductedMax: cond.right, convertedObtained: conv.left, convertedMax: conv.right });
    }
  }
  return subjects.filter((s) => s.marksObtained !== '' || s.components.length > 0);
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
    internals: safe(() => parseInternals(doc), []),
    cgpa: safe(() => parseCgpa(doc), null),
    profile: safe(() => stripProfile(doc), {}),
  };
}

/** True when nothing academic was found — the UI shows a "couldn't read this page" state instead of a false success. */
export function isEmptyExport(e: AcademicExport): boolean {
  return e.attendance.length === 0 && e.ledger.length === 0 && e.internals.length === 0
    && e.cgpa === null && Object.keys(e.profile).length === 0;
}
