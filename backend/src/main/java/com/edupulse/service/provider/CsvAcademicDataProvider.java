package com.edupulse.service.provider;

import org.springframework.stereotype.Service;

@Service
public class CsvAcademicDataProvider implements AcademicDataProvider {

    @Override
    public String getProviderStatus() {
        return "AVAILABLE";
    }

    @Override
    public void syncStudentData(String studentId) {
        // CSV Provider does not pull data on demand. It receives data via explicit upload.
        throw new UnsupportedOperationException("CSV Provider requires explicit file upload. Cannot auto-sync.");
    }

    @Override
    public boolean supportsAutoSync() {
        return false;
    }
}
