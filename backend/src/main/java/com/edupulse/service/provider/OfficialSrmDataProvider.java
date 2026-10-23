package com.edupulse.service.provider;

import com.edupulse.model.DataProvenance;
import com.edupulse.service.provider.contract.SrmApiContract;
import org.springframework.stereotype.Service;

@Service("officialSrmDataProvider")
public class OfficialSrmDataProvider implements AcademicDataProvider, SrmApiContract {

    @Override
    public String getProviderName() {
        return "Official SRM AP API (Live)";
    }

    @Override
    public String getProviderStatus() {
        return "UNSUPPORTED_ACCESS_PENDING_OAUTH";
    }

    @Override
    public DataProvenance getProvenance() {
        return DataProvenance.OFFICIAL_INSTITUTIONAL_API;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String getAuthenticationMechanism() {
        return "OAUTH2_AUTHORIZATION_CODE_PKCE (PENDING_INSTITUTIONAL_APPROVAL)";
    }

    @Override
    public String getBaseUrl() {
        return null;
    }

    @Override
    public void synchronizeOfficialStudentRecords(String srmRegistrationNumber) {
        throw new UnsupportedOperationException("Live SRM portal login requires official OAuth2/OIDC SSO credentials or institutional API authorization. Direct portal credential scraping is prohibited for student privacy.");
    }

    @Override
    public void syncStudentData(String studentId) {
        synchronizeOfficialStudentRecords(studentId);
    }

    @Override
    public boolean supportsAutoSync() {
        return true;
    }
}
