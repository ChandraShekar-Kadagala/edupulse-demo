# SRM AP Portal Integration Feasibility Report

## 1. Objective
Assess the technical and legal feasibility of integrating the official SRM University AP student portal (or related academic data sources) directly into EduPulse to automate the ingestion of attendance, assessments, and marks.

## 2. Identified Data Sources
1. **Official Student Portal:** `https://student.srmap.edu.in/srmapstudentcorner/HRDSystem`
2. **Official University Site:** `https://www.srmap.edu.in/students/`
3. *Unofficial/Community Services:* `https://oursrmap.purlyedit.in/`, `https://srmapi.in/`

## 3. Feasibility & Security Analysis

### Unofficial APIs (REJECTED)
Community-built APIs (such as `srmapi.in` or `oursrmap`) may provide convenient JSON wrappers around the official portal. However, utilizing these in a production support platform is strictly prohibited for the following reasons:
- **Authorization:** These services are not officially sanctioned by the university administration.
- **Privacy:** Routing student credentials or academic data through a third-party, unofficial middleman violates basic data privacy principles.
- **Stability:** These wrappers rely on web scraping, which breaks whenever the official portal updates its HTML structure.

### Official Portal Scraping (REJECTED)
Building a custom web scraper that takes a student's portal credentials to log in and extract HTML data is rejected:
- **Security:** EduPulse would be forced to collect, transmit, and potentially store plaintext portal passwords. This is an unacceptable security risk.
- **Access Controls:** Automated scraping often bypasses intended rate limits, MFA (Multi-Factor Authentication), and CAPTCHA requirements, violating the portal's terms of service.

### Official University API / Export (RECOMMENDED - PENDING APPROVAL)
The only acceptable method for automated integration is an official agreement with the university's IT or academic department.
- **Mechanism:** OAuth2 or API Key-based access to a university-provided REST API, or automated secure SFTP drops of CSV/XML exports.
- **Current Status:** **NOT CONFIGURED / UNAVAILABLE**. There is currently no publicly documented, authorized API provided by SRM AP for third-party platforms.

## 4. Conclusion & Action Plan
Until official authorization and a technical API are provided by the university, **automated SRM AP integration remains disabled**. 

The `OfficialSrmDataProvider` stub in the backend is intentionally hard-coded to throw a `NotConfiguredException`.

**Interim Solution:**
EduPulse will rely exclusively on the `CsvAcademicDataProvider`. Authorized faculty and administrators must use the **Academic Data Import** workflow to manually upload verified, exported CSV records into the system. This ensures data provenance, avoids credential theft, and respects university access controls.
