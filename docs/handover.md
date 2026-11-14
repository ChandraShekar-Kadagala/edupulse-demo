# EduPulse — developer handover

Last updated: 2026-11-14. This is the single starting point for anyone picking up EduPulse.

## 1. What EduPulse is
An explainable early-academic-risk platform. Students' attendance, marks and missing work feed a
transparent rule-based risk score (0–100); faculty see at-risk students, log interventions, and the
system measures whether those interventions helped. Backend: Spring Boot 3.2 / Java 21 / PostgreSQL
(Neon in production) / Flyway. Frontend: React 19 / Vite / Tailwind. CI: GitHub Actions. Hosting: Render.

## 2. How to run it
```bash
# Backend (PostgreSQL via docker-compose, or H2 dev profile)
cd backend
./mvnw test                      # unit + Spring + PostgreSQL Testcontainers (Docker needed for the last 5)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run   # H2, no Docker

# Frontend
cd frontend
npm install
npm run dev                      # expects backend at :8080
npm run build                    # tsc -b && vite build
```
Demo accounts and env vars: see [../README.md](../README.md). Release & migration rules:
[database-migrations.md](database-migrations.md) — **a push to `main` auto-deploys to Render and runs
Flyway on the production Neon DB**, so schema changes are staged for approval first.

## 3. Current status

### Done and live (on `main`)
- Explainable risk engine, single source of truth `RiskFormula.java` ([risk-engine.md](risk-engine.md)).
- Data-driven faculty dashboards from `course_assignments`; cohort overview; student profiles.
- Intervention effectiveness analytics — observational, with sample sizes and no causal claims
  ([intervention-analytics.md](intervention-analytics.md)).
- Security hardening: admin-only demo provisioning, explicit public-endpoint allow-list, no secrets in
  tokens ([security.md](security.md)). Flyway-owned schema, `ddl-auto=validate`.
- V2 migration applied to production (course assignments, stored provenance, wider roles).
- CI on every branch; 126 backend tests, 47 frontend tests.

### Done, on a branch, NOT yet deployed — student-controlled academic import
The safe alternative to portal scraping (see §5). Student logs into the SRM portal themselves, saves
their report, and brings it to EduPulse. Built:
- `frontend/src/features/import/parseReport.ts` — browser-side parser; extracts attendance/ledger/CGPA and
  **strips name, DOB, gender, photo before anything leaves the device**. 9 tests.
- `frontend/src/pages/ImportMyRecords.tsx` — route `/import-records`: load report → preview → download clean
  JSON → "Validate with EduPulse".
- `backend/.../service/importing/AcademicImportValidator.java` — authoritative server-side validation
  (missing data stays missing, inconsistent rows rejected, duplicates rejected, profile allowlist). 25 tests.
- `backend/.../controller/StudentImportController.java` — `POST /api/students/me/imports/preview`:
  validates an upload and returns accepted/rejected; **stores nothing** (no schema change, no prod risk). 3 tests.

### Staged, needs owner approval before it can persist imported data
- Flyway **V3** (proposed): `attendance_snapshots`, `exam_results`, consent + import-batch tables. Design in
  [account-linking-architecture.md](account-linking-architecture.md) §5. Not written as a migration file yet;
  must follow the self-verifying/fingerprint pattern in [database-migrations.md](database-migrations.md).

## 4. Remaining roadmap (not built)
Student import persistence (needs V3) → trend detection across snapshots → what-if simulator → ML risk
model (needs real labelled data) → notifications → extra roles/cohort analytics → wellbeing check-ins →
PDF/CSV reports → real-time updates → mobile. Order and guardrails: original phased plan + the SRM roadmap
([srmap-integration-roadmap.md](srmap-integration-roadmap.md)).

## 5. SRM data integration — the decision that shapes everything
EduPulse will **not** log into the SRM portal for students, hold their passwords, or solve the portal
CAPTCHA (full reasoning: [srmap-security-review.md](srmap-security-review.md),
[srmap-repository-audit.md](srmap-repository-audit.md)). Real academic data enters three ways, each labelled
by source and never called "live" unless it truly is:
1. **Student self-import** (built) — the student's own data, in their control.
2. **Institution records** — entered/uploaded into EduPulse by faculty/admin; or a future authorized SSO/API.
3. **Synthetic demo data** — the showcase accounts.

**Faculty dashboards** already work from `course_assignments` + whatever academic data exists; what makes
them show *real* students is lane 1 and 2. The StoreVia/Srmap-Api faculty directory file was **not** copied
in: that repository has no licence (all rights reserved), so its data is not ours to reuse. A faculty
directory (names, cabins, courses) can be added from the university's own public directory data.

## 6. Key files
| Area | File |
|---|---|
| Risk formula | `backend/src/main/java/com/edupulse/service/risk/RiskFormula.java` |
| Risk engine | `backend/.../service/RuleBasedRiskEngineImpl.java` |
| Faculty dashboard | `backend/.../service/FacultyService.java` |
| Intervention analytics | `backend/.../service/InterventionEffectivenessService.java` |
| Import validator | `backend/.../service/importing/AcademicImportValidator.java` |
| Import preview API | `backend/.../controller/StudentImportController.java` |
| Report parser (client) | `frontend/src/features/import/parseReport.ts` |
| Import page | `frontend/src/pages/ImportMyRecords.tsx` |
| Dashboards | `frontend/src/components/dashboard/*` |
| Docs | `docs/*.md` |

## 7. Known limitations (honest)
- Imported data cannot be saved to a profile yet (awaiting V3).
- Without institution records or SSO, faculty dashboards show real data only for students who self-import;
  otherwise demo data.
- The client parser targets documented SRM report structures; its selectors need tuning against one real
  saved report page (done by the student with their own login, not by automated access).
- ML risk model needs real labelled historical data before any accuracy claim.
- 5 backend tests require Docker (PostgreSQL Testcontainers); they run in CI.
