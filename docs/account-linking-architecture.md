# Academic data import and account-linking architecture

Status: **design** (2026-11-14). Nothing in this document is deployed except the pure validator noted in §4.
Inputs: [srmap-repository-audit.md](srmap-repository-audit.md), [srmap-data-contract.md](srmap-data-contract.md),
[srmap-security-review.md](srmap-security-review.md).

## 1. Goal and constraints
Give EduPulse accurate, student-authorized academic data — course-wise attendance, internal marks, CGPA, semester
results, subjects and timetable — while:
- staying **read-only** toward the institution (no attendance marking, feedback, password reset or profile edits);
- never handling the student's portal password, session cookie or CAPTCHA;
- staying on the existing Spring Boot + PostgreSQL (Neon) stack with Flyway (no MongoDB);
- labelling every value with how it arrived, and never calling an import "live".

## 2. Connection methods

| Method | Available? | Evidence | EduPulse decision |
|---|---|---|---|
| **Official / institution-approved API or data feed** | **Not verified.** No public documentation found; Srmap-Api has no institutional agreement; EduPulse's `OfficialSrmDataProvider` is disabled by design (`syncStudentData` throws `UnsupportedOperationException`). | audit §1 | Keep the `OfficialSrmDataProvider` slot. Build only after SRM AP IT provides a written integration agreement, endpoint documentation and scoped credentials (e.g. OAuth client). |
| **Automated login with student password + CAPTCHA solving** (Srmap-Api model) | Technically exists in Srmap-Api | audit §3.1, §3.4 | **Rejected.** Bypasses the portal's bot protection, requires holding students' passwords, and is not authorized. |
| **Student-controlled manual import** of their own report (saved HTML page; PDF/CSV if the institution provides exports) | **Yes** | EduPulse `POST /api/integration/sync-report` already accepts pasted HTML | **Primary method.** Redesign per §5–6 (preview, consent, provenance, real snapshots). |
| **Faculty/admin CSV import** of institution records | Yes (existing `CsvImportService`) | — | Unchanged; provenance `CSV_FACULTY_IMPORT`. |
| **Cached data** | Derived | — | Shown with import date and "may be out of date". |
| **Synthetic demo data** | Yes | `DataSeeder` | Unchanged; provenance `SYNTHETIC_TEST_FIXTURE`. |

User-facing labels come from `AcademicDataSourceType` (`LIVE_AUTHORIZED_CONNECTION`, `STUDENT_MANUAL_IMPORT`,
`CACHED_IMPORT`, `SYNTHETIC_DEMO`); only the first may ever be shown as "live", and no code path produces it today.

## 3. Component design (evaluation of proposed services)

| Proposed component | Decision | Responsibility in EduPulse |
|---|---|---|
| `SrmAccountLinkingService` | **Not built as named.** "Linking" an SRM account implies credentials. Replaced by `AcademicImportService` (manual import) now, and an `InstitutionalConnectionService` only if an authorized API is granted. | — |
| `AcademicDataProvider` (exists) | **Keep and narrow.** Providers declare `getProvenance()`, `isAvailable()`, `supportsAutoSync()`. Add a separate `AcademicReportImporter` interface (`parse(InputStream, ReportType) → raw records`) so file importers are not mistaken for sync providers. | `OfficialSrmDataProvider` (disabled), `CsvAcademicDataProvider`, `DemoAcademicDataProvider`, `PortalFixtureAcademicDataProvider` (dev only). |
| `AcademicImportValidator` | **Implemented** (pure, unit-tested). | Validates/normalizes raw records per the data contract; produces a `ValidationReport` with accepted rows and per-row issues. `backend/src/main/java/com/edupulse/service/importing/AcademicImportValidator.java` |
| `StudentConsentService` | **Build (Phase A).** | Records versioned, purpose-specific consent (`ACADEMIC_RISK_ANALYTICS`) before the first import; `revoke()` triggers deletion (below). |
| `AcademicDataProvenance` | **Reuse `DataProvenance` + `AcademicDataSourceType`.** Add enum value `STUDENT_MANUAL_IMPORT` to `DataProvenance` (stored as VARCHAR, no constraint change). Stop using `INSTITUTIONAL_PORTAL_REPORT` for student uploads. | Every imported row links to its import batch. |
| `AccountUnlinkService` | **Build as `ImportedDataDeletionService`.** | On consent revocation or "delete my imported data": delete the student's import batches and all rows created from them, recompute the risk snapshot, write an audit entry without academic content. |
| `AcademicImportController` | **Build (Phase A).** Replaces `IntegrationController.syncPortalReport`. | `POST /api/students/me/imports` (multipart upload → preview), `POST /api/students/me/imports/{id}/confirm`, `DELETE /api/students/me/imports/{id}`, `GET /api/students/me/imports`, `POST /api/students/me/consent`, `DELETE /api/students/me/consent`. Student role only, own data only. |

Reused existing pieces: `SrmPortalParserService` (jsoup parsing; extended with the column layouts documented in the
data contract), `RiskAssessmentService.ensureCurrentSnapshot`, `AuditLogRepository`, existing JWT + `token_version`.

## 4. What exists now (this audit stage)
- `AcademicImportValidator`, `AcademicImportRecords`, `AcademicDataSourceType` — pure classes, **not wired to any
  endpoint**, covered by `AcademicImportValidatorTest` (25 tests, synthetic fixtures only).
- No schema change, no endpoint change, no deployment.

## 5. Data model (proposed Flyway V3 — not created)
Additive tables on PostgreSQL; existing tables untouched except one nullable column.

```
student_data_consents
  id BIGINT PK, student_id BIGINT FK students(user_id), purpose VARCHAR(64), policy_version VARCHAR(16),
  granted_at TIMESTAMP, revoked_at TIMESTAMP NULL

academic_import_batches
  id BIGINT PK, student_id FK, consent_id FK, source_type VARCHAR(32)  -- STUDENT_MANUAL_IMPORT
  report_type VARCHAR(32)        -- ATTENDANCE | INTERNALS | EXAM_LEDGER | TIMETABLE
  report_date DATE NULL           -- the date the student says the report was generated
  uploaded_at TIMESTAMP, content_sha256 CHAR(64), status VARCHAR(16)  -- PREVIEW | CONFIRMED | DISCARDED
  accepted_rows INT, rejected_rows INT
  UNIQUE (student_id, content_sha256)          -- duplicate upload detection

attendance_snapshots              -- replaces fabricated per-day rows for imported data
  id BIGINT PK, student_id FK, course_id FK, batch_id FK academic_import_batches,
  as_of_date DATE, conducted INT, present INT, absent INT, od_ml INT
  UNIQUE (student_id, course_id, as_of_date)

exam_results
  id BIGINT PK, student_id FK, course_id FK, batch_id FK, semester VARCHAR(32), month_year VARCHAR(16),
  credit NUMERIC(4,1), grade VARCHAR(4), grade_points NUMERIC(4,2), result VARCHAR(16), attempt INT
  UNIQUE (student_id, course_id, semester, attempt)

marks.import_batch_id BIGINT NULL FK   -- links imported marks to their batch (existing table, nullable column)
```
Raw uploaded files are **not stored**. Rules from [database-migrations.md](database-migrations.md) apply
(self-verifying, fingerprint guard, staged for owner approval).

Risk engine change: when a student has attendance snapshots but no dated attendance rows for a course, use the
latest snapshot's `present/conducted`; trend detection uses the sequence of snapshots (each with its `as_of_date`),
which is honest dated data.

## 6. Flows

### 6.1 Consent (first use)
Student opens *Import my academic records* → reads the purpose, data list (academic only), retention and
"delete anytime" text → `POST /consent` → `student_data_consents` row.

### 6.2 Import
1. Student saves their own report from the portal and uploads it (max 2 MB, HTML/PDF/CSV per supported format),
   choosing the report type and the report date.
2. Server: authorization (STUDENT, own record, consent active) → SHA-256 duplicate check → parse in memory
   (`SrmPortalParserService`) → `AcademicImportValidator.validate(…, STUDENT_MANUAL_IMPORT)` →
   registration number in the report must equal the student's own (`registerNo` from the allowlisted profile) →
   batch saved as `PREVIEW` with counts; raw content discarded.
3. Preview shows accepted rows, rejected rows with reasons and warnings.
4. Student confirms → rows written with `batch_id`, provenance `STUDENT_MANUAL_IMPORT`; `ensureCurrentSnapshot`
   recalculates risk; audit log records batch id and counts only.

### 6.3 Unlink / delete
`DELETE /consent` or `DELETE /imports/{id}` → `ImportedDataDeletionService` deletes rows by `batch_id`, removes
the batch(es), recalculates risk, audit-logs the action. Faculty notes and interventions are not deleted (they are
faculty records), but they no longer reference deleted data.

### 6.4 Display
Every imported figure shows "Imported by you on {date} from a report dated {report_date} — not independently
verified". Faculty views show the same source label. Nothing is labelled "live", "synced" or "verified".

## 7. Security controls (summary)
No portal credentials, sessions or CAPTCHA anywhere in EduPulse; academic-only allowlist; consent before import;
own-data authorization; upload size and type limits; in-memory parsing; no raw-content retention; batch-level
deletion; counts-only logging; existing short-lived JWT with server-side revocation. Details:
[srmap-security-review.md](srmap-security-review.md) §2.
