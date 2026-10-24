package com.edupulse;

import com.edupulse.dto.LoginRequest;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.seeder.DataSeeder;
import com.edupulse.service.RiskAssessmentService;
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
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private RiskFactorRepository riskFactorRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void cleanDatabase() {
        tokenRepository.deleteAll();
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
    @DisplayName("1 & 2. Seeding with empty tables creates only designated demo accounts with AuthProvider.DEMO")
    public void testSeedingEnabled_EmptyTables_CreatesOnlyDesignatedDemoAccounts() {
        boolean seeded = dataSeeder.seedDemoData();
        assertTrue(seeded, "Expected seedDemoData to succeed on empty database");

        assertEquals(3, userRepository.count(), "Exactly 3 demo users should be created");
        assertEquals(1, studentRepository.count(), "Exactly 1 demo student record should be created");

        // Verify Demo Admin
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        assertTrue(adminOpt.isPresent(), "Demo admin user must exist");
        User admin = adminOpt.get();
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(AuthProvider.DEMO, admin.getAuthProvider());
        assertTrue(admin.getName().contains("Demo"), "Admin name must be marked as demo");
        assertEquals("demo.admin@edupulse.local", admin.getEmail());

        // Verify Demo Faculty
        Optional<User> facultyUserOpt = userRepository.findByUsername("faculty");
        assertTrue(facultyUserOpt.isPresent(), "Demo faculty user must exist");
        User facultyUser = facultyUserOpt.get();
        assertEquals(Role.FACULTY, facultyUser.getRole());
        assertEquals(AuthProvider.DEMO, facultyUser.getAuthProvider());
        assertTrue(facultyUser.getName().contains("Demo"), "Faculty name must be marked as demo");
        assertEquals("demo.faculty@edupulse.local", facultyUser.getEmail());

        // Verify Demo Student Arun
        Optional<User> arunUserOpt = userRepository.findByUsername("arun");
        assertTrue(arunUserOpt.isPresent(), "Demo student Arun user must exist");
        User arunUser = arunUserOpt.get();
        assertEquals(Role.STUDENT, arunUser.getRole());
        assertEquals(AuthProvider.DEMO, arunUser.getAuthProvider());
        assertTrue(arunUser.getName().contains("Demo"), "Student name must be marked as demo");
        assertEquals("demo.student@edupulse.local", arunUser.getEmail());

        Optional<Student> arunStudentOpt = studentRepository.findByStudentNumber("STU-1001");
        assertTrue(arunStudentOpt.isPresent(), "Student Arun must be queryable by student number STU-1001");
        Student arunStudent = arunStudentOpt.get();
        assertEquals(arunUser.getId(), arunStudent.getId());
        assertEquals("STU-1001", arunStudent.getStudentNumber());

        // Verify Illustrative Academic Fixtures
        assertTrue(courseRepository.count() > 0, "Demonstration course should be seeded");
        assertEquals(10, attendanceRepository.count(), "10 demonstration attendance records should be seeded");
        assertEquals(1, markRepository.count(), "Demonstration mark should be seeded");
        assertEquals(1, submissionRepository.count(), "Demonstration missing submission should be seeded");
        assertEquals(1, riskAssessmentRepository.count(), "Initial risk assessment should be computed for Arun");
    }

    @Test
    @DisplayName("3. Seeding with existing users aborts without modifying any records")
    public void testSeedingEnabled_WithExistingUsers_AbortsWithoutModifyingRecords() {
        // Pre-insert an existing application user
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
        assertFalse(userRepository.findByUsername("arun").isPresent(), "No demo accounts should be created");
        assertFalse(userRepository.findByUsername("admin").isPresent());
        assertFalse(userRepository.findByUsername("faculty").isPresent());
    }

    @Test
    @DisplayName("4. Repeated initialization produces no duplicates and no password resets")
    public void testRepeatedInitialization_NoDuplicatesAndNoPasswordResets() {
        // First run
        boolean firstRun = dataSeeder.seedDemoData();
        assertTrue(firstRun, "First run should seed data");
        long userCountAfterFirst = userRepository.count();
        assertEquals(3, userCountAfterFirst);

        User arunFirst = userRepository.findByUsername("arun").orElseThrow();
        String originalHash = arunFirst.getPasswordHash();
        Long originalTokenVersion = arunFirst.getTokenVersion();

        // Second run
        boolean secondRun = dataSeeder.seedDemoData();
        assertFalse(secondRun, "Second run should abort idempotently without modifying anything");

        assertEquals(userCountAfterFirst, userRepository.count(), "User count must not change on second run");
        User arunSecond = userRepository.findByUsername("arun").orElseThrow();
        assertEquals(originalHash, arunSecond.getPasswordHash(), "Password hash must not be reset on repeated execution");
        assertEquals(originalTokenVersion, arunSecond.getTokenVersion(), "Token version must not be changed");
    }

    @Test
    @DisplayName("5. Passwords are encrypted with configured BCrypt password encoder")
    public void testPasswordsStoredUsingConfiguredPasswordEncoder() {
        dataSeeder.seedDemoData();

        for (String username : new String[]{"admin", "faculty", "arun"}) {
            User user = userRepository.findByUsername(username).orElseThrow();
            String hash = user.getPasswordHash();

            assertNotNull(hash);
            assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"),
                    "Password hash must be a valid BCrypt string");
            assertTrue(passwordEncoder.matches("DemoEduPulse2026!", hash),
                    "Configured password encoder must successfully match the configured demo password");
        }
    }

    @Test
    @DisplayName("6. Demo users authenticate successfully and RBAC prevents unauthorized access")
    public void testDemoUsers_RbacAuthorizationEnforced() throws Exception {
        dataSeeder.seedDemoData();

        // 1. Authenticate as Demo Student 'arun'
        LoginRequest studentLogin = new LoginRequest();
        studentLogin.setUsername("arun");
        studentLogin.setPassword("DemoEduPulse2026!");

        MvcResult studentResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode studentJson = objectMapper.readTree(studentResult.getResponse().getContentAsString());
        String studentToken = studentJson.get("token").asText();
        assertEquals("DEMO", studentJson.get("authProvider").asText());
        assertEquals("ROLE_STUDENT", studentJson.get("role").asText());

        // Demo student attempting to access faculty/admin endpoint (/api/students) -> 403 FORBIDDEN
        mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // 2. Authenticate as Demo Faculty 'faculty'
        LoginRequest facultyLogin = new LoginRequest();
        facultyLogin.setUsername("faculty");
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
        mockMvc.perform(get("/api/students")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk());

        // Demo faculty attempting student-only private save (/api/v1/privacy/records) -> 403 FORBIDDEN
        mockMvc.perform(post("/api/v1/privacy/records")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ciphertext\":\"test\",\"metadata\":{}}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Demo status endpoint is public and exposes safe metadata without secrets")
    public void testDemoStatusEndpoint_PublicAndSafe() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/demo-status"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("demoModeEnabled").asBoolean());
        assertEquals("DEMO_SHOWCASE", json.get("demoMarker").asText());
        assertEquals("arun", json.get("defaultStudentUsername").asText());
        assertEquals("faculty", json.get("defaultFacultyUsername").asText());
        assertEquals("admin", json.get("defaultAdminUsername").asText());
        assertTrue(json.has("disclaimer"));

        // Verify NO passwords, hashes, or secrets are exposed in the JSON response
        assertFalse(json.has("password"));
        assertFalse(json.has("passwordHash"));
        assertFalse(json.has("jwtSecret"));
    }
}
