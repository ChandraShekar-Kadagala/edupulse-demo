# Phase 1 Baseline

## Verified Existing Architecture
- **Frameworks:** Java 21, Spring Boot 3.x, React, TypeScript, Vite.
- **Persistence:** PostgreSQL mapping `users`, `students`, `courses`, `attendance`, `marks`, `risk_assessments`.
- **Auth:** Spring Security JWT-based authentication in `AuthEntryPointJwt.java` and `JwtUtils.java`.

## Current Plaintext Exposure
- `RiskAssessment` records risk scores as plaintext doubles.
- `Mark` and `Attendance` records contain plaintext student performance data.
- API endpoints return these models unencrypted.

## Integration Defect
- The Official SRM AP data provider is correctly isolated and returning `UNSUPPORTED_ACCESS` due to the lack of an authorized institutional API.

## Implementation Plan (Phases 2-6)
1. Write the frontend risk logic to remove server dependency.
2. Build AES-GCM Web Crypto modules for browser-side encryptions.
3. Migrate `RiskAssessment` into a separate `EncryptedRiskRecord` entity.
4. Protect APIs with proper owner-validation rules.
5. Provide a memory-resident encryption key UI that never persists raw keys locally.
