/**
 * EduPulse record extractor — runs inside the student's OWN logged-in SRM portal tab.
 *
 * It reads the live page (and any same-origin frames the report is loaded into), extracts the academic
 * tables by their column headers, and strips personal fields (name, DOB, gender, photo) before anything
 * leaves. It never sees or stores the portal password, never logs in, never touches the CAPTCHA.
 *
 * This is the one function the extension injects into the page, so it is entirely self-contained
 * (all helpers nested) — it must not reference anything outside its own body. It is also exported so it
 * can be unit-tested against a jsdom document.
 */
export function extractAcademicExport(rootDoc) {
  const root = rootDoc || (typeof document !== 'undefined' ? document : null);
  const COURSE_CODE_RE = /\b[A-Z]{2,5}\s?\d{3}[A-Z]?\b/;
  const PROFILE_ALLOWLIST = ['registerNo', 'institution', 'semester', 'program', 'section', 'specialization'];

  const text = (el) => (el && el.textContent ? el.textContent : '').replace(/\s+/g, ' ').trim();
  const headerLabels = (table) => {
    const hr = table.querySelector('tr:has(th)') || table.querySelector('tr');
    return hr ? Array.from(hr.querySelectorAll('th, td')).map((c) => text(c).toLowerCase()) : [];
  };
  const col = (headers, ...pats) => headers.findIndex((h) => pats.some((p) => p.test(h)));
  const dataRows = (table) => Array.from(table.querySelectorAll('tr')).filter((tr) => tr.querySelector('td'));
  const cell = (tr, i) => (i >= 0 ? text(tr.querySelectorAll('td')[i]) : '');

  function parseAttendance(doc) {
    // 1. Fixed 9-column SRM layout, course code first (robust to the nested 2-row header).
    for (const table of Array.from(doc.querySelectorAll('table'))) {
      const rows = dataRows(table).filter((tr) => tr.querySelectorAll('td').length === 9 && COURSE_CODE_RE.test(text(tr.querySelectorAll('td')[0])));
      if (rows.length) {
        return rows.map((tr) => {
          const td = tr.querySelectorAll('td');
          return { subjectCode: text(td[0]), subjectName: text(td[1]), classesConducted: text(td[2]),
            present: text(td[3]), absent: text(td[4]), odMlTaken: text(td[5]), presentPercentage: text(td[6]),
            odMlPercentage: text(td[7]), attendancePercentage: text(td[8]) };
        });
      }
    }
    // 2. Other layouts: detect columns by header.
    for (const table of Array.from(doc.querySelectorAll('table'))) {
      const h = headerLabels(table);
      const codeIdx = col(h, /subject\s*code/, /course\s*code/, /sub\s*code/, /^code$/);
      const presentIdx = col(h, /present/, /attended/);
      if (codeIdx < 0 || presentIdx < 0) continue;
      const conductedIdx = col(h, /conducted/, /total.*(class|hour|conducted)/, /^total$/);
      const absentIdx = col(h, /absent/);
      const odIdx = col(h, /od.?ml/, /od\s*\/\s*ml/);
      const nameIdx = col(h, /subject\s*name/, /course\s*(title|name)/, /title/);
      const pPctIdx = col(h, /present\s*%/, /present percentage/);
      const aPctIdx = col(h, /attendance\s*%/, /total\s*%/, /percentage/);
      const rows = [];
      for (const tr of dataRows(table)) {
        const code = cell(tr, codeIdx);
        if (!COURSE_CODE_RE.test(code)) continue;
        const conducted = cell(tr, conductedIdx);
        const present = cell(tr, presentIdx);
        let absent = cell(tr, absentIdx);
        if (!absent && conducted && present) {
          const d = Number(conducted) - Number(present);
          if (Number.isFinite(d)) absent = String(d);
        }
        rows.push({ subjectCode: code, subjectName: cell(tr, nameIdx), classesConducted: conducted, present,
          absent, odMlTaken: cell(tr, odIdx), presentPercentage: cell(tr, pPctIdx), odMlPercentage: '',
          attendancePercentage: cell(tr, aPctIdx) });
      }
      if (rows.length) return rows;
    }
    return [];
  }

  function parseInternals(doc) {
    const table = Array.from(doc.querySelectorAll('table')).find((t) => {
      const txt = text(t).toLowerCase();
      return txt.includes('mark secured') || /max\.?\s*marks/.test(headerLabels(t).join('|'));
    });
    if (!table) return [];
    const split = (raw) => { const p = raw.split('/').map((x) => x.trim()); return { left: p[0] || '', right: p[1] || '' }; };
    const subjects = [];
    let current = null;
    for (const tr of Array.from(table.querySelectorAll('tr'))) {
      const td = tr.querySelectorAll('td');
      if (td.length === 4 && COURSE_CODE_RE.test(text(td[0]))) {
        current = { subjectCode: text(td[0]), subjectName: text(td[1]), marksObtained: text(td[2]), maxMarks: text(td[3]), components: [] };
        subjects.push(current);
      } else if (td.length === 3 && current) {
        const name = text(td[0]);
        if (!name || /^name$/i.test(name)) continue;
        const cond = split(text(td[1]));
        const conv = split(text(td[2]));
        current.components.push({ name, conductedObtained: cond.left, conductedMax: cond.right, convertedObtained: conv.left, convertedMax: conv.right });
      }
    }
    return subjects.filter((s) => s.marksObtained !== '' || s.components.length > 0);
  }

  function parseLedger(doc) {
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
      const rows = [];
      for (const tr of dataRows(table)) {
        const code = cell(tr, codeIdx);
        if (!COURSE_CODE_RE.test(code)) continue;
        rows.push({ semester: cell(tr, semIdx), monthYear: cell(tr, myIdx), subjectCode: code,
          subjectDescription: cell(tr, descIdx), credit: cell(tr, creditIdx), grade: cell(tr, gradeIdx),
          gradePoints: cell(tr, gpIdx), result: cell(tr, resultIdx), attempt: cell(tr, attemptIdx) });
      }
      if (rows.length) return rows;
    }
    return [];
  }

  function parseCgpa(doc) {
    const m = text(doc.body).match(/CGPA[^0-9]{0,8}([0-9]+(?:\.[0-9]+)?)/i);
    return m ? m[1] : null;
  }

  function stripProfile(doc) {
    const out = {};
    doc.querySelectorAll('table tr').forEach((tr) => {
      const td = tr.querySelectorAll('td');
      if (td.length !== 3) return;
      const key = text(td[0]);
      const val = text(td[2]);
      if (!val) return;
      if (/Register\s*No/i.test(key)) out.registerNo = val;
      else if (/Institution/i.test(key)) out.institution = val;
      else if (/^Semester$/i.test(key)) out.semester = val;
      else if (/Program\s*\/\s*Section/i.test(key)) {
        const [p, s] = val.split('/').map((v) => v.trim());
        if (p) out.program = p;
        if (s) out.section = s.replace(/['"]+/g, '');
      } else if (/Specialization/i.test(key)) out.specialization = val;
      // Never: Student Name, D.O.B. / Gender, photo.
    });
    Object.keys(out).forEach((k) => { if (!PROFILE_ALLOWLIST.includes(k)) delete out[k]; });
    return out;
  }

  // The report may sit in a same-origin iframe; gather the main document and any accessible frames.
  const docs = [root];
  try {
    root.querySelectorAll('iframe').forEach((f) => {
      try { if (f.contentDocument && f.contentDocument.body) docs.push(f.contentDocument); } catch (e) { /* cross-origin */ }
    });
  } catch (e) { /* ignore */ }

  const attendance = [];
  const ledger = [];
  const internals = [];
  let cgpa = null;
  let profile = {};
  const seenAtt = new Set();
  const seenLed = new Set();
  const seenInt = new Set();
  for (const d of docs) {
    try {
      parseAttendance(d).forEach((r) => { if (!seenAtt.has(r.subjectCode)) { seenAtt.add(r.subjectCode); attendance.push(r); } });
      parseLedger(d).forEach((r) => { const k = r.subjectCode + '|' + r.semester + '|' + r.attempt; if (!seenLed.has(k)) { seenLed.add(k); ledger.push(r); } });
      parseInternals(d).forEach((r) => { if (!seenInt.has(r.subjectCode)) { seenInt.add(r.subjectCode); internals.push(r); } });
      if (cgpa == null) cgpa = parseCgpa(d);
      profile = Object.assign({}, stripProfile(d), profile);
    } catch (e) { /* skip a bad frame */ }
  }

  return {
    sourceType: 'STUDENT_MANUAL_IMPORT',
    exportedAt: new Date().toISOString(),
    reportDate: null,
    attendance,
    ledger,
    internals,
    cgpa,
    profile,
  };
}
