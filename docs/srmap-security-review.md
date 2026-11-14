# Srmap-Api security review (for EduPulse integration design)

Scope: commit `290a88f`, static review only. Findings describe that repository; they are recorded so EduPulse does
not inherit the same patterns. Severity is from EduPulse's perspective if the pattern were adopted.

## 1. Findings

| # | Area | Finding | Evidence | Severity |
|---|---|---|---|---|
| S1 | SRM password handling | The student's **plaintext portal password is placed in the JWT payload**: `createToken({ username, password, admin })`. A JWT is signed, not encrypted — anyone holding the token can base64-decode the password. | `src/app/api/auth/login/route.ts` (`POST`), `src/server/utils/functions.ts` (`createToken`), `src/server/auth/verifyUser.ts` (`TokenPayload.password`) | Critical |
| S2 | Token lifetime | `ACCESS_EXPIRE=365` → tokens (and therefore the embedded password) are valid for a year. | `.env.example`, `createToken` (`expiresIn: ${expire}d`) | Critical |
| S3 | Token revocation | No revocation: no token version, deny-list or session table. Logout, password change and account deletion leave issued tokens valid until expiry. | `verifyToken`, `requireAuthResponse`, `api/auth/delete/route.ts` | High |
| S4 | Browser storage | `accessToken` (contains the password) and the **portal `JSESSIONID`** are stored in `localStorage` for up to five accounts. Any XSS or malicious extension can read them. README calls this "local encrypted credential preservation"; it is not encrypted. | `src/hooks/context/useLocalStorage.tsx`, `src/context/LocalStorageContext.tsx`, `src/hooks/context/useAuthActions.tsx` (decodes JWT, checks `decoded.password`) | Critical |
| S5 | Session ID exposure | The portal session cookie is returned to the browser and accepted back from the request body (`sessionId`) on data routes; the server then acts on the portal with whatever session ID the caller supplies. | `api/auth/login/route.ts`, `api/srmapi/fetch/route.ts`, `attendance/details/route.ts`, `exams/*` | High |
| S6 | Cache encryption | AES-256-**CBC without authentication** (no MAC/AEAD) — ciphertext is malleable and padding errors are distinguishable. | `encryptData`/`decryptData` in `server/utils/functions.ts` | Medium |
| S7 | Key derivation | PBKDF2-SHA256, 100 000 iterations, random 16-byte salt, key = student's portal password. Because the password travels in every JWT, the server can decrypt any cache at will; the scheme protects only against a database-only leak, and only as well as the password's strength. | `deriveKey` | Medium |
| S8 | Shared timetable collection | Each fetch also writes timetable + program/section/specialization to `college_db.timetables` **unencrypted**, deduplicated by SHA-256 of the content. Not personally named, but a section-level quasi-identifier, retained indefinitely and not removed on account deletion. | `api/srmapi/fetch/route.ts` (async block), `extractTimetable.ts` (`fetchTimetable`) | Medium |
| S9 | Retention | `users.data` and `attendanceHistory` (last 10 days) persist until the user deletes the account; no TTL. | `fetch/route.ts`, `handleUserSession.ts` | Medium |
| S10 | Deletion / unlink | `api/auth/delete` deletes the `users` document only; does not revoke tokens, clear `timetables` contributions or browser storage on other devices. | `api/auth/delete/route.ts` | Medium |
| S11 | Logging | ~60 `console.log/error` calls log raw error objects (e.g. `"Error From /api/srmapi/fetch:- ", err`). Axios errors include request config, which can contain the `Cookie` (portal session) header. | `src/server/**`, `src/app/api/**` | Medium |
| S12 | Authorization model | `isAdmin()` grants admin by a hardcoded list of student registration numbers; admin rights travel inside the long-lived JWT. | `server/utils/functions.ts` (`isAdmin`) | Medium |
| S13 | Input validation | Registration number check is only `/^AP/`. `/api/sync` builds `new RegExp(\`^${username}$\`)` from the token username (regex-injection pattern). | `validators/auth/login.ts`, `api/sync/route.ts` | Low |
| S14 | Bot-protection bypass | A bundled model solves the portal's login CAPTCHA; a **public** endpoint offers CAPTCHA solving to any caller. | `lib/captcha/captcha.ts`, `api/captcha/solve/route.ts` | Policy blocker |
| S15 | Portal write operations | Attendance marking, feedback submission and password reset are automated on the student's behalf. | audit §3.3 | Out of scope / policy |
| S16 | Deployment | CI deploys on push to `main` over SSH with password authentication; no tests or build gate. | `.github/workflows/deploy.yml` | Low (process) |
| S17 | Dead fallback | `fetchInternalMarks(sessionId \|\| user.session)` — `user.session` is never written (stored field is the encrypted `session_id`). | `exams/get*.ts`, `attendance/details/route.ts` | Low (correctness) |

## 2. Secure redesign principles for EduPulse

1. **EduPulse never receives, stores, forwards or logs a student's SRM portal password**, in any form
   (including encrypted, hashed or inside tokens). There is no "link with portal password" feature.
2. **No automated portal login and no CAPTCHA solving.** Live retrieval only through an institution-approved
   mechanism with its own authorization (e.g. an official API with scoped tokens or a sanctioned export feed).
3. **No portal session identifiers in EduPulse at all** — not in the browser, not in the database, not in logs.
4. **Student-controlled import**: the student downloads/saves their own report and uploads it; EduPulse parses,
   validates and shows a preview; nothing is stored until the student confirms.
5. **Data minimisation**: import only academic fields (data contract §1–6); drop DOB, gender, photo, instructor
   names and any non-academic data at parse time (`AcademicImportValidator.PROFILE_ALLOWLIST`).
6. **Consent and purpose**: explicit, versioned consent recorded before the first import; revocable at any time.
7. **Unlink = delete**: revoking consent deletes the student's imported records, import batches and derived
   snapshots (risk snapshots recomputed without them), and the action is audit-logged without academic content.
8. **Existing EduPulse token model stays**: short-lived JWT with server-side `token_version` revocation
   (already implemented); tokens carry no secrets. Imported data needs no client-side key.
9. **Encryption**: Neon/PostgreSQL encryption at rest + TLS in transit; if field-level encryption is added, use
   AES-GCM (authenticated) with a server-managed key from the secret store, never a key derived from a portal password.
10. **Logging**: log import outcome counts and batch IDs only; never raw HTML, marks, cookies or request configs.
11. **Retention**: raw uploaded HTML is never persisted (parse in memory, discard); normalized records kept while
    consent is active; import batches record a SHA-256 of the upload for duplicate detection only.
12. **Provenance**: every imported value carries its batch, source type (`STUDENT_MANUAL_IMPORT`) and the student's
    stated report date; the UI never labels it "live" or "verified" (`AcademicDataSourceType`).

## 3. Issues found in EduPulse's current integration code (same audit)

| # | Finding | Location | Severity |
|---|---|---|---|
| E1 | Aggregate attendance from an imported report is expanded into **invented daily records** (all presents first, absences last, ending today), and previous attendance is deleted on each import. This fabricates trends and destroys history. | `SrmPortalIntegrationService.syncAttendanceRecords` | High (data integrity) |
| E2 | Student-pasted HTML is labelled "Authentic SRM AP portal report", "verified institutional HTML parser pipeline", "synchronized". It is unverified and a manual import. | `IntegrationController.syncPortalReport`, `SrmPortalIntegrationService` messages/audit text | Medium (honesty) |
| E3 | `lastSyncTime`, `lastSyncStatus`, `lastSyncMessage`, row counters are instance fields of a singleton service: one student's import status is shown to all users. | `SrmPortalIntegrationService` | Medium |
| E4 | No size limit on pasted HTML fields. | `PortalReportSyncRequest` | Low |
| E5 | No consent record, import batch, duplicate-upload detection or "delete my imported data". | integration layer | Medium |
| E6 | The report's registration number is never read or compared with the logged-in student, so a student can import someone else's report into their own profile (data accuracy; imports still only write to the caller's own record). | `SrmPortalParserService`, `synchronizeFromHtmlReport` | Medium |

These are planned in [srmap-integration-roadmap.md](srmap-integration-roadmap.md) (Phase A).
