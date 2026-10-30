package com.edupulse;

import com.edupulse.dto.LoginRequest;
import com.edupulse.dto.StudentAcademicAnalyticsDto;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.seeder.DataSeeder;
import com.edupulse.service.AcademicAnalyticsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.seeder.enabled=true",
        "app.seeder.demo-password=DemoEduPulse2026!"
})
public class DemoShowcaseSeederTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    @Autowired
    private InterventionRepository interventionRepository;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private RiskFactorRepository riskFactorRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AcademicAnalyticsService academicAnalyticsService;

    @BeforeEach
    public void cleanDatabase() {
        tokenRepository.deleteAll();
        interventionRepository.deleteAll();
        riskFactorRepository.deleteAll();
        riskAssessmentRepository.deleteAll();
        submissionRepository.deleteAll();
        assignmentRepository.deleteAll();
        markRepository.deleteAll();
        assessmentRepository.deleteAll();
        attendanceRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        facultyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Seeding with empty tables creates 7 courses, 4-student showcase cohort and 2 faculty profiles")
    public void testSeedingEnabled_EmptyTables_CreatesFourStudentShowcaseCohort() {
        boolean seeded = dataSeeder.seedDemoData();
        assertTrue(seeded, "Expected seedDemoData to succeed on empty database");

        assertEquals(8, userRepository.count(), "Expected 8 demo users (admin, 2 faculty + alias, 4 students)");
        assertEquals(4, studentRepository.count(), "Exactly 4 demo student records should be created");
        assertEquals(2, facultyRepository.count(), "Exactly 2 demo faculty profiles should be created (roopa, aditya)");
        assertEquals(7, courseRepository.count(), "All 7 SRM AP courses must be seeded");
        assertEquals(3, interventionRepository.count(), "3 demo interventions should be seeded");

        // Verify Demo Admin
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        assertTrue(adminOpt.isPresent(), "Demo admin user must exist");
        User admin = adminOpt.get();
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(AuthProvider.DEMO, admin.getAuthProvider());

        // Verify Faculty 1: Dr. Roopa Tirumalasetti (assigned CSE 204)
        Optional<User> roopaOpt = userRepository.findByUsername("roopa");
        assertTrue(roopaOpt.isPresent(), "Demo faculty roopa must exist");
        User roopaUser = roopaOpt.get();
        assertEquals(Role.FACULTY, roopaUser.getRole());
        Faculty roopaProfile = facultyRepository.findById(roopaUser.getId()).orElseThrow();
        assertEquals("Associate Professor", roopaProfile.getTitle());

        // Verify Faculty 2: Dr. Aditya Kumar Sahu (assigned CSE 213)
        Optional<User> adityaOpt = userRepository.findByUsername("aditya");
        assertTrue(adityaOpt.isPresent(), "Demo faculty aditya must exist");
        User adityaUser = adityaOpt.get();
        assertEquals(Role.FACULTY, adityaUser.getRole());
        Faculty adityaProfile = facultyRepository.findById(adityaUser.getId()).orElseThrow();
        assertEquals("Assistant Professor", adityaProfile.getTitle());

        // Verify Student 1: KADAGALA CHANDRA SHEKAR (Actual Student Reference Data)
        Student chandra = studentRepository.findByStudentNumber("STU-2026-CHANDRA").orElseThrow();
        assertEquals("chandrashekar", chandra.getUser().getUsername());
        assertEquals("KADAGALA CHANDRA SHEKAR", chandra.getUser().getName());
        RiskAssessment chandraRisk = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(chandra.getId()).orElseThrow();
        assertEquals(RiskLevel.LOW, chandraRisk.getRiskLevel(), "Chandra Shekar must have LOW risk");
        assertEquals(0.0, chandraRisk.getRiskScore(), 0.1, "Chandra Shekar risk score must be 0.0");

        // Verify Student 1 exact attendance: 275/276 = 99.64%
        StudentAcademicAnalyticsDto chandraAnalytics = academicAnalyticsService.getStudentAnalytics(chandra.getId());
        assertEquals(276, chandraAnalytics.getTotalClasses());
        assertEquals(275, chandraAnalytics.getPresentClasses());
        assertEquals(1, chandraAnalytics.getAbsentClasses());
        assertEquals(99.64, chandraAnalytics.getOverallAttendancePercentage(), 0.01);
        assertEquals("ACTUAL STUDENT-SUPPLIED REFERENCE DATA", chandraAnalytics.getProvenance());

        // Verify Student 1 exact converted marks sum = 150.44
        double convertedSum = chandraAnalytics.getAssessmentBreakdown().stream()
                .filter(a -> a.getConvertedMarks() != null)
                .mapToDouble(a -> a.getConvertedMarks())
                .sum();
        assertEquals(150.44, convertedSum, 0.05, "Chandra Shekar converted marks sum must equal exactly 150.44");

        // Verify Student 2: Aarav Sharma (Synthetic Top Performer - LOW risk)
        Student aarav = studentRepository.findByStudentNumber("STU-1002").orElseThrow();
        assertEquals("aarav", aarav.getUser().getUsername());
        RiskAssessment aaravRisk = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(aarav.getId()).orElseThrow();
        assertEquals(RiskLevel.LOW, aaravRisk.getRiskLevel(), "Aarav must be LOW risk");
        assertEquals(0.0, aaravRisk.getRiskScore(), 0.1, "Aarav risk score should be 0.0");

        // Verify Student 3: Bhavna Patel (Synthetic Average Performer - MEDIUM risk)
        Student bhavna = studentRepository.findByStudentNumber("STU-1003").orElseThrow();
        assertEquals("bhavna", bhavna.getUser().getUsername());
        RiskAssessment bhavnaRisk = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(bhavna.getId()).orElseThrow();
        assertEquals(RiskLevel.MEDIUM, bhavnaRisk.getRiskLevel(), "Bhavna must be MEDIUM risk");
        assertTrue(bhavnaRisk.getRiskScore() >= 35.0 && bhavnaRisk.getRiskScore() < 65.0, "Bhavna risk score must be in medium range (35-65)");

        // Verify Student 4: Arun Kumar (Synthetic High-Risk Performer - HIGH risk)
        Student arun = studentRepository.findByStudentNumber("STU-1004").orElseThrow();
        assertEquals("arun", arun.getUser().getUsername());
        RiskAssessment arunRisk = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(arun.getId()).orElseThrow();
        assertEquals(RiskLevel.HIGH, arunRisk.getRiskLevel(), "Arun must be HIGH risk");
        assertTrue(arunRisk.getRiskScore() >= 65.0, "Arun risk score should be in high range (>= 65)");
    }

    @Test
    @DisplayName("2. Seeding with existing users aborts without modifying any records")
    public void testSeedingEnabled_WithExistingUsers_AbortsWithoutModifyingRecords() {
        User existingUser = User.builder()
                .username("real_user")
                .passwordHash(passwordEncoder.encode("ExistingPass123!"))
                .role(Role.STUDENT)
                .name("Real User Record")
                .email("real.user@example.edu")
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepository.save(existingUser);
        long initialCount = userRepository.count();

        boolean seeded = dataSeeder.seedDemoData();
        assertFalse(seeded, "Seeding should be aborted when database already contains user records");

        assertEquals(initialCount, userRepository.count(), "User count must not change when seeding aborts");
        assertTrue(userRepository.findByUsername("real_user").isPresent(), "Existing user must remain intact");
        assertFalse(userRepository.findByUsername("chandrashekar").isPresent(), "No demo accounts should be created");
        assertFalse(userRepository.findByUsername("aarav").isPresent());
    }

    @Test
    @DisplayName("3. Repeated initialization produces no duplicates and no password resets")
    public void testRepeatedInitialization_NoDuplicatesAndNoPasswordResets() {
        boolean firstRun = dataSeeder.seedDemoData();
        assertTrue(firstRun, "First run should seed data");
        long userCountAfterFirst = userRepository.count();
        assertEquals(8, userCountAfterFirst);

        User chandraFirst = userRepository.findByUsername("chandrashekar").orElseThrow();
        String originalHash = chandraFirst.getPasswordHash();

        boolean secondRun = dataSeeder.seedDemoData();
        assertFalse(secondRun, "Second run should abort idempotently without modifying anything");

        assertEquals(userCountAfterFirst, userRepository.count(), "User count must not change on second run");
        User chandraSecond = userRepository.findByUsername("chandrashekar").orElseThrow();
        assertEquals(originalHash, chandraSecond.getPasswordHash(), "Password hash must not be reset on repeated execution");
    }

    @Test
    @DisplayName("4. Passwords are encrypted with configured BCrypt password encoder")
    public void testPasswordsStoredUsingConfiguredPasswordEncoder() {
        dataSeeder.seedDemoData();

        for (String username : new String[]{"admin", "roopa", "aditya", "faculty", "chandrashekar", "aarav", "bhavna", "arun"}) {
            User user = userRepository.findByUsername(username).orElseThrow();
            String hash = user.getPasswordHash();
            assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"),
                    "Password hash must follow BCrypt format for user " + username);
            assertTrue(passwordEncoder.matches("DemoEduPulse2026!", hash),
                    "Password hash must verify against configured demo password for user " + username);
        }
    }

    @Test
    @DisplayName("5. Enforces RBAC: Students forbidden from faculty directories; Faculty can access cohort")
    public void testRbacEnforcementAcrossRoles() throws Exception {
        dataSeeder.seedDemoData();

        // 1. Authenticate as Student 'chandrashekar'
        LoginRequest studentLogin = new LoginRequest();
        studentLogin.setUsername("chandrashekar");
        studentLogin.setPassword("DemoEduPulse2026!");

        MvcResult studentResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode studentJson = objectMapper.readTree(studentResult.getResponse().getContentAsString());
        String studentToken = studentJson.get("token").asText();
        assertEquals("ROLE_STUDENT", studentJson.get("role").asText());

        // Demo student attempting to access faculty/admin endpoint (/api/students) -> 403 FORBIDDEN
        mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // 2. Authenticate as Demo Faculty 'roopa'
        LoginRequest facultyLogin = new LoginRequest();
        facultyLogin.setUsername("roopa");
        facultyLogin.setPassword("DemoEduPulse2026!");

        MvcResult facultyResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facultyLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode facultyJson = objectMapper.readTree(facultyResult.getResponse().getContentAsString());
        String facultyToken = facultyJson.get("token").asText();
        assertEquals("DEMO", facultyJson.get("authProvider").asText());
        assertEquals("ROLE_FACULTY", facultyJson.get("role").asText());

        // Demo faculty CAN access faculty endpoint (/api/students) -> 200 OK
        MvcResult studentsResult = mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode studentsArray = objectMapper.readTree(studentsResult.getResponse().getContentAsString());
        assertTrue(studentsArray.isArray());
        assertEquals(4, studentsArray.size(), "Faculty should receive all 4 students in cohort");

        // Verify StudentDto has both flat fields and user object
        JsonNode firstStudent = studentsArray.get(0);
        assertTrue(firstStudent.has("name"));
        assertTrue(firstStudent.has("user"));
        assertNotNull(firstStudent.get("user").get("name").asText());
        assertTrue(firstStudent.has("riskLevel"));
        assertTrue(firstStudent.has("attendancePercentage"));

        // Faculty CAN access all interventions endpoint (/api/interventions) -> 200 OK
        MvcResult interventionsResult = mockMvc.perform(get("/api/interventions")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode interventionsArray = objectMapper.readTree(interventionsResult.getResponse().getContentAsString());
        assertTrue(interventionsArray.isArray());
        assertEquals(3, interventionsArray.size(), "Faculty should receive all 3 active/planned/completed interventions");
    }

    @Test
    @DisplayName("6. Demo status endpoint is public and exposes safe metadata with 4 students and 2 faculty")
    public void testDemoStatusEndpoint_PublicAndSafe() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/demo-status"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("demoModeEnabled").asBoolean());
        assertEquals("DEMO_SHOWCASE", json.get("demoMarker").asText());
        assertEquals("chandrashekar", json.get("defaultStudentUsername").asText());
        assertEquals("roopa", json.get("defaultFacultyUsername").asText());
        assertEquals("admin", json.get("defaultAdminUsername").asText());
        assertTrue(json.has("students"));
        assertEquals(4, json.get("students").size());
        assertTrue(json.has("facultyList"));
        assertEquals(2, json.get("facultyList").size());

        // Verify NO passwords, hashes, or secrets are exposed in the JSON response
        assertFalse(json.has("password"));
        assertFalse(json.has("passwordHash"));
        assertFalse(json.has("jwtSecret"));
    }
}
