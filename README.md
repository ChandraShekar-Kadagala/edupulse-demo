# EduPulse

[![CI](https://github.com/ChandraShekar-Kadagala/edupulse-demo/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/ChandraShekar-Kadagala/edupulse-demo/actions/workflows/ci.yml)
**Early Academic Risk Detection & Intervention Intelligence Platform**

EduPulse is a secure, role-based platform designed to identify students who may be experiencing academic difficulties early enough for supportive action.

## Features
- **Explainable Risk Engine:** Deterministic rule-based scoring (0-100) based on attendance thresholds and continuous assessment marks.
- **Data Import (CSV):** Secure ingestion of academic records.
- **Intervention Lifecycle:** Support plans, tracking, and faculty follow-ups.
- **Progressive Web App (PWA):** Installable web experience with offline visual state recovery.
- **Data Privacy & Boundaries:** Strong backend validation via strict DTO structures and token lifecycle limits ensures student data remains fully compartmentalized.

---

## 🛠️ Windows Setup & Running Locally

### Prerequisites
1. **Java 21 (OpenJDK)**
2. **Node.js (v18+)**
3. **Docker Desktop** (for PostgreSQL)

### 1. Database (PostgreSQL)
From the project root (`c:\javapr\edupulse`):
```powershell
docker-compose up -d
```
*This binds PostgreSQL to `localhost:5432` with username `postgres`; the password comes from the `DB_PASSWORD` environment variable.*

### 2. Backend (Spring Boot)
Open a new PowerShell terminal and manually inject the `JAVA_HOME` environment variable to point to your JDK installation (adjust path if necessary):
```powershell
cd c:\javapr\edupulse\backend
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

### 3. Frontend (React / Vite)
Open a separate PowerShell terminal:
```powershell
cd c:\javapr\edupulse\frontend
npm install
npm run dev
```
For production build simulation:
```powershell
npm run build
npm run preview
```

---

## 🔐 Environment Variables & Secrets Management
Do not hardcode secrets in production.
**Backend environment variables** (names only; set values in your shell, `.env` (git-ignored) or the Render dashboard):
* `JDBC_DATABASE_URL` *or* `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
* `JWT_SECRET`: Base64-encoded 256-bit key
* `SPRING_PROFILES_ACTIVE`: `prod` on Render; omit locally for the PostgreSQL default profile, or `dev` for in-memory H2
* `APP_CORS_ALLOWED_ORIGINS`: comma-separated frontend origins
* `SEED_DEMO_DATA`: `true` to provision the demo showcase on startup (default `false`)
* `DEMO_USER_PASSWORD`: password shared by the six public demo accounts
* `DEMO_ADMIN_PASSWORD`: optional, **non-public** password for a demo admin; if unset, no admin is seeded
* `FLYWAY_ENABLED` (default `true`), `JPA_DDL_AUTO` (default `validate`)

**Schema management:** Flyway owns the schema in every PostgreSQL environment (`backend/src/main/resources/db/migration`); Hibernate only validates. Add forward-only `V{n}__description.sql` migrations; never edit an applied one. The H2 `dev`/`test` profiles let Hibernate build the schema, and `PostgresMigrationIntegrationTest` verifies the real migrations against PostgreSQL in CI.

---

## 👥 Demo Showcase Accounts
When `SEED_DEMO_DATA=true` (or an admin calls `POST /api/admin/demo/provision`), `DataSeeder` idempotently provisions six public demo accounts (auth provider `DEMO`). Their credentials are listed on the login page.

| Username | Role | Data |
|---|---|---|
| `demo.student.a` | Student (STU-1001) | Anonymized student-supplied reference data |
| `demo.student.b` | Student (STU-1002) | Synthetic: top performer |
| `demo.student.c` | Student (STU-1003) | Synthetic: average performer |
| `demo.student.d` | Student (STU-1004) | Synthetic: high risk |
| `demo.faculty.1` | Faculty (CSE 204) | Synthetic |
| `demo.faculty.2` | Faculty (CSE 213) | Synthetic |

There is no public admin account. Real (non-`DEMO`) accounts are never modified by provisioning.

---

## ✅ Tests
```powershell
cd backend; .mvnw.cmd test          # unit + Spring integration (+ PostgreSQL Testcontainers when Docker is running)
cd frontend; npm run lint; npm test; npm run build   # build = tsc -b && vite build
```
On Windows with Docker Desktop 29+, set `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine` so Testcontainers finds Docker; without Docker the PostgreSQL test is skipped locally (it always runs in CI).

---

## 📄 CSV Import Format
The `/api/import/academic-data` endpoint strictly expects the following CSV format.
Duplicate records are safely ignored via composite DB constraints and transaction rollbacks.

```csv
StudentId,CourseId,AttendancePercentage,AssessmentName,MarksObtained,MaxMarks
STU-1001,CS101,65.0,Midterm,45.0,100.0
STU-1002,CS101,95.0,Midterm,85.0,100.0
```

---

## 📈 Risk Scoring
See [docs/risk-engine.md](docs/risk-engine.md). The formula lives only in `RiskFormula.java` (backend); the frontend renders the backend explanation.

---

## 🚀 Deployment Requirements
* **Reverse Proxy / HTTPS:** You must use an NGINX proxy or AWS ALB to terminate TLS (HTTPS). Service worker (PWA) requires a secure context.
* **Database Migrations:** Use a tool like Flyway or Liquibase in production instead of `hibernate.ddl-auto=update`.
* **Backup/Restore:** Run daily `pg_dump` jobs for PostgreSQL. Store encrypted backups in S3.
* **Integration Strategy:** Official SRM AP account synchronization remains **disabled** until an authorized integration contract (via API or secure data export) is confirmed to avoid credential harvesting risks.

---

## ⚠️ Known Limitations & Troubleshooting
* **Rate Limiting:** Password reset token generation and login endpoints currently lack explicit bucket-based rate limiting (e.g., Bucket4j). Enforce rate limits at the NGINX ingress level to prevent account enumeration.
* **Email Delivery:** SMTP is mocked. Reset links are dumped to `System.out` in `dev` profile.
* **Proxy Serialization:** If new JPA mappings are added, ensure they map strictly to DTOs. Direct entity return causes `ByteBuddyInterceptor` serialization crashes.
* **Offline PWA limitation:** Academic API responses are purposefully excluded from the Workbox cache to preserve student privacy. Offline states trigger a red banner warning instead.
