package com.edupulse.service.provider;

import com.edupulse.model.DataProvenance;
import com.edupulse.model.Student;
import com.edupulse.repository.StudentRepository;
import com.edupulse.service.provider.portal.SrmPortalIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("portalFixtureAcademicDataProvider")
public class PortalFixtureAcademicDataProvider implements AcademicDataProvider {

    @Autowired
    private SrmPortalIntegrationService portalIntegrationService;

    @Autowired
    private StudentRepository studentRepository;

    @Override
    public String getProviderName() {
        return "SRM AP Portal Prototype (Verified HTML Fixtures)";
    }

    @Override
    public String getProviderStatus() {
        return "PROTOTYPE_FIXTURE_ACTIVE";
    }

    @Override
    public DataProvenance getProvenance() {
        return DataProvenance.INSTITUTIONAL_PORTAL_FIXTURE;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void syncStudentData(String studentIdentifier) {
        Student student = studentRepository.findByStudentNumber(studentIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with number: " + studentIdentifier));

        portalIntegrationService.synchronizeFromFixtures(student.getId());
    }

    @Override
    public boolean supportsAutoSync() {
        return false;
    }
}
