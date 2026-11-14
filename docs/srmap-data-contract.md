# SRM AP academic data contract

Field-level description of the academic data that SRM AP Student Corner reports expose, **as parsed by
Srmap-Api at commit `290a88f`**, and the rules EduPulse applies before storing anything
(`backend/src/main/java/com/edupulse/service/importing/AcademicImportValidator.java`).

Conventions
- *Source* = Student Corner report resource (`students/report/studentreportresources.jsp`, form field `ids`) or page.
- *Type (source)* = what Srmap-Api produces. **Every field is a string**; no field is typed or validated there.
- *Mandatory* = required by the EduPulse validator for the row to be accepted.
- *Last updated*: no report carries a portal-side "as of" timestamp. Srmap-Api records only its own fetch time
  (`last_fetched` via `getFullDateAndTime()`, or `session_time`). EduPulse must store the **import time** and the
  student's stated **report date**, and never present either as the institution's update time.
- Only fields present in the source are listed. Nothing here is inferred.

## 1. Subject-wise attendance
Source: `ids=3`, table `#tblSubjectWiseAttendance`, rows with exactly 9 `<td>`. Parser: `fetchFromWebsite()` →
`AttendanceRecord` (`src/server/srmapi/fetchData.ts`). EduPulse validator: `validateAttendance()`.

| Field | Type (source) | EduPulse type | Mandatory | Validation | Known limitations |
|---|---|---|---|---|---|
| `subject_code` | string | `String` normalised `"CSE 204"` | yes | `^[A-Z]{2,5}\s*\d{3}[A-Z]?$`; unique per import | Spacing varies (`CSE204`/`CSE 204`). |
| `subject_name` | string | `String` | no | trimmed | Free text. |
| `classes_conducted` | string | `int ≥ 0` | yes | integer, non-negative | Counts periods/hours, not days. |
| `present` | string | `int ≥ 0` | yes | `present + absent == conducted` | |
| `absent` | string | `int ≥ 0` | yes | see above | |
| `od_ml_taken` | string | `int ≥ 0` | no (blank → 0) | warning if `> absent` | Meaning of OD/ML adjustment is portal policy. |
| `present_percentage` | string | `Double` | no | warning if differs from computed by > 1.0 | Computed value is authoritative. |
| `od_ml_percentage` | string | — | no | not stored | Derived. |
| `attendance_percentage` | string | — | no | not stored | Portal total incl. OD/ML; re-derived. |

**Aggregate only.** The report gives totals, not dates. EduPulse must store it as a dated *summary snapshot*
(see architecture §5), never expand it into invented per-day rows.

### 1a. Period-level attendance (optional)
Source: `students/transaction/studentattendance.jsp`, `ids=33`. Parser: `fetchAttendance()`
(`fetchCheckAttendance.ts`). Fields `date`, `day`, `hour`, `subject`, `status` (all strings). This is the only
dated attendance source. Not part of the first EduPulse import scope; if added later it requires the same
validation (date format, status vocabulary, subject → course mapping).

## 2. Internal assessment marks
Source: `ids=5`, `table.table.table-striped.table-bordered > tbody > tr` with 4 `<td>` (subject summary) followed by a
detail row whose inner table rows have 3 `<td>`. Parser: `fetchInternalMarks()` + `splitPair()`
(`src/server/srmapi/exams/fetchInternals.ts`). EduPulse validator: `validateInternals()`.

| Field | Type (source) | EduPulse type | Mandatory | Validation | Known limitations |
|---|---|---|---|---|---|
| `subject_code` | string | normalised code | yes | as §1 | |
| `subject_name` | string | `String` | no | | |
| `marks_obtained` / `max_marks` (subject total) | string | — | no | not stored; recomputed from components | Portal total may include unconverted components. |
| `exams[].name` | string | `String` component name | yes | unique per subject (case-insensitive) | Names are free text ("CLA 1", "Mid Semester Exam I"). |
| `exams[].conducted_obtained` | string (left of `"a/b"`) | `Double` or **pending** | no | blank or `-` → pending (never 0); `0 ≤ obtained ≤ max` | `splitPair` returns `""` when absent. |
| `exams[].conducted_max` | string (right of `"a/b"`) | `Double > 0` | yes | `> 0` | |
| `exams[].converted_obtained` | string | `Double` | no | `≤ converted_max` | Conversion weights are portal-defined. |
| `exams[].converted_max` | string | `Double` | no | ≥ 0 | |
| `last_fetched` | string (fetch time) | import timestamp | — | — | Fetch time, not portal update time. |

Past semesters: `ids=22` (semester list) and `ids=23` with `filter=<semester>` (`fetchPastInternals.ts`), fields
`semester`, `code`, `description`, `subject_type`, `mark_obtained`, `max_mark`. Same rules apply.

## 3. CGPA
Two sources, both `ids=6`:
- `fetchFromWebsite()` reads `div[style*='float: right'][style*='font-size']` and takes text after `":"`.
  **Defaults to the string `"0"` when the element is missing.**
- `fetchExamLedger()` regex `CGPA\s*:\s*([0-9.]+)`; empty string when missing.

| Field | Type (source) | EduPulse type | Mandatory | Validation | Known limitations |
|---|---|---|---|---|---|
| `cgpa` | string | `Double` or **null** | no | numeric, `0 < cgpa ≤ 10`; blank → null; literal `0` → null + warning | A real CGPA of 0 is indistinguishable from the `"0"` fallback, so 0 is never stored. |

## 4. Semester results (exam ledger)
Source: `ids=6`, `div.subTable table tr` with ≥ 9 `<td>`. Parser: `fetchExamLedger()`. EduPulse validator:
`validateLedger()`.

| Field | Type (source) | EduPulse type | Mandatory | Validation | Known limitations |
|---|---|---|---|---|---|
| `semester` | string | `String` | yes | non-blank | Label text, e.g. "Semester 1". |
| `month_year` | string | `String` | no | | e.g. "DEC-2023"; not parsed to a date. |
| `subject_code` | string | normalised code | yes | as §1 | |
| `subject_description` | string | `String` | no | | |
| `credit` | string | `Double ≥ 0` | yes | non-negative number | |
| `grade` | string | `String` | yes | one of `O, A+, A, B+, B, C, D, P, F, AB, I, W` | Grade scale should be confirmed against the official regulations before use in GPA maths. |
| `grade_points` | string | `Double` | yes | `0 ≤ gp ≤ 10` | |
| `result` | string | `String` | no | warning if not in `PASS, FAIL, P, F, ABSENT, WITHHELD, RA` | Vocabulary not documented by the portal. |
| `attempt` | string | `int ≥ 1` | no (blank → 1) | integer ≥ 1; unique per (code, semester, attempt) | |

## 5. Subjects
Source: `ids=2`, `table.table-striped tr` with ≥ 4 `<td>`, merged with the timetable legend. Parser:
`fetchFromWebsite()` → `SubjectDetail`.

| Field | Type (source) | Mandatory | Notes |
|---|---|---|---|
| `code` | string | yes | Header rows filtered by `/course code|sub code|^code$/i`. |
| `name` | string | yes | Timetable legend name preferred over subject-table name. |
| `credit` | string | no | Numeric text. |
| `semester` | string | no | |
| `ltp` | string | no | From timetable legend, else 5th column ("category"). |
| `faculty` | string | no | **Instructor names (third-party personal data).** EduPulse must not import them by default. |
| `classrooms` | string | no | e.g. "(C 305), (S 404)". |
| `facultyCabins` | object[] | no | Derived from a static faculty directory, not the portal. Not imported. |

## 6. Timetable
Source: `ids=10`. Parser: `fetchFromWebsite()`: rows from index 2; the first 5 rows are weekday rows
(`TimetableDay { day, subjects: string[] }`, one cell per period); rows from index 7 are the subject legend
(code, name, LTP, faculty, classrooms). EduPulse validator: `normalizeTimetable()`.

| Field | Type (source) | EduPulse type | Mandatory | Validation | Known limitations |
|---|---|---|---|---|---|
| `day` | string | `Monday`…`Saturday` | yes | Mon/Monday…Sat/Saturday; unique | Only 5 weekday rows are taken by the source parser. |
| `subjects[i]` | string (cell text) | `TimetableSlot(day, period=i+1, courseCode)` | no | first course-code token in the cell; empty/`-`/`FREE` → no slot | Period start/end times are not in the cells; the slot→time map must come from the institution's published slot table. |

## 7. Profile
Source: `ids=1` (`table.table-striped tr` with 3 `<td>`) and the photo from `HRDSystem`.

| Field | Source label | EduPulse handling |
|---|---|---|
| `registerNo` | "Register No" | **Kept** (must match the logged-in EduPulse student's registration number). |
| `institution`, `semester`, `program`, `section`, `specialization` | as named | **Kept** (allowlist `PROFILE_ALLOWLIST`). |
| `studentName` | "Student Name" | Dropped — EduPulse already holds the account name. |
| `dob`, `gender` | "D.O.B. / Gender" | **Always dropped** — not needed for academic-risk analysis. |
| `picture` | profile image URL | **Always dropped.** |

## 8. Fields EduPulse does not use
Feedback subjects/comments, attendance-marking tokens, password-reset data, portal session identifiers,
faculty cabins, and any field from the write operations in the audit §3.3.
