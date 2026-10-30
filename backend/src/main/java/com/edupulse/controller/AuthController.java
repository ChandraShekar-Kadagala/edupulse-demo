package com.edupulse.controller;

import com.edupulse.dto.JwtResponse;
import com.edupulse.dto.LoginRequest;
import com.edupulse.seeder.DataSeeder;
import com.edupulse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired(required = false)
    private DataSeeder dataSeeder;

    @Value("${app.seeder.enabled:false}")
    private boolean demoModeEnabled;

    @Value("${app.seeder.demo-password:DemoEduPulse2026!}")
    private String demoPassword;

    @GetMapping("/demo-status")
    public ResponseEntity<?> getDemoStatus() {
        return ResponseEntity.ok(Map.of(
                "demoModeEnabled", true,
                "demoMarker", "DEMO_SHOWCASE",
                "demoPassword", demoPassword != null ? demoPassword : "DemoEduPulse2026!",
                "students", List.of(
                        buildStudentDemo(
                                "demo.student.a", "STU-1001", "Student A — Reference",
                                "LOW", 0.0, 9.1, "99.6% (275/276)", "44.2% (150.44/340)",
                                "Anonymized SRM AP academic reference data (High attendance, clean submissions)",
                                "STUDENT-SUPPLIED REFERENCE DATA"
                        ),
                        buildStudentDemo(
                                "demo.student.b", "STU-1002", "Student B — Top Performer",
                                "LOW", 0.0, 9.4, "97.1% (268/276)", "92.4% (314/340)",
                                "Top academic performance across all assessments",
                                "SYNTHETIC DEMO DATA"
                        ),
                        buildStudentDemo(
                                "demo.student.c", "STU-1003", "Student C — Average Performer",
                                "MEDIUM", 37.0, 7.2, "71.0% (196/276)", "38.8% (132/340)",
                                "Average performance with 4 overdue deliverables (Medium risk flag)",
                                "SYNTHETIC DEMO DATA"
                        ),
                        buildStudentDemo(
                                "demo.student.d", "STU-1004", "Student D — High Risk",
                                "HIGH", 74.8, 5.1, "59.8% (165/276)", "27.6% (94/340)",
                                "Critical attendance deficit and 6 overdue deliverables (High risk flag)",
                                "SYNTHETIC DEMO DATA"
                        )
                ),
                "facultyList", List.of(
                        buildFacultyDemo("demo.faculty.1", "Faculty A", "Associate Professor", "CSE 204", "Design and Analysis of Algorithms", "Faculty Instructor & Evaluator for CSE 204"),
                        buildFacultyDemo("demo.faculty.2", "Faculty B", "Assistant Professor", "CSE 213", "Database Systems", "Faculty Instructor & Evaluator for CSE 213")
                ),
                "defaultStudentUsername", "demo.student.a",
                "defaultStudentRegNo", "STU-1001",
                "defaultFacultyUsername", "demo.faculty.1",
                "defaultAdminUsername", "admin",
                "disclaimer", "Illustrative demonstration accounts with pre-loaded SRM AP curriculum scenarios. Student A represents anonymized reference data; Students B, C, D and Faculty accounts provide synthetic comparative benchmarks."
        ));
    }

    private Map<String, Object> buildStudentDemo(String username, String regNo, String name, String risk, double riskScore, double cgpa, String attendance, String marks, String profile, String provenance) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("username", username);
        map.put("regNo", regNo);
        map.put("name", name);
        map.put("role", "STUDENT");
        map.put("risk", risk);
        map.put("riskScore", riskScore);
        map.put("cgpa", cgpa);
        map.put("attendance", attendance);
        map.put("marks", marks);
        map.put("profile", profile);
        map.put("provenance", provenance);
        return map;
    }

    private Map<String, Object> buildFacultyDemo(String username, String name, String title, String course, String courseName, String profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("username", username);
        map.put("name", name);
        map.put("role", "FACULTY");
        map.put("title", title);
        map.put("course", course);
        map.put("courseName", courseName);
        map.put("profile", profile);
        return map;
    }

    @PostMapping("/provision-demo-showcase")
    public ResponseEntity<?> provisionDemoShowcase() {
        if (dataSeeder == null) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "DataSeeder is inactive (app.seeder.enabled=false). Set SEED_DEMO_DATA=true in environment to activate seeder."
            ));
        }
        boolean success = dataSeeder.seedDemoData();
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", "Demo showcase accounts successfully provisioned or refreshed."
        ));
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
