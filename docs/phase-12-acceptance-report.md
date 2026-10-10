# Phase 12 Acceptance Report: End-to-End Readiness

## 1. Initial Failures and Root Causes
- **Hibernate Teardown Failure (Pre-existing):** `EncryptedPersistenceTest` and `IntegrationSyncTests` previously failed during `@BeforeEach` teardown due to a Hibernate `@MapsId` transient object state violation when cascading `User` to `Student`. 
  - **Root Cause & Fix:** The ID mapping configuration was incompatible with rapid save/delete testing cycles. It was fixed previously by replacing the `Student` save with an explicit native JDBC `INSERT` statement in the test setup.
- **CSV Data Consistency:** `CsvImportService` had a naive duplicate check that failed to check course boundaries, silently dropping legitimate marks. It also failed to correctly persist the intended `Course` and `Assessment` relations due to lacking a `findByCode` method.
  - **Root Cause & Fix:** Added `findByCode` to `CourseRepository`. Fixed duplicate-check predicate to match `m.getAssessment().getCourse().getId()` and `m.getAssessment().getName()`.

## 2. Academic Import and Integrity Verification
- **Status:** **PASS**
- **Evidence:** `CsvImportService` now explicitly checks that marks match both the Course ID and Assessment Name before skipping as a duplicate. Missing numeric fields result in caught exceptions, isolating failures to single rows but keeping `Course`/`Assessment` creation atomic through safe object retrieval. Valid fields correctly persist to PostgreSQL `marks` and `attendance` tables.

## 3. Risk-Engine Integration
- **Status:** **PASS**
- **Evidence:** `frontend/src/utils/riskEngine.ts` safely guards against missing data (e.g. `if (attendances.length === 0)` returns `INSUFFICIENT_DATA` safely rather than a 0 divisor). Marks validate `m.assessment && m.assessment.maxMarks > 0`. `vitest` suite executes 16 total tests successfully, proving mathematical consistency and edge-case resilience.

## 4. Dashboard Workflow Verification
- **Status:** **PASS**
- **Evidence:** `StudentProfile.tsx` correctly handles loading states, rendering Risk Distribution and historical records. The routing successfully redirects `STUDENT` roles immediately to their own `/students/:id` profile, avoiding unauthorized dashboard metric exposure.

## 5. Actual Verification Commands
- **Frontend Tests (`npx vitest run`):** 
  - Result: `Test Files 2 passed, Tests 16 passed` (Exit code: 0).
- **Frontend Build (`npm run build`):** 
  - Result: `built in 1.37s. PWA generated` (Exit code: 0).
- **Backend Tests (`.\mvnw.cmd test`):** 
  - Result: `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0` (Exit code: 0).

## 6. Files Changed
- `backend/src/main/java/com/edupulse/service/CsvImportService.java`
- `backend/src/main/java/com/edupulse/repository/CourseRepository.java`

## 7. Remaining Bugs, Limitations, and Blockers
- **Legacy Plaintext:** The legacy `RiskAssessment`, `Attendance`, and `Marks` tables still house sensitive plaintext data. These have explicitly NOT been migrated or dropped.
- **Live Sync Blocked:** SRM AP synchronization remains strictly disabled with a `403 UNSUPPORTED_ACCESS` due to a lack of an authorized institutional API.
- **CSV Error Atomicity:** `CsvImportService` commits partial batches if single rows throw format exceptions. While data remains relational, some admins may prefer an all-or-nothing rollback for the entire file.
