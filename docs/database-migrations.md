# Database migrations

Flyway owns the PostgreSQL schema (`backend/src/main/resources/db/migration`). Hibernate runs with
`ddl-auto=validate` and never changes the schema. Every push to `main` deploys the backend to Render,
and the backend applies any pending migration to the production Neon database **on startup**.

Rules:
- Forward-only `V{n}__description.sql`. Never edit or delete an applied migration.
- No `DROP TABLE`, `DROP COLUMN`, `TRUNCATE` or `DELETE` of existing data without explicit owner approval.
- Each migration must pass `PostgresMigrationIntegrationTest` (fresh PostgreSQL) and
  `LegacySchemaV2MigrationTest`-style rehearsal against the legacy production schema shape.

## History

| Version | File | Summary |
|---|---|---|
| 1 | `V1__Baseline.sql` | Baseline schema (pg_dump). Existing production was created by Hibernate before Flyway and was **baselined** at V1 (V1 never ran there). |
| 2 | `V2__course_assignments_provenance_roles.sql` | Faculty-course assignments, stored student data provenance, wider role constraint. |

## V2 review record (2026-11-13)

### What V2 changes
| Statement | Effect on existing data |
|---|---|
| `CREATE TABLE course_assignments` + 2 indexes | None (new table). |
| `INSERT INTO course_assignments` | Two rows, only for DEMO faculty `demo.faculty.1`→CSE 204 and `demo.faculty.2`→CSE 213. |
| `ALTER TABLE students ADD COLUMN data_provenance` | New nullable column; existing columns untouched. |
| `UPDATE students SET data_provenance` | Writes only the new column, only for the 4 DEMO showcase students. |
| Replace role `CHECK` on `users` | Strictly wider (`ADMIN, FACULTY, STUDENT` + `MENTOR, HOD, GUARDIAN`). No row changes; no role granted. |

### Built-in safety checks (inside the migration transaction)
1. **Preconditions** – required tables/columns exist, FK targets are primary keys, the new table/column do
   not already exist, and every current role value is allowed by the new constraint. Otherwise `RAISE`.
2. **Data-preservation fingerprint** – before any change, every pre-existing table is fingerprinted
   (row count + md5 over each row's JSON, ignoring only the new column). After the changes the fingerprint
   is recomputed; any difference raises and rolls back.
3. **Atomicity** – Flyway runs V2 in a single PostgreSQL transaction (DDL is transactional). On any error
   nothing is applied, nothing is recorded in `flyway_schema_history`, and the new application version fails
   its health check so Render keeps the previous version serving.

### Evidence (automated, runs in CI)
| Check | Test | Result |
|---|---|---|
| Fresh PostgreSQL 15: V1 + V2 apply, entities validate, repositories work | `PostgresMigrationIntegrationTest` | pass |
| Legacy production shape (Hibernate-created schema from a 2026-10 pg_dump, schema only, synthetic rows), Flyway baselined at V1 exactly like production: V2 applies, app starts with `validate`, all pre-existing rows byte-identical, history = BASELINE 1 + V2 success, only DEMO rows labelled/assigned | `LegacySchemaV2MigrationTest.v2AppliesToLegacySchemaAndPreservesData` | pass |
| Precondition failure rolls back completely (no column, no history row, data identical) | `...preconditionFailureRollsBackEverything` | pass |
| Fingerprint guard aborts when an unexpected trigger rewrites an existing column | `...dataPreservationGuardDetectsUnexpectedChanges` | pass |
| Previous release (`0e5950c`, prod profile) starts against a V2 database | manual, local PostgreSQL 15 | pass |

### Limitations of the review
- The production Neon database could not be inspected directly (no credentials on the review machine).
  Compatibility is established by the legacy-schema rehearsal plus the in-transaction precondition checks,
  which abort before any change if production differs from what V2 expects.
- No Neon branch/snapshot was created by the reviewer. Existing data is protected by the in-transaction
  fingerprint guarantee (V2 cannot commit if any pre-existing value changed). The owner can additionally
  use Neon's point-in-time restore / branching from the Neon console.

### Rollback and recovery
- **Code rollback** (preferred): redeploy the previous commit in Render. Verified compatible with a V2 schema
  (unknown newer migrations are ignored by Flyway; Hibernate ignores the extra table/column).
- **Schema rollback**: not needed for safety (V2 is additive). If ever required, write a reviewed forward
  migration (V3) that removes only what V2 added. That is **destructive to V2-created data**
  (course assignments, provenance labels) and is not a "safe rollback" — it needs owner approval.
- **Disaster recovery**: Neon point-in-time restore or branch from before the deploy time (owner action in the
  Neon console). This is the only mechanism that restores a previous database state.

### Post-deploy verification
- `GET /health` reports `"schemaVersion": "2"` (public, no data exposed).
- `GET /api/auth/demo-status` lists faculty `courses` read from `course_assignments` and provenance read
  from `students.data_provenance`.
