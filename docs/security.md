# EduPulse Security & Privacy Review

## 1. Authentication & Authorization
- **Role-Based Access Control (RBAC):** Implemented using Spring Security. Roles include `ADMIN`, `FACULTY`, and `STUDENT`.
- **JWT Tokens:** Used for session management.
  - *Risk:* JWT tokens must be stored securely. Currently, the frontend uses standard browser storage. For the future mobile app, `expo-secure-store` is mandated.
  - *Mitigation:* Tokens are short-lived, and secrets are strictly managed via environment variables (`JWT_SECRET`).
- **Data Ownership:** 
  - Students are explicitly restricted from viewing other students' risk assessments or academic histories.
  - Object-level ownership checks (`@PreAuthorize("... or #id == principal.id")`) are compiled and covered by integration tests (`IntegrationSyncTests.testCrossStudentAccessForbidden`).
- **Public endpoint allow-list:** only `/api/auth/login`, `/api/auth/forgot-password`, `/api/auth/reset-password`, `/api/auth/demo-status`, `/api/auth/logout` and `/health` are anonymous. There is deliberately no `/api/auth/**` wildcard (regression-tested in `DemoProvisioningSecurityTests`).
- **Login identifiers:** username or student registration number only. The former hidden alias table (informal names mapped onto demo accounts) was removed.

## 1a. Demo Showcase Accounts
- Six public demo accounts (4 students, 2 faculty), auth provider `DEMO`, share the public demo password (`DEMO_USER_PASSWORD`). They exist only to demonstrate the product and hold showcase data.
- **No public admin.** The demo admin is created only when a separate, non-public `DEMO_ADMIN_PASSWORD` is configured, and is never shown on the login page.
- **Provisioning** is `POST /api/admin/demo/provision`: requires `ADMIN`, returns 409 when `SEED_DEMO_DATA=false`, is idempotent, and only creates/refreshes `DEMO` accounts. It refuses (409) to renumber a non-demo student that holds a demo registration number. The former anonymous `POST /api/auth/provision-demo-showcase` no longer exists.

## 2. CSV Upload & Data Integrity
- **Validation:** 
  - Browser-side validation exists for immediate UX feedback.
  - Backend validation strictly enforces bounds (e.g., Attendance between 0 and 100).
- **Transactions:** 
  - The `CsvImportService` operates within a `@Transactional` boundary to prevent partial database writes.
  - Duplicate detection ensures existing academic records are not silently overwritten.
- **Authorization:** Only `ADMIN` and `FACULTY` roles can access the `/api/v1/data-import/csv` endpoint.

## 3. Academic Data Integrations
- **SRM AP Integration:** The official integration (`OfficialSrmDataProvider`) is explicitly disabled. 
- **Privacy Stance:** EduPulse strictly forbids unauthorized web scraping or the collection/storage of plaintext student portal passwords. We do not use third-party unofficial API wrappers due to inherent credential theft risks.

## 4. PWA Caching
- **Offline Safety:** The `vite-plugin-pwa` Service Worker is explicitly configured to *not* cache authenticated API responses (`/api/v1/*`). This ensures that sensitive student academic data is not exposed or leaked when the device is offline or shared.

## 5. Verification & Remaining Risks
- **Build status:** the backend compiles and the full Maven suite (unit, Spring integration, PostgreSQL Testcontainers migration test) runs in GitHub Actions on every push and pull request (`.github/workflows/ci.yml`).
- **Production follow-up (operator action):** if the production `admin` account was ever provisioned by the old seeder, its password equals the public demo password. Rotate it with the forgot/reset-password flow or a direct password-hash update by the database owner. EduPulse code no longer creates or resets it.
- **Audit Logging:** audit logging for data imports records counts; per-row change tracking by user ID is still future work.
