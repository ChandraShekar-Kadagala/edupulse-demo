# Privacy Acceptance Report

## Implementation Status
| Requirement | Status | Evidence & Actual Source Files |
| :--- | :--- | :--- |
| Phase 1: Baseline Audit | PASS | `docs/privacy-phase1-baseline.md`. Legacy plaintext tables remain intact and explicitly documented. |
| Phase 2: TS Risk Engine | PASS | `frontend/src/utils/riskEngine.ts`. 100% parity verified via `frontend/src/utils/riskEngine.test.ts`. |
| Phase 3: Client Encryption & Versioning | PASS | AES-GCM (AAD) implemented in `frontend/src/utils/crypto.ts`. Version 1.1 correctly strictly validates AAD. Version 1.0 gracefully degrades (no AAD) and is unit tested. |
| Phase 4: Passphrase Recovery Validation | PASS | `importKeyWithPassphrase` enforces 100,000 - 500,000 PBKDF2 iterations, 16-byte salt, 12-byte IV, and 48-byte wrapped payload bounds to prevent unbounded work/memory exhaustion. UI reload recovery validated. |
| Phase 5: Encrypted Persistence & Ownership | PASS | `EncryptedRiskRecord.java` stores purely ciphertext/metadata. `PrivacyApiController.java` derives ownership strictly from `@AuthenticationPrincipal` (backend token trust), entirely ignoring client-provided owner IDs. Tests in `EncryptedPersistenceTest.java` prove cross-student isolation. |
| Phase 6: Privacy UI & Security | PASS | Recovery package export implemented in `frontend/src/pages/PrivacySetup.tsx`. `index.html` implements `Content-Security-Policy`. `frontend/vite.config.ts` prevents Workbox runtime cache for `/api/.*`. |

## Build and Test Results
- **Backend Tests (`mvnw test`):** 12 tests run, 0 failures, 0 errors. Exit code 0.
- **Frontend Tests (`npx vitest run`):** 15 tests run (across 2 files: `riskEngine.test.ts`, `crypto.test.ts`), 0 failures. Exit code 0.
- **Frontend Build (`npm run build`):** Completed successfully. Exit code 0.

## PBKDF2 Iteration Policy & Benchmarks
The PBKDF2 work factor is hardcoded to 100,000 iterations.
* **Trade-off:** OWASP recommends 600,000 for server-side hashing. However, in a browser (single-threaded Web Crypto UI blocking), 600,000 causes significant UI hangs on lower-end mobile devices. 100,000 iterations provides a safe baseline resistance against offline cracking for strong passphrases while keeping the UI responsive (sub-1-second recovery on average mobile devices).

## Remaining Risks & Blockers
- **Legacy Plaintext Exposure:** `RiskAssessment`, `Attendance`, and `Marks` tables still store legacy plaintext data. This is safely deferred pending explicit administrative migration approval.
- **Production CSP:** The HTML meta tag provides a baseline CSP, but actual HTTP response headers must be configured at the production hosting/proxy layer for optimal security against XSS.
- **Official API Sync:** Blocked due to lack of official university integration endpoint. `UNSUPPORTED_ACCESS` accurately returned.
