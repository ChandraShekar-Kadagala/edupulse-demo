package com.edupulse.service.provider;

import com.edupulse.model.DataProvenance;
import org.springframework.stereotype.Service;

@Service
public class CsvAcademicDataProvider implements AcademicDataProvider {

    @Override
    public String getProviderName() {
        return "CSV Faculty Import Provider";
    }

    @Override
    public String getProviderStatus() {
        return "AVAILABLE";
    }

    @Override
    public DataProvenance getProvenance() {
        return DataProvenance.CSV_FACULTY_IMPORT;
    }

    @Override
    public boolean isAvailable() {
        return true;
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
