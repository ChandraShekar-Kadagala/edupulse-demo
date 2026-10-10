# EduPulse Native Mobile Roadmap

## 1. Vision
The EduPulse native mobile application will provide a dedicated, accessible platform for students to monitor their academic performance and for faculty to quickly review high-risk students and log interventions on the go.

The mobile app will complement, not replace, the existing web application.

## 2. Technology Stack
- **Framework:** React Native
- **Toolchain:** Expo (Managed Workflow)
- **Language:** TypeScript
- **Backend:** The existing Spring Boot Java 21 REST API and PostgreSQL database.
- **Location:** A new `mobile/` directory alongside `frontend/` and `backend/`.

## 3. Architecture & Constraints
- **No Logic Duplication:** The mobile application MUST NOT implement its own risk engine. All risk scores, explanations, and intervention statuses must be fetched directly from the Spring Boot API.
- **Single Source of Truth:** The mobile app will use the exact same API endpoints (`/api/v1/...`) as the React web application.
- **Security:**
  - JWT tokens must be stored using `expo-secure-store` (encrypted on-device storage), NEVER in `AsyncStorage`.
  - The app must handle 401 Unauthorized errors by immediately clearing the secure store and redirecting to the Login screen.
  - No sensitive academic data will be persisted to local offline storage. Offline functionality will be limited to non-sensitive structural UI.

## 4. Phased Implementation Plan

### Phase A: Setup & Scaffolding
- Initialize the Expo project (`npx create-expo-app mobile -t expo-template-blank-typescript`).
- Configure environment variables for API base URLs (distinguishing between `localhost` for iOS simulators, `10.0.2.2` for Android emulators, and physical device LAN IPs).
- Set up React Navigation (Stack and Tab navigators).

### Phase B: Authentication & Security
- Implement the Login Screen.
- Integrate `expo-secure-store` for JWT persistence.
- Create an Axios interceptor to automatically attach the Bearer token and handle session expiry.

### Phase C: Student Experience
- **Home Tab:** Academic overview, attendance charts, and current risk score.
- **Performance Tab:** Historical trends and assessment grades.
- **Support Tab:** Assigned interventions, faculty recommendations, and follow-up notes.

### Phase D: Faculty Experience
- **Dashboard:** List of assigned students categorized by Risk Level (High/Medium/Low).
- **Student Profile:** Deep dive into a student's contributing risk factors.
- **Action Modal:** Form to quickly create or update an intervention (e.g., "Meeting Scheduled").

## 5. Development & Testing Environment
*Important Note on Local Networking:*
When developing the React Native app locally, the mobile device/emulator runs on a different network interface than the development machine. The API base URL cannot simply be `http://localhost:8080`.
- **Android Emulator:** Use `http://10.0.2.2:8080`.
- **Physical Device:** Use the developer machine's local LAN IP (e.g., `http://192.168.1.5:8080`).

## 6. Release Criteria
The mobile app will only enter active development *after* the Spring Boot backend APIs and data import workflows are fully verified, tested, and considered stable.
