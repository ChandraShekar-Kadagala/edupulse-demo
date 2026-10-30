package com.edupulse.controller;

import com.edupulse.dto.DashboardSummaryDto;
import com.edupulse.service.AcademicAnalyticsService;
import com.edupulse.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AcademicAnalyticsService academicAnalyticsService;

    @Autowired
    private com.edupulse.service.FacultyService facultyService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<DashboardSummaryDto> getSummary(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.edupulse.security.UserDetailsImpl userDetails) {
        DashboardSummaryDto summary = academicAnalyticsService.getDashboardSummary();
        if (userDetails != null) {
            boolean isFaculty = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
            if (isFaculty) {
                summary.setCurrentFaculty(facultyService.getFacultyById(userDetails.getId()));
                summary.setFacultyView(facultyService.getFacultyDashboard(userDetails.getId(), userDetails.getUsername()));
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
