# Privacy Architecture
## Overview
The EduPulse Privacy Architecture leverages Client-Side Encryption (CSE) using Web Crypto API (AES-GCM) to ensure that sensitive academic risk records are only decryptable by the student or authorized personnel holding the correct keys. The backend (Spring Boot/Java 21/PostgreSQL) only stores ciphertext and necessary metadata.

## Components
1. **Frontend (React/TypeScript)**: Handles key generation, storage in memory, and encryption/decryption of risk records before sending/after receiving from the backend.
2. **Backend (Spring Boot)**: Exposes Privacy APIs for CRUD operations on encrypted records. Returns `UNSUPPORTED_ACCESS` for unauthorized AP access.
3. **Database (PostgreSQL)**: Stores encrypted payloads in the `risk_records` table.
