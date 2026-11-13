# Proposed production migrations (awaiting approval)

Every push to `main` auto-deploys the backend to Render, and production Flyway applies any pending
migration in `backend/src/main/resources/db/migration/` against the Neon database on startup. Schema
changes are therefore staged here first and only moved into that folder after the owner approves.

## V2 — `V2__course_assignments_provenance_roles.sql`

| Change | Effect on existing data | Why |
|---|---|---|
| `CREATE TABLE course_assignments` | none (new table) | Faculty dashboards are currently driven by username matching and invented fallback records; real assignments are required for correct faculty data (Phase 2/3). |
| Backfill 2 rows | inserts assignments for `demo.faculty.1` → CSE 204 and `demo.faculty.2` → CSE 213, only for `DEMO` accounts | Preserves current showcase behaviour. |
| `students.data_provenance` (nullable) | new column; labels only the 4 `DEMO` showcase students | Provenance is stored, not inferred (Phase 3 requirement to distinguish reference / synthetic / imported data). |
| Replace `users_role_check` | widens allowed values; no row changes | Enables MENTOR / HOD / GUARDIAN roles (Phase 10). No role is granted. |

**Not included:** no `DROP TABLE`, `TRUNCATE`, `DELETE`, column drops or type narrowing.

### Review checklist
1. Take a Neon branch/snapshot (Neon console → Branches → create from `main`) before deploying.
2. Confirm `SELECT count(*) FROM flyway_schema_history;` shows V1 as the only applied version.
3. Approve, then the migration file is moved into `db/migration/`, verified by
   `PostgresMigrationIntegrationTest` (fresh PostgreSQL) in CI, and deployed.

### Rollback
Flyway is forward-only. If V2 must be reverted, deploy the previous commit **and** run (owner, manually):
```sql
DROP TABLE IF EXISTS course_assignments;
ALTER TABLE students DROP COLUMN IF EXISTS data_provenance;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN','FACULTY','STUDENT'));
DELETE FROM flyway_schema_history WHERE version = '2';
```
Or restore the Neon branch taken in step 1.
