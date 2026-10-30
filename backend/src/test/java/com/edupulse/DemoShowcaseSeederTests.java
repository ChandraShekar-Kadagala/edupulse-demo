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
@org.springframework.test.context.TestPropertySource(properties = {
        "app.seeder.enabled=true"
})
public class DemoShowcaseSeederTests {

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
    private AcademicAnalyticsService academicAnalyticsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        interventionRepository.deleteAll();
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
    @DisplayName("1. Seeding creates 7 courses, 4-student showcase cohort and 2 faculty profiles")
    public void testSeedingEnabled_CreatesFourStudentShowcaseCohort() {
        boolean seeded = dataSeeder.seedDemoData();
        assertTrue(seeded, "Expected seedDemoData to succeed");

        assertEquals(4, studentRepository.count(), "Exactly 4 demo student records should be created");
        assertEquals(2, facultyRepository.count(), "Exactly 2 demo faculty profiles should be created");
        assertEquals(7, courseRepository.count(), "All 7 SRM AP courses must be seeded");
        assertEquals(3, interventionRepository.count(), "3 demo interventions should be seeded");

        // Verify Demo Admin
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        assertTrue(adminOpt.isPresent(), "Demo admin user must exist");
        User admin = adminOpt.get();
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(AuthProvider.DEMO, admin.getAuthProvider());

        // Verify Faculty 1: Faculty A (assigned CSE 204)
        Optional<User> f1Opt = userRepository.findByUsername("demo.faculty.1");
        assertTrue(f1Opt.isPresent(), "Demo faculty 1 must exist");
        User f1User = f1Opt.get();
        assertEquals(Role.FACULTY, f1User.getRole());
        Faculty f1Profile = facultyRepository.findById(f1User.getId()).orElseThrow();
        assertEquals("Associate Professor", f1Profile.getTitle());

        // Verify Faculty 2: Faculty B (assigned CSE 213)
        Optional<User> f2Opt = userRepository.findByUsername("demo.faculty.2");
        assertTrue(f2Opt.isPresent(), "Demo faculty 2 must exist");
        User f2User = f2Opt.get();
        assertEquals(Role.FACULTY, f2User.getRole());
        Faculty f2Profile = facultyRepository.findById(f2User.getId()).orElseThrow();
        assertEquals("Assistant Professor", f2Profile.getTitle());

        // Verify Student 1: Student A (Reference Profile - Anonymized)
        Student studentA = studentRepository.findByStudentNumber("STU-1001").orElseThrow();
        assertEquals("demo.student.a", studentA.getUser().getUsername());
        assertEquals("Student A — Reference", studentA.getUser().getName());
        RiskAssessment aRisk = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(studentA.getId()).orElseThrow();
        assertEquals(RiskLevel.LOW, aRisk.getRiskLevel(), "Student A must have LOW risk");
        assertEquals(0.0, aRisk.getRiskScore(), 0.1, "Student A risk score must be 0.0");

        // Verify Student A exact attendance: 275/276 = 99.64%
        StudentAcademicAnalyticsDto aAnalytics = academicAnalyticsService.getStudentAnalytics(studentA.getId());
        assertEquals(276, aAnalytics.getTotalClasses());
        assertEquals(275, aAnalytics.getPresentClasses());
        assertEquals(1, aAnalytics.getAbsentClasses());
        assertEquals(99.64, aAnalytics.getOverallAttendancePercentage(), 0.01);
        assertEquals("STUDENT-SUPPLIED REFERENCE DATA", aAnalytics.getProvenance());

        // Verify Student A exact converted marks sum = 150.44
        double convertedSum = aAnalytics.getAssessmentBreakdown().stream()
                .filter(a -> a.getConvertedMarks() != null)
                .mapToDouble(a -> a.getConvertedMarks())
                .sum();
        assertEquals(150.44, convertedSum, 0.05, "Student A converted marks sum must equal exactly 150.44");

        // Verify Student 2: Student B — Top Performer (LOW risk)
        Student studentB = studentRepository.findByStudentNumber("STU-1002").orElseThrow();
        assertEquals("demo.student.b", studentB.getUser().getUsername());
        RiskAssessment bRisk = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(studentB.getId()).orElseThrow();
        assertEquals(RiskLevel.LOW, bRisk.getRiskLevel(), "Student B must be LOW risk");
        assertEquals(0.0, bRisk.getRiskScore(), 0.1, "Student B risk score should be 0.0");

        // Verify Student 3: Student C — Average Performer (MEDIUM risk)
        Student studentC = studentRepository.findByStudentNumber("STU-1003").orElseThrow();
        assertEquals("demo.student.c", studentC.getUser().getUsername());
        RiskAssessment cRisk = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(studentC.getId()).orElseThrow();
        assertEquals(RiskLevel.MEDIUM, cRisk.getRiskLevel(), "Student C must be MEDIUM risk");
        assertTrue(cRisk.getRiskScore() >= 35.0 && cRisk.getRiskScore() < 65.0, "Student C risk score must be in medium range (35-65)");

        // Verify Student 4: Student D — High Risk (HIGH risk)
        Student studentD = studentRepository.findByStudentNumber("STU-1004").orElseThrow();
        assertEquals("demo.student.d", studentD.getUser().getUsername());
        RiskAssessment dRisk = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(studentD.getId()).orElseThrow();
        assertEquals(RiskLevel.HIGH, dRisk.getRiskLevel(), "Student D must be HIGH risk");
        assertTrue(dRisk.getRiskScore() >= 65.0, "Student D risk score should be in high range (>= 65)");
    }

    @Test
    @DisplayName("2. Seeding preserves existing real application users without overwriting or deleting them")
    public void testSeedingPreservesRealApplicationUsers() {
        User realUser = User.builder()
                .username("real_student_john")
                .passwordHash(passwordEncoder.encode("SecretPassword123!"))
                .role(Role.STUDENT)
                .name("John Doe (Real Student)")
                .email("john.doe@srmap.edu.in")
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepository.save(realUser);
        String realUserHash = realUser.getPasswordHash();

        boolean seeded = dataSeeder.seedDemoData();
        assertTrue(seeded, "Seeding should safely complete demo accounts");

        // Verify real user remains completely intact
        User reloadedReal = userRepository.findByUsername("real_student_john").orElseThrow();
        assertEquals("John Doe (Real Student)", reloadedReal.getName());
        assertEquals("SecretPassword123!", "SecretPassword123!");
        assertTrue(passwordEncoder.matches("SecretPassword123!", reloadedReal.getPasswordHash()), "Real user password must NOT be modified");
        assertEquals(AuthProvider.LOCAL, reloadedReal.getAuthProvider());
    }

    @Test
    @DisplayName("3. Repeated initialization is idempotent and produces no duplicates")
    public void testRepeatedInitialization_NoDuplicates() {
        boolean firstRun = dataSeeder.seedDemoData();
        assertTrue(firstRun, "First run should seed data");
        long studentCountAfterFirst = studentRepository.count();
        assertEquals(4, studentCountAfterFirst);

        boolean secondRun = dataSeeder.seedDemoData();
        assertTrue(secondRun, "Second run should report success idempotently");

        assertEquals(studentCountAfterFirst, studentRepository.count(), "Student count must not duplicate on second run");
        assertEquals(2, facultyRepository.count(), "Faculty count must remain exactly 2");
    }

    @Test
    @DisplayName("4. Passwords are encrypted with configured BCrypt password encoder")
    public void testPasswordsStoredUsingConfiguredPasswordEncoder() {
        dataSeeder.seedDemoData();

        for (String username : new String[]{"admin", "demo.faculty.1", "demo.faculty.2", "demo.student.a", "demo.student.b", "demo.student.c", "demo.student.d"}) {
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

        // 1. Authenticate as Student 'demo.student.a'
        LoginRequest studentLogin = new LoginRequest();
        studentLogin.setUsername("demo.student.a");
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

        // 2. Authenticate as Demo Faculty 'demo.faculty.1'
        LoginRequest facultyLogin = new LoginRequest();
        facultyLogin.setUsername("demo.faculty.1");
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
    @DisplayName("6. Demo status endpoint is public and exposes metadata with 4 students and 2 faculty")
    public void testDemoStatusEndpoint_PublicAndSafe() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/demo-status"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("demoModeEnabled").asBoolean());
        assertEquals("DEMO_SHOWCASE", json.get("demoMarker").asText());
        assertEquals("demo.student.a", json.get("defaultStudentUsername").asText());
        assertEquals("demo.faculty.1", json.get("defaultFacultyUsername").asText());
        assertEquals("admin", json.get("defaultAdminUsername").asText());
        assertTrue(json.has("students"));
        assertEquals(4, json.get("students").size());
        assertTrue(json.has("facultyList"));
        assertEquals(2, json.get("facultyList").size());
    }

    private String obtainToken(String username, String password) throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("7. Faculty dashboard endpoints provide rich course-specific cohort data for CSE 204 and CSE 213")
    public void testFacultyDashboardEndpoints() throws Exception {
        dataSeeder.seedDemoData();

        // Faculty 1 (CSE 204)
        String faculty1Token = obtainToken("demo.faculty.1", "DemoEduPulse2026!");
        MvcResult fac1DashboardResult = mockMvc.perform(get("/api/faculty/me/dashboard")
                        .header("Authorization", "Bearer " + faculty1Token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode fac1Json = objectMapper.readTree(fac1DashboardResult.getResponse().getContentAsString());
        assertEquals("CSE 204", fac1Json.get("courseCode").asText());
        assertEquals("DESIGN AND ANALYSIS OF ALGORITHMS", fac1Json.get("courseName").asText());
        assertEquals(4, fac1Json.get("enrolledStudentsCount").asInt());
        assertEquals(48, fac1Json.get("totalConductedSessions").asInt());
        assertTrue(fac1Json.has("students"));
        assertEquals(4, fac1Json.get("students").size());
        assertTrue(fac1Json.has("assessments"));
        assertTrue(fac1Json.has("riskEngineSummary"));

        // Verify summary endpoint also includes faculty view
        MvcResult summaryResult = mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + faculty1Token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode summaryJson = objectMapper.readTree(summaryResult.getResponse().getContentAsString());
        assertTrue(summaryJson.has("facultyView"));
        assertEquals("CSE 204", summaryJson.get("facultyView").get("courseCode").asText());

        // Faculty 2 (CSE 213)
        String faculty2Token = obtainToken("demo.faculty.2", "DemoEduPulse2026!");
        MvcResult fac2DashboardResult = mockMvc.perform(get("/api/faculty/me/dashboard")
                        .header("Authorization", "Bearer " + faculty2Token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode fac2Json = objectMapper.readTree(fac2DashboardResult.getResponse().getContentAsString());
        assertEquals("CSE 213", fac2Json.get("courseCode").asText());
        assertEquals("ADVANCED JAVA PROGRAMMING", fac2Json.get("courseName").asText());
        assertEquals(4, fac2Json.get("enrolledStudentsCount").asInt());
        assertEquals(49, fac2Json.get("totalConductedSessions").asInt());
    }
}
