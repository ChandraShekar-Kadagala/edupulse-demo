package com.edupulse.controller;

import com.edupulse.dto.StudentAcademicAnalyticsDto;
import com.edupulse.model.DataProvenance;
import com.edupulse.security.UserDetailsImpl;
import com.edupulse.service.provider.AcademicDataProvider;
import com.edupulse.service.provider.portal.SrmPortalIntegrationService;
import com.edupulse.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/integration", "/api/v1/integration"})
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
public class IntegrationController {

    @Autowired
    @Qualifier("officialSrmDataProvider")
    private AcademicDataProvider officialDataProvider;

    @Autowired
    @Qualifier("portalFixtureAcademicDataProvider")
    private AcademicDataProvider fixtureDataProvider;

    @Autowired
    @Qualifier("demoAcademicDataProvider")
    private AcademicDataProvider demoDataProvider;

    @Autowired
    private SrmPortalIntegrationService portalIntegrationService;

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'FACULTY')")
    public ResponseEntity<?> getIntegrationStatus() {
        return ResponseEntity.ok(portalIntegrationService.getIntegrationStatus());
    }

    @PostMapping("/link")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> linkAccount(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "official") String provider) {
        try {
            String studentNumber = studentRepository.findById(userDetails.getId())
                    .map(s -> s.getStudentNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));

            if ("demo".equalsIgnoreCase(provider)) {
                demoDataProvider.syncStudentData(studentNumber);
                return ResponseEntity.ok(Map.of(
                        "message", "Test account linked and synchronized successfully.",
                        "provenance", DataProvenance.SYNTHETIC_TEST_FIXTURE.name(),
                        "status", "SUCCESS",
                        "disclaimer", "Synthetic test fixture for sandbox demonstration only."
                ));
            } else if ("fixture".equalsIgnoreCase(provider)) {
                // Verified institutional portal HTML fixture pipeline
                StudentAcademicAnalyticsDto analytics = portalIntegrationService.synchronizeFromFixtures(userDetails.getId());
                return ResponseEntity.ok(Map.of(
                        "message", "Academic records successfully parsed and synchronized from verified portal fixtures.",
                        "provenance", DataProvenance.INSTITUTIONAL_PORTAL_FIXTURE.name(),
                        "status", "SUCCESS",
                        "analytics", analytics,
                        "disclaimer", "Ingested via verified institutional HTML parser pipeline. Institutional passwords are never stored."
                ));
            } else {
                // "official" or default -> calls officialDataProvider which throws IllegalStateException
                officialDataProvider.syncStudentData(studentNumber);
                return ResponseEntity.ok(Map.of("message", "Account linked successfully"));
            }
        } catch (IllegalStateException | UnsupportedOperationException e) {
            return ResponseEntity.status(403).body(Map.of("message", "No official public API exists. Cannot sync data."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
