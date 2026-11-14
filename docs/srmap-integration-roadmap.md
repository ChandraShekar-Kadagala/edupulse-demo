# SRM AP academic data integration roadmap

Date: 2026-11-14. Based on [srmap-repository-audit.md](srmap-repository-audit.md),
[srmap-data-contract.md](srmap-data-contract.md), [srmap-security-review.md](srmap-security-review.md) and
[account-linking-architecture.md](account-linking-architecture.md).

**There is no live SRM integration in EduPulse, and this roadmap does not create one without institutional
authorization.**

## Final recommendation

### A. Reusable now (knowledge, not code)
| Item | From | How EduPulse uses it |
|---|---|---|
| Inventory of read-only portal reports (attendance `ids=3`, internals `ids=5`, ledger/CGPA `ids=6`, timetable `ids=10`, subjects `ids=2`, profile `ids=1`, past internals `ids=22/23`, period attendance `ids=33`) | `fetchData.ts`, `exams/*`, `fetchCheckAttendance.ts` | Defines which report types the manual importer supports. |
| Table shapes and column orders | same | Data contract + synthetic parser fixtures for `SrmPortalParserService`. |
| `splitPair` "obtained/max" convention for internal components | `fetchInternals.ts` | Parser rule; blank means pending. |
| Daily aggregate attendance snapshots as history | `api/srmapi/fetch/route.ts` (`attendanceHistory`) | Pattern for `attendance_snapshots` (dated imports instead of invented daily rows). |

### B. Requires redesign before use
| Item | Why | Redesign |
|---|---|---|
| Parsers (TypeScript/Cheerio) | Different stack; all strings; CGPA defaults to `"0"`; no validation | Java/jsoup in `SrmPortalParserService` + `AcademicImportValidator` (done) |
| Caching model | Password-derived keys, CBC without MAC, unencrypted shared timetables | PostgreSQL rows linked to import batches; no raw content; deletion by batch |
| Session/token model | Password in JWT, 365-day expiry, session ID in browser | Not needed: EduPulse keeps its short-lived JWT + `token_version`; no portal session at all |
| Account deletion | Leaves tokens, shared timetable data | `ImportedDataDeletionService` with batch-level deletion and audit |
| EduPulse `syncPortalReport` | Invented daily attendance, overclaiming labels, global status fields, no consent/size limits/reg-no check | Phase A below |

### C. Requires institutional authorization (do not build without it)
| Item | Requirement |
|---|---|
| Any automated retrieval from `student.srmap.edu.in` on a student's behalf | Written agreement with SRM University AP IT; documented API or sanctioned export; scoped credentials (e.g. OAuth2 client) issued to EduPulse; data-processing terms. |
| Live "sync" or scheduled refresh | Same; plus rate limits and an institutional contact for incidents. |
| Institution-wide roster/marks feeds for faculty dashboards | Data-sharing agreement; faculty CSV import remains the interim path. |

### D. Never (rejected regardless of demand)
CAPTCHA solving or any bot-protection bypass; collecting, storing or forwarding portal passwords; storing or exposing
portal session cookies; portal write operations (attendance marking, feedback submission, password reset, profile
edits); hosting a CAPTCHA-solving service; importing DOB, gender, photos or instructor personal data.

### E. Implementable immediately (no institutional dependency)
Everything in Phase A.

## Phases

### Phase 0 — Audit (this change) ✅
- Five documents in `docs/`.
- `AcademicImportValidator` + `AcademicImportRecords` + `AcademicDataSourceType`, unit-tested (25 tests, synthetic
  data). Not wired to endpoints; no schema change; not deployed.

### Phase A — Safe student import (no institution needed)
Order matters: fix integrity issues first.
1. **Stop fabricating attendance** (E1): remove the per-day expansion in
   `SrmPortalIntegrationService.syncAttendanceRecords`; store imported attendance as `attendance_snapshots`
   (V3 migration, staged for owner approval per [database-migrations.md](database-migrations.md)).
2. **Honest labels** (E2): rename provenance for student uploads to `STUDENT_MANUAL_IMPORT`; replace
   "authentic/verified/synchronized" wording in `IntegrationController`, `SrmPortalIntegrationService` and
   `AccountLinking.tsx`; soften `OfficialSrmDataProvider`'s message to "no official API is available to EduPulse"
   (availability is unverified, not known to be absent).
3. **Per-student import status** (E3): replace singleton fields with `academic_import_batches` rows.
4. **Limits and ownership** (E4, E6): 2 MB upload limit, report-type allowlist, registration number in the report
   must match the logged-in student.
5. **Consent + preview/confirm + deletion** (E5): `StudentConsentService`, `AcademicImportController`,
   `ImportedDataDeletionService`, UI screens.
6. **Wire the validator** into the import path; show rejected rows and warnings in the preview.
7. Tests: controller/authorization tests (own data only, consent required), duplicate upload (same SHA-256),
   unlink deletes exactly the batch's rows and recomputes risk, migration rehearsal for V3.

### Phase B — Data quality and analytics on imported data
- Risk engine reads latest attendance snapshot when no dated rows exist; trend detection across snapshots with
  their `as_of_date`.
- Exam-ledger import feeds `exam_results`; CGPA shown with its source batch; grade-scale confirmation against
  official regulations before any GPA calculation.

### Phase C — Institutional connection (only after authorization)
- Implement `OfficialSrmDataProvider` against the approved interface; provenance `OFFICIAL_INSTITUTIONAL_API`;
  label `LIVE_AUTHORIZED_CONNECTION`; least-privilege scopes; token storage server-side only; revocation on unlink.

## Out of scope for EduPulse
Arcade, resources hub, secure share, vacant rooms, faculty ratings and everything under `deprecated/` in Srmap-Api.
