import { describe, it, expect } from 'vitest';
import { generateKey, exportKey, importKey, encryptData, decryptData, exportKeyWithPassphrase, importKeyWithPassphrase } from './crypto';

// Polyfill for Node environment
if (typeof window === 'undefined') {
  global.window = {
    crypto: require('crypto').webcrypto
  } as any;
}

describe('Web Crypto AES-GCM Implementation with AAD and Passphrase', () => {
  it('generates, exports, and imports a raw key securely', async () => {
    const key = await generateKey();
    const exported = await exportKey(key);
    const imported = await importKey(exported);
    expect(imported.algorithm.name).toBe('AES-GCM');
  });

  it('encrypts and decrypts payloads correctly with fresh nonces and AAD', async () => {
    const key = await generateKey();
    const data = { riskScore: 85 };
    const context = "STU-999-record-context";
    
    const encrypted1 = await encryptData(key, data, context);
    const encrypted2 = await encryptData(key, data, context);
    
    expect(encrypted1.nonceBase64).not.toBe(encrypted2.nonceBase64);
    expect(encrypted1.ciphertextBase64).not.toBe(encrypted2.ciphertextBase64);
    expect(encrypted1.version).toBe('1.1');
    
    const decrypted = await decryptData(key, encrypted1, context);
    expect(decrypted).toEqual(data);
  });

  it('rejects tampered ciphertext', async () => {
    const key = await generateKey();
    const encrypted = await encryptData(key, { riskScore: 85 }, "CTX");
    
    const tampered = encrypted.ciphertextBase64.substring(0, encrypted.ciphertextBase64.length - 2) + 'AA';
    await expect(decryptData(key, { ...encrypted, ciphertextBase64: tampered }, "CTX")).rejects.toThrow();
  });

  it('rejects incorrect keys', async () => {
    const key1 = await generateKey();
    const key2 = await generateKey();
    const encrypted = await encryptData(key1, { riskScore: 85 }, "CTX");
    
    await expect(decryptData(key2, encrypted, "CTX")).rejects.toThrow();
  });
  
  it('rejects record-context substitution (AAD mismatch)', async () => {
    const key = await generateKey();
    const encrypted = await encryptData(key, { riskScore: 85 }, "UserA-Context");
    
    await expect(decryptData(key, encrypted, "UserB-Context")).rejects.toThrow();
  });

  it('rejects unsupported versions', async () => {
    const key = await generateKey();
    const encrypted = await encryptData(key, { riskScore: 85 }, "CTX");
    
    await expect(decryptData(key, { ...encrypted, version: '9.9' }, "CTX")).rejects.toThrow('Unsupported encryption version');
  });

  it('rejects changing v1.1 payload to v1.0 to bypass AAD', async () => {
    const key = await generateKey();
    const encrypted = await encryptData(key, { riskScore: 85 }, "CTX");
    expect(encrypted.version).toBe('1.1');
    
    // Attacker modifies version to 1.0 to bypass AAD check
    await expect(decryptData(key, { ...encrypted, version: '1.0' }, "CTX")).rejects.toThrow();
  });

  it('handles version 1.0 logic without AAD securely', async () => {
    const key = await generateKey();
    const data = { riskScore: 85 };
    
    // Simulate v1.0 encryption (no AAD)
    const encoder = new TextEncoder();
    const plaintext = encoder.encode(JSON.stringify(data));
    const nonce = window.crypto.getRandomValues(new Uint8Array(12));
    const ciphertextBuffer = await window.crypto.subtle.encrypt(
      { name: 'AES-GCM', iv: nonce },
      key,
      plaintext
    );
    const ciphertextBytes = new Uint8Array(ciphertextBuffer);
    
    const v1Payload = {
      ciphertextBase64: btoa(String.fromCharCode(...ciphertextBytes)),
      nonceBase64: btoa(String.fromCharCode(...nonce)),
      version: '1.0'
    };
    
    // Decrypt version 1.0, supplying an AAD string that should be ignored by the logic
    const decrypted = await decryptData(key, v1Payload, "CTX");
    expect(decrypted).toEqual(data);
  });

  it('rejects invalid Base64 and malformed nonces', async () => {
    const key = await generateKey();
    const encrypted = await encryptData(key, { riskScore: 85 }, "CTX");
    
    await expect(decryptData(key, { ...encrypted, nonceBase64: 'invalid!base64' }, "CTX")).rejects.toThrow('Invalid Base64 encoding');
    
    // Valid base64 but wrong length
    await expect(decryptData(key, { ...encrypted, nonceBase64: btoa('123') }, "CTX")).rejects.toThrow('Invalid nonce length');
  });

  it('generates a passphrase-protected recovery package and restores the key', async () => {
    const key = await generateKey();
    const passphrase = "MySecureRecoveryPassword123!";
    
    const pkg = await exportKeyWithPassphrase(key, passphrase);
    expect(pkg.wrappedKeyBase64).toBeDefined();
    expect(pkg.saltBase64).toBeDefined();
    
    const restoredKey = await importKeyWithPassphrase(pkg, passphrase);
    expect(restoredKey.algorithm.name).toBe('AES-GCM');
    
    // Verify it works
    const encrypted = await encryptData(key, { test: 1 }, "CTX");
    const decrypted = await decryptData(restoredKey, encrypted, "CTX");
    expect(decrypted.test).toBe(1);
  });
  
  it('rejects incorrect recovery secret (wrong passphrase)', async () => {
    const key = await generateKey();
    const pkg = await exportKeyWithPassphrase(key, "CorrectPassword");
    
    await expect(importKeyWithPassphrase(pkg, "WrongPassword")).rejects.toThrow('Invalid recovery passphrase or corrupted package');
  });

  it('rejects invalid recovery packages (iterations, lengths)', async () => {
    const key = await generateKey();
    const pkg = await exportKeyWithPassphrase(key, "CorrectPassword");
    
    // Unbounded iterations
    await expect(importKeyWithPassphrase({ ...pkg, iterations: 100 }, "CorrectPassword")).rejects.toThrow('Invalid PBKDF2 iteration count');
    await expect(importKeyWithPassphrase({ ...pkg, iterations: 1000000 }, "CorrectPassword")).rejects.toThrow('Invalid PBKDF2 iteration count');

    // Invalid salt length
    const invalidSaltPkg = { ...pkg, saltBase64: btoa('123') }; // Too short
    await expect(importKeyWithPassphrase(invalidSaltPkg, "CorrectPassword")).rejects.toThrow('Invalid salt length');

    // Invalid IV length
    const invalidIvPkg = { ...pkg, ivBase64: btoa('123') }; // Too short
    await expect(importKeyWithPassphrase(invalidIvPkg, "CorrectPassword")).rejects.toThrow('Invalid IV length');

    // Invalid wrapped key length
    const invalidKeyPkg = { ...pkg, wrappedKeyBase64: btoa('123') }; // Too short
    await expect(importKeyWithPassphrase(invalidKeyPkg, "CorrectPassword")).rejects.toThrow('Invalid wrapped key payload size');
  });
});
