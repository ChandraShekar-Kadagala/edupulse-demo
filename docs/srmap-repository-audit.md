# Srmap-Api repository audit

| | |
|---|---|
| Repository | https://github.com/StoreVia/Srmap-Api |
| Commit audited | `290a88f` (2026-11-13, "Updated workflow"), package `srmapi.next` v5.12.0 |
| Audit date | 2026-11-14 |
| Method | Static reading of source in an isolated directory. Nothing was installed, built or executed; no request was sent to the SRM AP portal; no student account was used. |
| Purpose | Decide what EduPulse can learn from or reuse for student academic-data import. |

Related: [srmap-data-contract.md](srmap-data-contract.md) · [srmap-security-review.md](srmap-security-review.md) ·
[account-linking-architecture.md](account-linking-architecture.md) · [srmap-integration-roadmap.md](srmap-integration-roadmap.md)

## 1. What the repository is

A Next.js 16 / React 19 web app (PWA) with Next.js API routes as its backend, MongoDB for storage and
Backblaze B2 for file sharing. It logs into the SRM AP Student Corner portal **on the student's behalf** using the
student's portal username and password, solves the portal login CAPTCHA with a bundled neural model, keeps the
portal session ID, and scrapes report pages with Cheerio. It also contains unrelated features (arcade, resources
hub, secure file share, vacant-room finder) and a `deprecated/` tree (movies, forums, Omegle clone).

It is **not** an official or institution-sanctioned API. No document in the repository references an agreement with
SRM University AP.

## 2. Differences from the audit brief

| Brief expected | Actually present at `290a88f` |
|---|---|
| `python/api.py`, FastAPI service, Python requirements | **Not present.** No Python file and no `requirements*.txt` exist in the repository. |
| ONNX model + metadata | **Not present.** CAPTCHA inference uses a TensorFlow Lite model, `src/static/captcha/model/captcha_float32.tflite` (1.3 MB), run in Node via `tfjs-tflite-node` + `sharp` from `src/lib/captcha/captcha.ts`. |
| Database models | No schema/ODM. Collections are used ad hoc through the MongoDB driver (`src/lib/database/mongodb.ts`). |
| `src/server/auth`, `src/server/srmapi`, `src/app/api/auth`, `src/app/api/srmapi` | Present (inventory below). |

## 3. Source-file inventory (in-scope modules)

### 3.1 Portal authentication and session
| File | Key functions | Responsibility |
|---|---|---|
| `src/server/auth/login.ts` | `attemptLogin`, `login` | GET login page → read `JSESSIONID` from `set-cookie` → GET `/captchas` image → `solveCaptcha()` → POST `StudentLoginToPortal` with `txtUserName`, `txtAuthKey` (password), `ccode`. Success is inferred from an `<h2>` in the response HTML. Retries via `callApiWithRetry(…, 2)`. |
| `src/server/auth/handleUserSession.ts` | `handleUserSession` | Calls `login`; on success upserts `college_db.users` with `session_id` encrypted using the **student's password**; on portal failure tries to decrypt cached data to offer "cached mode". |
| `src/server/auth/verifyUser.ts` | `validUser` | Verifies the Bearer JWT; payload type `{ username, password, token }`; rejects only if `password === ""`. |
| `src/app/api/auth/login/route.ts` | `POST` | Turnstile check, reg-number check (`/^AP/` only), blocked-user check, then either cached login or `handleUserSession`. Returns `accessToken` (JWT containing the password) **and the portal `sessionId`** to the browser. |
| `src/app/api/auth/delete/route.ts` | `POST` | Deletes the `users` document for the token's username and posts an admin notification. |
| `src/app/api/auth/forgot/route.ts` | `POST` | Drives the portal's password-reset flow (`StudentPasswordResetInitiate`). Portal write operation. |
| `src/app/api/srmapi/initiate/session/route.ts` | `POST` | Re-establishes a portal session for an authenticated user. |
| `src/validators/auth/login.ts` | `isValidRegNumber` | Only checks the `AP` prefix. |
| `src/server/utils/headers.ts` | `main`, `captcha`, `authenticate`, `basic` | Browser-like request header sets for portal calls. |
| `src/server/utils/functions.ts` | `createClient`, `deriveKey`, `encryptData`, `decryptData`, `createToken`, `verifyToken`, `requireAuthResponse`, `isBlocked`, `isAdmin` | Axios client bound to a session cookie; PBKDF2 + AES-256-CBC cache encryption; JWT issue/verify (expiry `ACCESS_EXPIRE` days); admin check against a hardcoded list of student registration numbers. |
| `src/shared/utils/functions.ts` | `isSessionValid`, `getTime`, `getFullDateAndTime` | Portal session considered valid for 15 minutes after `session_time` (IST). |

### 3.2 Portal data retrieval and parsing (read-only)
| File | Key functions | Portal resource (`studentreportresources.jsp`, form field `ids`) | Output |
|---|---|---|---|
| `src/server/srmapi/fetchData.ts` | `fetchFromWebsite` | `HRDSystem` (home), `ids=1` profile, `2` subjects, `3` attendance, `6` CGPA, `10` timetable — fetched in parallel | `WebsiteData { attendance, timetable, subjects, profile, cgpa, name, id }` |
| `src/server/srmapi/exams/fetchInternals.ts` | `fetchInternalMarks`, `splitPair` | `ids=5` | `InternalData { last_fetched, subjects[ { exams[] } ] }` |
| `src/server/srmapi/exams/fetchPastInternals.ts` | (fetcher) | `ids=22` semester list, `ids=23` + `filter` per semester | `PastInternalsData { semesters, rows, selected_semester }` |
| `src/server/srmapi/exams/fetchExamLedger.ts` | `fetchExamLedger` | `ids=6` | `ExamLedgerData { last_fetched, cgpa, rows[] }` |
| `src/server/srmapi/utils/fetchCheckAttendance.ts` | `fetchAttendance` | `studentattendance.jsp`, `ids=33` | Per-period attendance `{ date, day, hour, subject, status }[]` |
| `src/server/srmapi/utils/extractTimetable.ts` | `fetchTimetable` | (derived from `WebsiteData`) | Timetable + subjects + non-identifying profile subset |
| `src/server/srmapi/exams/get*.ts` | `getInternalsForUser`, `getExamLedgerForUser`, `getPastInternalsForUser` | — | Session-validity gate around the fetchers |
| `src/app/api/srmapi/fetch/route.ts` | `POST` | — | Fetch → encrypt → cache in `users.data`; append a daily encrypted attendance snapshot (`attendanceHistory`, last 10); write timetable to shared `timetables` collection |
| `src/app/api/srmapi/attendance/{details,history}/route.ts`, `exams/{internals,past-internals,semester-results}/route.ts` | `POST`/`GET` | — | Thin API wrappers |

### 3.3 Portal write operations (out of EduPulse scope)
| File | Portal endpoint | Operation |
|---|---|---|
| `src/app/api/srmapi/attendance/mark/route.ts` | `students/transaction/studentattendanceresources.jsp` | Marks attendance |
| `src/server/srmapi/feedback/feedbackSubmit.ts`, `feedbackComments.ts`, `feedbackSubjects.ts` | `subjectwisefeedback*.jsp` | Submits course feedback |
| `src/app/api/auth/forgot/route.ts` | `StudentPasswordResetInitiate`, `loginmanagerresources.jsp` | Password reset |

### 3.4 CAPTCHA component
| File | Responsibility |
|---|---|
| `src/lib/captcha/captcha.ts` | `getCaptchaModel`, `preprocessCaptcha`, `decodeCaptchaOutput`, `solveCaptcha` — loads the TFLite model and returns the predicted CAPTCHA text. |
| `src/static/captcha/model/captcha_float32.tflite`, `src/static/captcha/captchaModel.ts` | Model file and loader reference. |
| `src/app/api/captcha/solve/route.ts` | **Public** solving endpoint: accepts an uploaded CAPTCHA image and returns the text, rate-tracked per IP (`recordCaptchaRequest`), with an admin kill switch. Not behind user authentication. |
| `src/server/captcha/captchaAdmin.ts`, `src/app/api/admin/captcha/**` | Usage statistics, per-IP blocking, enable/disable. |

ML-engineering summary (deliberately not a reproduction guide): a single-file float32 TFLite classifier run
in-process on Node 22 with `sharp` for image preparation; no separate service, GPU or queue; a failed solve makes the
login attempt throw a generic "SRM server is unreachable" error and `callApiWithRetry` retries once.
**Purpose: defeating the portal's bot-protection on login.** EduPulse must not adopt, port, call or host this
component (see the security review §4 and architecture §2).

### 3.5 Storage, configuration and deployment
| File | Notes |
|---|---|
| `src/lib/database/mongodb.ts`, `useMongo.ts` | Global MongoClient. Collections used: `college_db.users` (`username`, `session_id`, `session_time`, `data`, `attendanceHistory`, `settings`, `createdAt`), `blocked`, `settings`, `notifications`, `timetables`; secondary DB for secure share. |
| `.env.example` | `ACCESS_SECRET`, `ACCESS_EXPIRE=365`, `MONGO_URI`, Turnstile keys, B2 keys. |
| `ecosystem.config.js` | PM2 cluster mode, port 6767. |
| `.github/workflows/deploy.yml` | On push to `main`: SSH (password auth) to a server and run `deploy.sh`. No tests, lint or build in CI. |
| `src/context/LocalStorageContext.tsx`, `src/hooks/context/useLocalStorage.tsx`, `useAuthActions.tsx` | Browser `localStorage` holds `accessToken` and portal `sessionId` for up to 5 accounts; `useAuthActions` decodes the JWT and checks `decoded.password`. |

### 3.6 Out of scope (not audited in depth)
Arcade/chess/typing test, resources hub, secure share (B2), faculty directory and ratings, vacant rooms,
`deprecated/**`, UI components, PWA service worker.

## 4. Responsibility summary

| Concern | Where it lives | Reusable by EduPulse? |
|---|---|---|
| Knowledge of which portal reports exist and their table shapes | `fetchData.ts`, `fetchInternals.ts`, `fetchExamLedger.ts`, `fetchPastInternals.ts` | **Yes, as documentation** — informs the import data contract and parser fixtures. |
| HTML parsing logic (Cheerio selectors, column orders) | same | **Redesign** — EduPulse already has a Java/jsoup parser (`SrmPortalParserService`); port column knowledge, not code. |
| Daily aggregate attendance snapshots for trend history | `fetch/route.ts` (`attendanceHistory`) | **Yes, as a pattern** — EduPulse should store dated aggregate snapshots instead of fabricating per-day rows. |
| Automated portal login, session capture | `server/auth/*` | **No** — requires institutional authorization; never with the student's password on EduPulse servers. |
| CAPTCHA solving | `lib/captcha/*`, `api/captcha/solve` | **No** — bypasses portal protection. |
| Token/session/cache security model | `server/utils/functions.ts`, browser storage | **No** — see security review. |
| Portal write operations | §3.3 | **No** — EduPulse is read-only. |
| MongoDB storage | `lib/database/*` | **No** — EduPulse stays on PostgreSQL/Neon. |
