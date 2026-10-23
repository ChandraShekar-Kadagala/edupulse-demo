package com.edupulse.service.provider;

import com.edupulse.model.DataProvenance;

public interface AcademicDataProvider {
    
    /**
     * Reports the display name of the provider.
     */
    String getProviderName();

    /**
     * Reports the current status of the provider (e.g., AVAILABLE, NOT_CONFIGURED, FAILED)
     */
    String getProviderStatus();

    /**
     * Data provenance category for records created by this provider.
     */
    DataProvenance getProvenance();

    /**
     * Whether this provider is currently available for synchronization.
     */
    boolean isAvailable();

    /**
     * Synchronizes academic data for a specific student.
     */
    void syncStudentData(String studentId);
    
    /**
     * Returns true if this provider supports automated syncing
     */
    boolean supportsAutoSync();
}
