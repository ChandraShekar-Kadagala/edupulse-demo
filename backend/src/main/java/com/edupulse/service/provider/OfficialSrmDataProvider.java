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
        return "UNSUPPORTED_ACCESS_PORTAL_AUTH_REQUIRED";
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
        return "OAUTH2_AUTHORIZATION_CODE_PKCE (INSTITUTIONAL_APPROVAL_REQUIRED)";
    }

    @Override
    public String getBaseUrl() {
        return "https://student.srmap.edu.in/srmapstudentcorner";
    }

    @Override
    public void synchronizeOfficialStudentRecords(String srmRegistrationNumber) {
        throw new UnsupportedOperationException(
                "Live SRM portal login requires official OAuth2/OIDC SSO credentials or institutional API authorization. " +
                "Direct portal credential scraping is prohibited for student privacy. " +
                "Direct automated portal scraping against https://student.srmap.edu.in is blocked because SRM AP does not provide an official API and protects its legacy student portal with image CAPTCHAs. " +
                "In compliance with EduPulse security principles, automated CAPTCHA bypassing and credential scraping are prohibited. " +
                "Please use verified Institutional Portal Report ingestion or institutional OAuth2 authorization."
        );
    }

    @Override
    public void syncStudentData(String studentId) {
        synchronizeOfficialStudentRecords(studentId);
    }

    @Override
    public boolean supportsAutoSync() {
        return false;
    }
}
