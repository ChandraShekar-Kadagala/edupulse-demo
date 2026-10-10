import React, { createContext, useContext, useState, ReactNode } from 'react';

interface CryptoContextType {
  key: CryptoKey | null;
  setKey: (key: CryptoKey | null) => void;
  isKeyLoaded: boolean;
}

const CryptoContext = createContext<CryptoContextType>({
  key: null,
  setKey: () => {},
  isKeyLoaded: false,
});

export const useCrypto = () => useContext(CryptoContext);

export const CryptoProvider = ({ children }: { children: ReactNode }) => {
  const [key, setKey] = useState<CryptoKey | null>(null);

  return (
    <CryptoContext.Provider value={{ key, setKey, isKeyLoaded: !!key }}>
      {children}
    </CryptoContext.Provider>
  );
};
