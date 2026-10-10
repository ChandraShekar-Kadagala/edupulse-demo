package com.edupulse.controller;

import com.edupulse.security.UserDetailsImpl;
import com.edupulse.service.provider.AcademicDataProvider;
import com.edupulse.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/integration")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
public class IntegrationController {

    @Autowired
    @Qualifier("officialSrmDataProvider")
    private AcademicDataProvider officialDataProvider;

    @Autowired
    @Qualifier("demoAcademicDataProvider")
    private AcademicDataProvider demoDataProvider;

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'FACULTY')")
    public ResponseEntity<?> getIntegrationStatus() {
        return ResponseEntity.ok(Map.of(
            "provider", "Official SRM AP API",
            "status", "UNSUPPORTED_ACCESS",
            "message", "No official public API exists for SRM AP. Account linking is blocked.",
            "lastSync", "N/A"
        ));
    }

    @PostMapping("/link")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> linkAccount(@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestParam(defaultValue = "official") String provider) {
        try {
            String studentNumber = studentRepository.findById(userDetails.getId())
                .map(s -> s.getStudentNumber())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

            if ("demo".equals(provider)) {
                demoDataProvider.syncStudentData(studentNumber);
                return ResponseEntity.ok(Map.of("message", "Test account linked and synchronized successfully."));
            } else {
                officialDataProvider.syncStudentData(studentNumber);
                return ResponseEntity.ok(Map.of("message", "Account linked successfully"));
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("message", "No official public API exists. Cannot sync data."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
