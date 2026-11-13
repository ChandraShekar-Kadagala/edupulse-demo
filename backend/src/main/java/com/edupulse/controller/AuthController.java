package com.edupulse.controller;

import com.edupulse.dto.JwtResponse;
import com.edupulse.dto.LoginRequest;
import com.edupulse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@lombok.RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final com.edupulse.service.DemoShowcaseService demoShowcaseService;

    @Value("${app.demo.public-accounts-enabled:true}")
    private boolean publicDemoAccountsEnabled;

    @Value("${app.seeder.demo-password:DemoEduPulse2026!}")
    private String demoPassword;

    /**
     * Public showcase roster for the login page. Values are read live from the database
     * (DEMO accounts only); nothing is hardcoded. The shared demo password is public by design.
     */
    @GetMapping("/demo-status")
    public ResponseEntity<Map<String, Object>> getDemoStatus() {
        if (!publicDemoAccountsEnabled) {
            return ResponseEntity.ok(Map.of("demoModeEnabled", false));
        }
        List<Map<String, Object>> students = demoShowcaseService.students();
        List<Map<String, Object>> faculty = demoShowcaseService.faculty();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("demoModeEnabled", !students.isEmpty() || !faculty.isEmpty());
        body.put("demoMarker", "DEMO_SHOWCASE");
        body.put("demoPassword", demoPassword);
        body.put("students", students);
        body.put("facultyList", faculty);
        body.put("defaultStudentUsername", "demo.student.a");
        body.put("defaultStudentRegNo", "STU-1001");
        body.put("defaultFacultyUsername", "demo.faculty.1");
        body.put("disclaimer", "Illustrative demonstration accounts. Student A holds anonymized student-supplied reference data; "
                + "Students B-D and both faculty accounts are synthetic. Figures are read live from the database.");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username != null && !username.trim().isEmpty()) {
            authService.createPasswordResetTokenForUser(username.trim());
        }
        // Always return success to prevent username enumeration
        return ResponseEntity.ok(Map.of("message", "If an account exists, a reset link was generated."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        
        if (token == null || newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid token or password too short"));
        }

        try {
            authService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password successfully reset"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@org.springframework.security.core.annotation.AuthenticationPrincipal com.edupulse.security.UserDetailsImpl userDetails) {
        if (userDetails != null) {
            authService.logoutUser(userDetails.getId());
        }
        return ResponseEntity.ok(Map.of("message", "User logged out successfully. Active tokens invalidated."));
    }
}
