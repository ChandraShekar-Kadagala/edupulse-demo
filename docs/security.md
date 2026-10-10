# EduPulse Security & Privacy Review

## 1. Authentication & Authorization
- **Role-Based Access Control (RBAC):** Implemented using Spring Security. Roles include `ADMIN`, `FACULTY`, and `STUDENT`.
- **JWT Tokens:** Used for session management.
  - *Risk:* JWT tokens must be stored securely. Currently, the frontend uses standard browser storage. For the future mobile app, `expo-secure-store` is mandated.
  - *Mitigation:* Tokens are short-lived, and secrets are strictly managed via environment variables (`JWT_SECRET`).
- **Data Ownership:** 
  - Students are explicitly restricted from viewing other students' risk assessments or academic histories.
  - *Unverified:* The backend Java checks for object-level ownership (`@PreAuthorize("hasRole('ADMIN') or #studentId == principal.id")`) are written but unverified due to the current inability to compile the Java backend.

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

## 5. Unresolved Risks & Unverified Components
- **Backend Compilation Blocker:** Because `JAVA_HOME` is missing in the current build environment, all Java security annotations (`@PreAuthorize`), transaction boundaries, and CSV validation logic remain uncompiled and unverified. **Do not deploy EduPulse to production until the backend passes a full Maven test suite.**
- **Audit Logging:** Comprehensive audit logging for data imports is currently rudimentary (basic counts) and should be expanded to track individual row changes by user ID.
