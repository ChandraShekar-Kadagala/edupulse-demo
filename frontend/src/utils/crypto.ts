/**
 * Web Crypto API implementation for AES-GCM with AAD and Passphrase Recovery
 */

// Helper: safe base64 decode
export function safeAtob(b64: string): string {
  try {
    return atob(b64);
  } catch (e) {
    throw new Error('Invalid Base64 encoding');
  }
}

// Generate a random AES-GCM 256-bit key
export async function generateKey(): Promise<CryptoKey> {
  return window.crypto.subtle.generateKey(
    { name: 'AES-GCM', length: 256 },
    true, // extractable
    ['encrypt', 'decrypt']
  );
}

// Export key to raw string (base64) for raw backup
export async function exportKey(key: CryptoKey): Promise<string> {
  const exported = await window.crypto.subtle.exportKey('raw', key);
  return btoa(String.fromCharCode(...new Uint8Array(exported)));
}

// Import key from base64 string
export async function importKey(base64: string): Promise<CryptoKey> {
  const binary_string = safeAtob(base64);
  const len = binary_string.length;
  if (len !== 32) throw new Error('Invalid key length, expected 256-bit key');
  
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binary_string.charCodeAt(i);
  }
  
  return window.crypto.subtle.importKey(
    'raw',
    bytes.buffer,
    { name: 'AES-GCM' },
    true,
    ['encrypt', 'decrypt']
  );
}

export interface EncryptedPayload {
  ciphertextBase64: string;
  nonceBase64: string;
  version: string;
}

// Encrypt data using AES-GCM with Additional Authenticated Data
export async function encryptData(key: CryptoKey, data: any, contextAad: string): Promise<EncryptedPayload> {
  const encoder = new TextEncoder();
  const plaintext = encoder.encode(JSON.stringify(data));
  const aad = encoder.encode(contextAad);
  
  // 96-bit nonce
  const nonce = window.crypto.getRandomValues(new Uint8Array(12));
  
  const ciphertextBuffer = await window.crypto.subtle.encrypt(
    {
      name: 'AES-GCM',
      iv: nonce,
      additionalData: aad
    },
    key,
    plaintext
  );
  
  const ciphertextBytes = new Uint8Array(ciphertextBuffer);
  return {
    ciphertextBase64: btoa(String.fromCharCode(...ciphertextBytes)),
    nonceBase64: btoa(String.fromCharCode(...nonce)),
    version: '1.1'
  };
}

// Decrypt data using AES-GCM with Additional Authenticated Data
export async function decryptData(key: CryptoKey, payload: EncryptedPayload, contextAad: string): Promise<any> {
  if (payload.version !== '1.1' && payload.version !== '1.0') {
    throw new Error('Unsupported encryption version');
  }

  const ciphertextStr = safeAtob(payload.ciphertextBase64);
  const ciphertextBytes = new Uint8Array(ciphertextStr.length);
  for (let i = 0; i < ciphertextStr.length; i++) {
    ciphertextBytes[i] = ciphertextStr.charCodeAt(i);
  }

  const nonceStr = safeAtob(payload.nonceBase64);
  const nonceBytes = new Uint8Array(nonceStr.length);
  if (nonceBytes.length !== 12) {
    throw new Error('Invalid nonce length');
  }
  for (let i = 0; i < nonceStr.length; i++) {
    nonceBytes[i] = nonceStr.charCodeAt(i);
  }

  const encoder = new TextEncoder();
  const options: AesGcmParams = {
    name: 'AES-GCM',
    iv: nonceBytes
  };

  if (payload.version === '1.1') {
    options.additionalData = encoder.encode(contextAad);
  }

  try {
    const decryptedBuffer = await window.crypto.subtle.decrypt(
      options,
      key,
      ciphertextBytes
    );

    const decoder = new TextDecoder();
    const decryptedText = decoder.decode(decryptedBuffer);
    return JSON.parse(decryptedText);
  } catch (e) {
    throw new Error('Decryption failed. Incorrect key, modified ciphertext, or invalid context AAD.');
  }
}

export interface RecoveryPackage {
  wrappedKeyBase64: string;
  saltBase64: string;
  ivBase64: string;
  iterations: number;
}

// Passphrase-protected recovery package generation
export async function exportKeyWithPassphrase(key: CryptoKey, passphrase: string): Promise<RecoveryPackage> {
  const encoder = new TextEncoder();
  
  const rawKey = await window.crypto.subtle.exportKey('raw', key);
  
  const salt = window.crypto.getRandomValues(new Uint8Array(16));
  const iterations = 100000;
  
  const keyMaterial = await window.crypto.subtle.importKey(
    'raw',
    encoder.encode(passphrase),
    { name: 'PBKDF2' },
    false,
    ['deriveKey']
  );
  
  const kek = await window.crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt: salt,
      iterations: iterations,
      hash: 'SHA-256'
    },
    keyMaterial,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt']
  );

  const iv = window.crypto.getRandomValues(new Uint8Array(12));
  
  const wrapped = await window.crypto.subtle.encrypt(
    { name: 'AES-GCM', iv: iv },
    kek,
    rawKey
  );
  
  return {
    wrappedKeyBase64: btoa(String.fromCharCode(...new Uint8Array(wrapped))),
    saltBase64: btoa(String.fromCharCode(...salt)),
    ivBase64: btoa(String.fromCharCode(...iv)),
    iterations: iterations
  };
}

// Passphrase-protected recovery package restoration
export async function importKeyWithPassphrase(pkg: RecoveryPackage, passphrase: string): Promise<CryptoKey> {
  if (!pkg || typeof pkg.saltBase64 !== 'string' || typeof pkg.ivBase64 !== 'string' || typeof pkg.wrappedKeyBase64 !== 'string' || typeof pkg.iterations !== 'number') {
    throw new Error('Invalid recovery package structure');
  }

  // Prevent unbounded PBKDF2 work (min 100k, max 500k)
  if (pkg.iterations < 100000 || pkg.iterations > 500000) {
    throw new Error('Invalid PBKDF2 iteration count. Must be between 100,000 and 500,000 for safe recovery.');
  }

  const encoder = new TextEncoder();
  
  const saltStr = safeAtob(pkg.saltBase64);
  const salt = new Uint8Array(saltStr.length);
  if (salt.length !== 16) throw new Error('Invalid salt length. Must be 16 bytes.');
  for (let i = 0; i < salt.length; i++) salt[i] = saltStr.charCodeAt(i);
    
  const keyMaterial = await window.crypto.subtle.importKey(
    'raw',
    encoder.encode(passphrase),
    { name: 'PBKDF2' },
    false,
    ['deriveKey']
  );
  
  const kek = await window.crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt: salt,
      iterations: pkg.iterations,
      hash: 'SHA-256'
    },
    keyMaterial,
    { name: 'AES-GCM', length: 256 },
    false,
    ['decrypt']
  );

  const ivStr = safeAtob(pkg.ivBase64);
  const iv = new Uint8Array(ivStr.length);
  if (iv.length !== 12) throw new Error('Invalid IV length. Must be 12 bytes.');
  for (let i = 0; i < iv.length; i++) iv[i] = ivStr.charCodeAt(i);
  
  const wrappedStr = safeAtob(pkg.wrappedKeyBase64);
  const wrapped = new Uint8Array(wrappedStr.length);
  // Expected wrapped key size for 256-bit AES-GCM raw key (32 bytes + 16 bytes auth tag = 48 bytes)
  if (wrapped.length !== 48) {
     throw new Error('Invalid wrapped key payload size.');
  }
  for (let i = 0; i < wrapped.length; i++) wrapped[i] = wrappedStr.charCodeAt(i);
  
  try {
    const rawKey = await window.crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: iv },
      kek,
      wrapped
    );
    
    if (rawKey.byteLength !== 32) throw new Error('Invalid recovered key length.');

    return window.crypto.subtle.importKey(
      'raw',
      rawKey,
      { name: 'AES-GCM' },
      true,
      ['encrypt', 'decrypt']
    );
  } catch (e) {
    throw new Error('Invalid recovery passphrase or corrupted package');
  }
}
