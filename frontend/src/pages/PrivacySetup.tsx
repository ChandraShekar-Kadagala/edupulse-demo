import React, { useState } from 'react';
import { useCrypto } from '../context/CryptoContext';
import { generateKey, exportKey, importKey, exportKeyWithPassphrase, importKeyWithPassphrase } from '../utils/crypto';
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';

export default function PrivacySetup() {
  const { key, setKey, isKeyLoaded } = useCrypto();
  const [exportedRaw, setExportedRaw] = useState<string>('');
  const [exportedPkg, setExportedPkg] = useState<string>('');
  
  const [importStr, setImportStr] = useState<string>('');
  const [passphrase, setPassphrase] = useState<string>('');
  const [importMode, setImportMode] = useState<'raw' | 'passphrase'>('passphrase');
  const [error, setError] = useState<string>('');

  const handleGenerate = async () => {
    try {
      const newKey = await generateKey();
      setKey(newKey);
      
      const exp = await exportKey(newKey);
      setExportedRaw(exp);
      
      setError('');
    } catch (err) {
      setError('Failed to generate key');
    }
  };

  const handleGeneratePassphraseRecovery = async () => {
    if (!key) return;
    if (passphrase.length < 8) {
      setError('Passphrase must be at least 8 characters');
      return;
    }
    try {
      const pkg = await exportKeyWithPassphrase(key, passphrase);
      setExportedPkg(JSON.stringify(pkg));
      setError('');
    } catch (err) {
      setError('Failed to generate recovery package');
    }
  };

  const handleImport = async () => {
    try {
      if (!importStr) throw new Error('Empty input');
      let newKey: CryptoKey;
      
      if (importMode === 'raw') {
        newKey = await importKey(importStr);
      } else {
        const pkg = JSON.parse(importStr);
        newKey = await importKeyWithPassphrase(pkg, passphrase);
      }
      
      setKey(newKey);
      setError('');
    } catch (err) {
      setError('Invalid recovery data or incorrect passphrase');
    }
  };

  const handleLogout = () => {
    setKey(null);
    setExportedRaw('');
    setExportedPkg('');
    setPassphrase('');
  };

  return (
    <div className="max-w-2xl mx-auto p-4 space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Privacy & Encryption Setup</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {!isKeyLoaded ? (
            <>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                EduPulse uses end-to-end client-side encryption for your academic risk history. 
                The server NEVER sees your decryption key.
              </p>
              
              <div className="flex gap-4">
                <Button onClick={handleGenerate}>Generate New Key</Button>
              </div>

              <div className="pt-4 border-t">
                <p className="text-sm font-medium mb-2">Or restore existing key:</p>
                <div className="flex gap-4 mb-2">
                  <label className="text-sm flex items-center gap-1">
                    <input type="radio" checked={importMode === 'passphrase'} onChange={() => setImportMode('passphrase')} />
                    Passphrase Recovery Package
                  </label>
                  <label className="text-sm flex items-center gap-1">
                    <input type="radio" checked={importMode === 'raw'} onChange={() => setImportMode('raw')} />
                    Raw Base64 Key
                  </label>
                </div>
                
                <div className="flex flex-col gap-2">
                  <textarea 
                    placeholder="Paste recovery data here" 
                    className="w-full px-3 py-2 border rounded text-xs font-mono h-20"
                    value={importStr}
                    onChange={(e) => setImportStr(e.target.value)}
                  />
                  {importMode === 'passphrase' && (
                    <input 
                      type="password" 
                      placeholder="Enter recovery passphrase" 
                      className="w-full px-3 py-2 border rounded"
                      value={passphrase}
                      onChange={(e) => setPassphrase(e.target.value)}
                    />
                  )}
                  <Button variant="outline" onClick={handleImport}>Restore Key</Button>
                </div>
                {error && <p className="text-red-500 text-sm mt-2">{error}</p>}
              </div>
            </>
          ) : (
            <>
              <div className="p-4 bg-green-50 text-green-700 rounded-md">
                ✓ Encryption Key Loaded Securely in Memory
              </div>

              {exportedRaw && (
                <div className="p-4 border rounded-md bg-yellow-50">
                  <p className="font-bold text-yellow-800">⚠️ CRITICAL: Unprotected Raw Backup</p>
                  <p className="text-sm text-yellow-700 mb-2">
                    Below is your raw encryption key encoded in Base64. <strong>Base64 is an encoding format, NOT encryption.</strong> Anyone with this string can decrypt your data. Store it offline.
                  </p>
                  <code className="block p-2 bg-white rounded border break-all text-xs mb-4">
                    {exportedRaw}
                  </code>
                  
                  <p className="font-bold text-yellow-800 border-t pt-4">🔒 Generate a Passphrase-Protected Package (Recommended)</p>
                  <p className="text-sm text-yellow-700 mb-2">
                    Create an encrypted backup using a secure passphrase (PBKDF2 derivation). We never send this passphrase to the server.
                  </p>
                  <div className="flex gap-2">
                    <input 
                      type="password" 
                      placeholder="Create a strong passphrase" 
                      className="flex-1 px-3 py-2 border rounded"
                      value={passphrase}
                      onChange={(e) => setPassphrase(e.target.value)}
                    />
                    <Button variant="outline" onClick={handleGeneratePassphraseRecovery}>Generate Package</Button>
                  </div>
                  {error && <p className="text-red-500 text-sm mt-2">{error}</p>}
                  
                  {exportedPkg && (
                    <div className="mt-4">
                      <p className="text-sm text-yellow-700 mb-2 font-bold">Your Encrypted Recovery Package (JSON):</p>
                      <code className="block p-2 bg-white rounded border break-all text-xs">
                        {exportedPkg}
                      </code>
                    </div>
                  )}
                </div>
              )}

              <Button variant="outline" onClick={handleLogout}>
                Clear Key from Memory
              </Button>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
