package com.edupulse.service.provider;

import org.springframework.stereotype.Service;

@Service
public class OfficialSrmDataProvider implements AcademicDataProvider {

    @Override
    public String getProviderStatus() {
        return "NOT_CONFIGURED"; // Currently blocked by lack of official API/authorization
    }

    @Override
    public void syncStudentData(String studentId) {
        throw new IllegalStateException("Official university integration is currently disabled pending API authorization.");
    }

    @Override
    public boolean supportsAutoSync() {
        return true;
    }
}
