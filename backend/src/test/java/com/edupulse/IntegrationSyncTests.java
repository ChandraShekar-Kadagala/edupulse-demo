package com.edupulse;

import com.edupulse.model.User;
import com.edupulse.model.Student;
import com.edupulse.model.Role;
import com.edupulse.repository.UserRepository;
import com.edupulse.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class IntegrationSyncTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String studentToken;
    private String adminToken;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        User studentUser = userRepository.findByUsername("teststudent").orElseGet(() -> {
            User newUser = User.builder()
                .username("teststudent")
                .email("teststudent@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .name("Test Student")
                .build();
            return userRepository.save(newUser);
        });

        if (studentRepository.findById(studentUser.getId()).isEmpty()) {
            jdbcTemplate.update("INSERT INTO students (user_id, student_number, department, current_semester, current_cgpa) VALUES (?, ?, ?, ?, ?)",
                studentUser.getId(), "STU-999", "CS", 1, 0.0);
        }

        if (!userRepository.findByUsername("testadmin").isPresent()) {
            User admin = User.builder()
                .username("testadmin")
                .email("testadmin@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .name("Test Admin")
                .build();
            userRepository.save(admin);
        }

        ResponseEntity<Map> studentLogin = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login",
            Map.of("username", "teststudent", "password", "password"),
            Map.class
        );
        studentToken = (String) studentLogin.getBody().get("token");

        ResponseEntity<Map> adminLogin = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login",
            Map.of("username", "testadmin", "password", "password"),
            Map.class
        );
        adminToken = (String) adminLogin.getBody().get("token");
    }

    @Test
    void testGetIntegrationStatus() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        ResponseEntity<Map> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/integration/status",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UNSUPPORTED_ACCESS");
    }

    @Test
    void testOfficialSyncReturns403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        ResponseEntity<Map> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/integration/link",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("message")).isEqualTo("No official public API exists. Cannot sync data.");
    }

    @Autowired
    private com.edupulse.repository.MarkRepository markRepository;
    @Autowired
    private com.edupulse.repository.AttendanceRepository attendanceRepository;
    @Autowired
    private com.edupulse.repository.RiskAssessmentRepository riskAssessmentRepository;

    @Test
    void testDemoSyncSuccessfullyUpdatesData() {
        Student student = studentRepository.findByStudentNumber("STU-999").get();
        int initialMarks = markRepository.findByStudentId(student.getId()).size();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        ResponseEntity<Map> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/integration/link?provider=demo",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Assert Database State
        long newMarks = markRepository.findByStudentId(student.getId()).size();
        assertThat(newMarks).isGreaterThan(initialMarks);
        
        long attendances = attendanceRepository.findByStudentIdAndCourseId(student.getId(), 1L).size();
        assertThat(attendances).isGreaterThanOrEqualTo(10);
        
        long risks = riskAssessmentRepository.findByStudentId(student.getId()).size();
        assertThat(risks).isGreaterThan(0);
        
        // Test idempotency / updates
        ResponseEntity<Map> response2 = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/integration/link?provider=demo",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class
        );
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        long duplicateMarks = markRepository.findByStudentId(student.getId()).size();
        assertThat(duplicateMarks).isEqualTo(newMarks); // No duplicates created
    }
}
