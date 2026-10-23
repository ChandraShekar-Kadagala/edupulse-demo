package com.edupulse;

import com.edupulse.dto.StudentAcademicAnalyticsDto;
import com.edupulse.dto.DashboardSummaryDto;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AcademicIntelligenceTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String strongStudentToken;
    private Long strongStudentId;
    private Long weakStudentId;
    private Long emptyStudentId;

    @BeforeEach
    void setUp() {
        if (restTemplate.getRestTemplate().getRequestFactory() instanceof org.springframework.http.client.SimpleClientHttpRequestFactory factory) {
            factory.setBufferRequestBody(true);
        }

        // 1. Admin setup
        userRepository.findByUsername("ai_admin").orElseGet(() -> {
            User admin = User.builder()
                    .username("ai_admin")
                    .email("ai_admin@edupulse.edu")
                    .passwordHash(passwordEncoder.encode("password"))
                    .role(Role.ADMIN)
                    .name("AI Admin")
                    .build();
            return userRepository.save(admin);
        });

        ResponseEntity<Map> adminLogin = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                Map.of("username", "ai_admin", "password", "password"),
                Map.class);
        adminToken = (String) adminLogin.getBody().get("token");

        // 2. Strong student fixture
        User strongUser = userRepository.findByUsername("strong_student").orElseGet(() -> {
            User u = User.builder()
                    .username("strong_student")
                    .email("strong@edupulse.edu")
                    .passwordHash(passwordEncoder.encode("password"))
                    .role(Role.STUDENT)
                    .name("Strong Student")
                    .build();
            return userRepository.save(u);
        });
        strongStudentId = strongUser.getId();

        if (studentRepository.findById(strongStudentId).isEmpty()) {
            jdbcTemplate.update("INSERT INTO students (user_id, student_number, department, current_semester, current_cgpa) VALUES (?, ?, ?, ?, ?)",
                    strongStudentId, "STU-STRONG", "CSE", 4, 9.2);
        }

        ResponseEntity<Map> strongLogin = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                Map.of("username", "strong_student", "password", "password"),
                Map.class);
        strongStudentToken = (String) strongLogin.getBody().get("token");

        Student strongStudent = studentRepository.findById(strongStudentId).get();
        Course strongCourse = courseRepository.findByCode("CS101").orElseGet(() ->
                courseRepository.save(Course.builder().code("CS101").name("Intro to CS").credits(3).department("CSE").build()));

        for (int i = 0; i < 10; i++) {
            LocalDate d = LocalDate.of(2026, 1, 10).plusDays(i);
            if (attendanceRepository.findByStudentIdAndCourseIdAndDate(strongStudent.getId(), strongCourse.getId(), d).isEmpty()) {
                attendanceRepository.save(Attendance.builder().student(strongStudent).course(strongCourse).date(d).status(AttendanceStatus.PRESENT).build());
            }
        }

        Assessment strongAssessment = assessmentRepository.findByCourseIdAndName(strongCourse.getId(), "Midterm").orElseGet(() ->
                assessmentRepository.save(Assessment.builder().course(strongCourse).name("Midterm").maxMarks(100.0).type(AssessmentType.INTERNAL).build()));

        if (markRepository.findByStudentIdAndAssessmentId(strongStudent.getId(), strongAssessment.getId()).isEmpty()) {
            markRepository.save(Mark.builder().student(strongStudent).assessment(strongAssessment).marksObtained(90.0).build());
        }

        // 3. Weak student fixture
        User weakUser = userRepository.findByUsername("weak_student").orElseGet(() -> {
            User u = User.builder()
                    .username("weak_student")
                    .email("weak@edupulse.edu")
                    .passwordHash(passwordEncoder.encode("password"))
                    .role(Role.STUDENT)
                    .name("Weak Student")
                    .build();
            return userRepository.save(u);
        });
        weakStudentId = weakUser.getId();

        if (studentRepository.findById(weakStudentId).isEmpty()) {
            jdbcTemplate.update("INSERT INTO students (user_id, student_number, department, current_semester, current_cgpa) VALUES (?, ?, ?, ?, ?)",
                    weakStudentId, "STU-WEAK", "CSE", 4, 5.4);
        }

        Student weakStudent = studentRepository.findById(weakStudentId).get();
        Course weakCourse = courseRepository.findByCode("CS201").orElseGet(() ->
                courseRepository.save(Course.builder().code("CS201").name("Data Structures").credits(4).department("CSE").build()));

        // 50% attendance (5 present, 5 absent) -> penalty 37.5
        for (int i = 0; i < 5; i++) {
            LocalDate d = LocalDate.of(2026, 2, 1).plusDays(i);
            if (attendanceRepository.findByStudentIdAndCourseIdAndDate(weakStudent.getId(), weakCourse.getId(), d).isEmpty()) {
                attendanceRepository.save(Attendance.builder().student(weakStudent).course(weakCourse).date(d).status(AttendanceStatus.PRESENT).build());
            }
        }
        for (int i = 5; i < 10; i++) {
            LocalDate d = LocalDate.of(2026, 2, 1).plusDays(i);
            if (attendanceRepository.findByStudentIdAndCourseIdAndDate(weakStudent.getId(), weakCourse.getId(), d).isEmpty()) {
                attendanceRepository.save(Attendance.builder().student(weakStudent).course(weakCourse).date(d).status(AttendanceStatus.ABSENT).build());
            }
        }

        // 40% marks -> penalty 10.0
        Assessment weakAssessment = assessmentRepository.findByCourseIdAndName(weakCourse.getId(), "Data Structures Exam").orElseGet(() ->
                assessmentRepository.save(Assessment.builder().course(weakCourse).name("Data Structures Exam").maxMarks(100.0).type(AssessmentType.INTERNAL).build()));

        if (markRepository.findByStudentIdAndAssessmentId(weakStudent.getId(), weakAssessment.getId()).isEmpty()) {
            markRepository.save(Mark.builder().student(weakStudent).assessment(weakAssessment).marksObtained(40.0).build());
        }

        // 4 missing assignments -> penalty 20.0
        for (int i = 1; i <= 4; i++) {
            final int idx = i;
            Assignment assignment = assignmentRepository.findById((long) (200 + idx)).orElseGet(() ->
                    assignmentRepository.save(Assignment.builder().course(weakCourse).name("HW " + idx).dueDate(LocalDate.now().minusDays(5)).build()));

            if (submissionRepository.findByStudentId(weakStudent.getId()).stream().noneMatch(s -> s.getAssignment().getId().equals(assignment.getId()))) {
                submissionRepository.save(AssignmentSubmission.builder().student(weakStudent).assignment(assignment).submittedAt(null).build());
            }
        }

        // 4. Empty/incomplete student fixture
        User emptyUser = userRepository.findByUsername("empty_student").orElseGet(() -> {
            User u = User.builder()
                    .username("empty_student")
                    .email("empty@edupulse.edu")
                    .passwordHash(passwordEncoder.encode("password"))
                    .role(Role.STUDENT)
                    .name("Empty Student")
                    .build();
            return userRepository.save(u);
        });
        emptyStudentId = emptyUser.getId();

        if (studentRepository.findById(emptyStudentId).isEmpty()) {
            jdbcTemplate.update("INSERT INTO students (user_id, student_number, department, current_semester, current_cgpa) VALUES (?, ?, ?, ?, ?)",
                    emptyStudentId, "STU-EMPTY", "ECE", 1, 0.0);
        }
    }

    @Test
    void testStrongStudentAcademicAnalytics() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(strongStudentToken);

        ResponseEntity<StudentAcademicAnalyticsDto> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/students/" + strongStudentId + "/analytics",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                StudentAcademicAnalyticsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentAcademicAnalyticsDto body = response.getBody();
        assertThat(body).isNotNull();

        // Attendance asserts
        assertThat(body.isHasAttendanceData()).isTrue();
        assertThat(body.getTotalClasses()).isGreaterThanOrEqualTo(10);
        assertThat(body.getOverallAttendancePercentage()).isEqualTo(100.0);

        // Marks asserts
        assertThat(body.isHasMarksData()).isTrue();
        assertThat(body.getOverallAverageMarksPercentage()).isEqualTo(90.0);

        // Risk asserts
        assertThat(body.getRiskScore()).isEqualTo(0.0);
        assertThat(body.getRiskLevel()).isEqualTo(RiskLevel.LOW);

        // Recommendations assert: includes honors or enrichment
        assertThat(body.getRecommendations()).anyMatch(r -> "ENRICHMENT".equals(r.getCategory()));
        assertThat(body.getStrengths()).isNotEmpty();
    }

    @Test
    void testWeakStudentHighRiskAndExplainableFactors() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<StudentAcademicAnalyticsDto> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/students/" + weakStudentId + "/analytics",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                StudentAcademicAnalyticsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentAcademicAnalyticsDto body = response.getBody();
        assertThat(body).isNotNull();

        // Overall risk score = 37.5 + 10.0 + 20.0 = 67.5 -> HIGH
        assertThat(body.getRiskScore()).isGreaterThanOrEqualTo(65.0);
        assertThat(body.getRiskLevel()).isEqualTo(RiskLevel.HIGH);

        // Verify explainable factors and verified inputs
        assertThat(body.getRiskFactors()).anyMatch(f -> f.getFactorType().equals("Low Attendance") && f.getDescription().contains("50.0%"));
        assertThat(body.getRiskFactors()).anyMatch(f -> f.getFactorType().equals("Low Grades") && f.getDescription().contains("40.0%"));
        assertThat(body.getRiskFactors()).anyMatch(f -> f.getFactorType().equals("Missing Assignments"));

        // Actionable recommendations
        assertThat(body.getRecommendations()).anyMatch(r -> "ATTENDANCE".equals(r.getCategory()) && r.getPriority().equals("HIGH"));
        assertThat(body.getRecommendations()).anyMatch(r -> "ACADEMIC_PERFORMANCE".equals(r.getCategory()) && r.getPriority().equals("HIGH"));
        assertThat(body.getRecommendations()).anyMatch(r -> "COURSEWORK".equals(r.getCategory()));
        assertThat(body.getUpcomingConcerns()).isNotEmpty();
    }

    @Test
    void testEmptyStudentMissingDataDistinction() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<StudentAcademicAnalyticsDto> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/students/" + emptyStudentId + "/analytics",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                StudentAcademicAnalyticsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentAcademicAnalyticsDto body = response.getBody();
        assertThat(body).isNotNull();

        // Crucial: Missing data must NOT be represented as 0% or 0 marks
        assertThat(body.isHasAttendanceData()).isFalse();
        assertThat(body.getOverallAttendancePercentage()).isNull();
        assertThat(body.getTotalClasses()).isEqualTo(0);

        assertThat(body.isHasMarksData()).isFalse();
        assertThat(body.getOverallAverageMarksPercentage()).isNull();
        assertThat(body.getTotalAssessments()).isEqualTo(0);

        // Data status must be INSUFFICIENT_DATA
        assertThat(body.getDataStatus()).isEqualTo("INSUFFICIENT_DATA");

        // Student is not unfairly penalized for missing data
        assertThat(body.getRiskScore()).isEqualTo(0.0);
        assertThat(body.getRiskLevel()).isEqualTo(RiskLevel.LOW);

        // Explanations & recommendations call out data collection
        assertThat(body.getRecommendations()).anyMatch(r -> "DATA_COLLECTION".equals(r.getCategory()));
    }

    @Test
    void testCohortDashboardSummaryEndpoint() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<DashboardSummaryDto> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/dashboard/summary",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                DashboardSummaryDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DashboardSummaryDto summary = response.getBody();
        assertThat(summary).isNotNull();

        assertThat(summary.getTotalStudents()).isGreaterThanOrEqualTo(3);
        assertThat(summary.getHighRisk()).isGreaterThanOrEqualTo(1); // Weak student is high risk
        assertThat(summary.getRecentHighRisk()).isNotEmpty();
        assertThat(summary.getRecentHighRisk()).anyMatch(s -> s.getId().equals(weakStudentId));
    }

    @Test
    void testCrossStudentAccessForbiddenOnAnalytics() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(strongStudentToken); // Strong student logged in

        // Accessing own analytics is permitted
        ResponseEntity<StudentAcademicAnalyticsDto> ownResponse = restTemplate.exchange(
                "http://localhost:" + port + "/api/students/" + strongStudentId + "/analytics",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                StudentAcademicAnalyticsDto.class);
        assertThat(ownResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Accessing another student's analytics is strictly forbidden
        ResponseEntity<String> otherResponse = restTemplate.exchange(
                "http://localhost:" + port + "/api/students/" + weakStudentId + "/analytics",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        assertThat(otherResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
