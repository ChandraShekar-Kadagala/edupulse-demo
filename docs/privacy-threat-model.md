# Privacy Threat Model
## Scope
The scope of this threat model includes the EduPulse web application, focusing on the handling of academic risk records.

## Assumptions
- The database is considered an untrusted environment for plaintext data.
- The user's device is trusted.
- The connection between client and server is secured via HTTPS/TLS.

## Threats
1. **Database Compromise**: If the database is compromised, the attacker only sees ciphertext. Without the client's key, the data remains secure.
2. **Key Material Leakage**: Keys are generated in the browser and stored only in memory. They are not persisted to localStorage or sent to the server. If the user closes the tab, the key is lost unless they have safely backed it up.
3. **Cross-Site Scripting (XSS)**: A successful XSS attack could steal the encryption keys from memory. Mitigation: Strict Content Security Policy (CSP) and React's built-in XSS protection.
4. **Unauthorized API Access**: The API requires valid session/tokens. However, even with API access, unauthorized users cannot decrypt the payloads. The SRM API is disabled unless authorized.

## Mitigations
- Web Crypto API for secure key generation and AES-GCM encryption.
- Zero-knowledge backend persistence.
- strict CSP.
- PWA cache hardening to ensure no sensitive plaintext is cached.
