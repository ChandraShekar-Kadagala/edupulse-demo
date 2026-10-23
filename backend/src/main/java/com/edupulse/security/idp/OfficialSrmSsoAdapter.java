package com.edupulse.security.idp;

import com.edupulse.model.AuthProvider;
import org.springframework.stereotype.Component;

@Component
public class OfficialSrmSsoAdapter implements IdentityProviderAdapter {
    @Override
    public AuthProvider getProvider() {
        return AuthProvider.OFFICIAL_SRM_SSO;
    }

    @Override
    public boolean isConfigured() {
        // Explicitly disabled pending institutional authorization & official API documentation
        return false;
    }

    @Override
    public String getDisplayName() {
        return "SRM AP Official SSO (Pending Authorization)";
    }
}
