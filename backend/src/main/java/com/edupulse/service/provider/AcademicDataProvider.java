package com.edupulse.service.provider;

import com.edupulse.model.Student;
import java.util.List;

public interface AcademicDataProvider {
    
    /**
     * Reports the current status of the provider (e.g., AVAILABLE, NOT_CONFIGURED, FAILED)
     */
    String getProviderStatus();

    /**
     * Synchronizes academic data for a specific student.
     */
    void syncStudentData(String studentId);
    
    /**
     * Returns true if this provider supports automated syncing
     */
    boolean supportsAutoSync();
}
