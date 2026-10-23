package com.edupulse.security.idp;

import com.edupulse.model.AuthProvider;

public interface IdentityProviderAdapter {
    AuthProvider getProvider();
    boolean isConfigured();
    String getDisplayName();
}
