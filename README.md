# EduPulse
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
*This binds PostgreSQL to `localhost:5432` with username `postgres` and password `password`.*

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
**Backend Config (`application-postgres.yml` or ENV vars):**
* `SPRING_DATASOURCE_URL`: `jdbc:postgresql://localhost:5432/edupulse`
* `SPRING_DATASOURCE_USERNAME`: `postgres`
* `SPRING_DATASOURCE_PASSWORD`: (Inject via secret manager)
* `EDUPULSE_JWT_SECRET`: A secure Base64 256-bit key (e.g. `export EDUPULSE_JWT_SECRET=super-secure-key`)
* `APP_SEEDER_ENABLED`: `true` to populate demo accounts on startup.

---

## 👥 Synthetic Demo Accounts & Startup Behavior
When `app.seeder.enabled=true`, the backend leverages `DataSeeder.java` to inject mock data.
* **Admin:** `admin` / `password`
* **Faculty:** `faculty1` / `password` (Name: Dr. Smith)
* **Student:** `arun` / `password` (Registration: STU-1001, demonstrates high-risk trajectory)
* **Student B:** `bob` / `password` (Registration: STU-1002, demonstrates low-risk trajectory)

*Note on Safe Startup:* The `spring.jpa.hibernate.ddl-auto` is set to `update`, which prevents destructive schema changes.

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
The `RuleBasedRiskEngineImpl` calculates risk dynamically based on standard deviations:
1. Iterates over all imported `Attendance` records.
2. Iterates over all imported `Marks` records.
3. Automatically triggers HIGH RISK if average attendance drops below 75% or aggregate marks drop below 50%.
4. Emits a historical snapshot (`RiskAssessment`) preventing constant data mutation.

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
