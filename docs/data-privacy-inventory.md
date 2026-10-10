# Data Privacy Inventory
- Users Table: Plaintext account data (hashed passwords).
- Students Table: Plaintext demographics.
- Attendance & Marks Tables: Plaintext historical academic data (Legacy Risk).
- EncryptedRiskRecords Table: Ciphertext AES-GCM (AAD bound) containing private risk summaries.

