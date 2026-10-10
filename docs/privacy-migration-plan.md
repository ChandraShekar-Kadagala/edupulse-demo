# Privacy Migration Plan

## Legacy Data Handling
Legacy data currently in plaintext will remain readable for existing workflows during the transition period. A migration tool will be provided to optionally encrypt legacy data if the student generates a key and opts in.

## New Data
All new risk records will be created using the new privacy-first architecture, ensuring they are encrypted client-side before being sent to the backend.

## Database Changes
- Add a new column `encrypted_payload` (or use a new table `encrypted_risk_records`).
- Add metadata columns needed for listing (e.g., `student_id`, `created_at`).
- Existing plaintext columns can be deprecated eventually.

## Application Rollout
1. Deploy database schema changes.
2. Deploy backend API updates (Privacy API).
3. Deploy frontend with encryption capabilities.
4. Encourage users to generate and securely store their encryption keys.
