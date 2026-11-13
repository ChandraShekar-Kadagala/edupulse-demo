package com.edupulse.controller;

import com.edupulse.dto.DashboardSummaryDto;
import com.edupulse.service.AcademicAnalyticsService;
import com.edupulse.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping("/api/dashboard")
@lombok.RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    private final AcademicAnalyticsService academicAnalyticsService;

    private final com.edupulse.service.FacultyService facultyService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<DashboardSummaryDto> getSummary(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.edupulse.security.UserDetailsImpl userDetails) {
        DashboardSummaryDto summary = academicAnalyticsService.getDashboardSummary();
        if (userDetails != null) {
            boolean isFaculty = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
            if (isFaculty) {
                try {
                    summary.setCurrentFaculty(facultyService.getFacultyById(userDetails.getId()));
                    summary.setFacultyView(facultyService.getFacultyDashboard(userDetails.getId(), null));
                } catch (com.edupulse.exception.ResourceNotFoundException noProfile) {
                    // FACULTY account without a faculty profile: the cohort summary is still returned.
                }
            }
        }
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }
}
