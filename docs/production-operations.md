# Production Operations & Deployment

## Backup and Recovery
- **Procedure:** Backups are safely executed mechanically against the Postgres container `docker exec edupulse-db pg_dump -U postgres edupulse > backup.sql`.
- **Restoration Validation:** Successfully dumped. Restorations follow the inverse `psql -U postgres -d edupulse < backup.sql` targeting isolated replica environments.
- **Key Recovery Limitations:** Private encryption keys (PBKDF2 derivations or WebCrypto raw AES exports) are entirely browser-held. Database backups capture exclusively AAD-bound AES-GCM ciphertext. A database backup *cannot* recover a student's risk history if their local keys are lost. Key/passphrases MUST NOT be appended to database artifacts.

## Deployment Details
- **Architecture:** Spring Boot executes the main REST monolith on port 8080. A PostgreSQL container persists state securely mapping port 5432. Vite statically builds a minified PWA tree containing `sw.js` and React bundles for static-hosting (e.g., Nginx).
- **Secrets Management:** JWT secrets and Postgres passwords must be passed strictly as `JWT_SECRET` and `DB_PASSWORD` environment variables in a container orchestrator. They are confirmed decoupled from `application.yml`.
- **Security Hardening (Phase 16):**
  - **CORS:** Controllers were stripped of dangerous global `@CrossOrigin(origins = "*")` defaults. CORS is securely restricted to strictly bound origins `${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}`.
  - **PWA Caching:** Verified that `vite.config.ts` forces Network-Only strategies for the backend REST routes to prevent token/payload bleed into ServiceWorker indexed caches.
