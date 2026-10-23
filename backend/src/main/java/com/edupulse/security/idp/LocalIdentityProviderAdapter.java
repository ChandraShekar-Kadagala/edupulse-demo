package com.edupulse.security.idp;

import com.edupulse.model.AuthProvider;
import org.springframework.stereotype.Component;

@Component
public class LocalIdentityProviderAdapter implements IdentityProviderAdapter {
    @Override
    public AuthProvider getProvider() {
        return AuthProvider.LOCAL;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "EduPulse Local Identity";
    }
}
