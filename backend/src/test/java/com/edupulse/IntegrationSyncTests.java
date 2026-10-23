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
        if (restTemplate.getRestTemplate().getRequestFactory() instanceof org.springframework.http.client.SimpleClientHttpRequestFactory factory) {
            factory.setBufferRequestBody(true);
        }

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
    @Autowired
    private com.edupulse.repository.AuditLogRepository auditLogRepository;

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
        assertThat(response.getBody().get("provenance")).isEqualTo("SYNTHETIC_TEST_FIXTURE");
        
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

    @Test
    void testCsvImportByAdminSucceedsAndRecordsAuditLog() {
        String csvContent = "student_number,period,course_code,attendance_pct,marks_obtained,max_marks\n" +
                "STU-999,Midterm,CS101,80.0,40.0,50.0\n";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(adminToken);

        org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test_import.csv";
            }
        };
        body.add("file", resource);

        HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/data-import/csv", requestEntity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("SUCCESS");
        assertThat(response.getBody().get("provenance")).isEqualTo("CSV_FACULTY_IMPORT");

        // Verify audit log entry
        boolean hasAudit = auditLogRepository.findAll().stream().anyMatch(a -> "CSV_ACADEMIC_IMPORT".equals(a.getAction()));
        assertThat(hasAudit).isTrue();
    }

    @Test
    void testCsvImportTwicePreventsAttendanceDuplication() {
        Student student = studentRepository.findByStudentNumber("STU-999").get();
        String csvContent = "student_number,period,course_code,attendance_pct,marks_obtained,max_marks\n" +
                "STU-999,Finals,CS202,70.0,45.0,50.0\n";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(adminToken);

        org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test_idempotent.csv";
            }
        };
        body.add("file", resource);

        // Upload 1st time
        ResponseEntity<Map> response1 = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/data-import/csv", new HttpEntity<>(body, headers), Map.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response1.getBody().get("status")).isEqualTo("SUCCESS");

        long attendanceCountAfterFirst = attendanceRepository.findAll().stream().filter(a -> a.getStudent().getId().equals(student.getId())).count();

        // Upload 2nd time with same content
        ResponseEntity<Map> response2 = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/data-import/csv", new HttpEntity<>(body, headers), Map.class);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        long attendanceCountAfterSecond = attendanceRepository.findAll().stream().filter(a -> a.getStudent().getId().equals(student.getId())).count();

        // Attendance records should not have multiplied
        assertThat(attendanceCountAfterSecond).isEqualTo(attendanceCountAfterFirst);
    }

    @Test
    void testStudentForbiddenFromCsvImport() {
        String csvContent = "student_number,period,course_code,attendance_pct,marks_obtained,max_marks\n" +
                "STU-999,Finals,CS202,70.0,45.0,50.0\n";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(studentToken); // Student role

        org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "student_forbidden.csv";
            }
        };
        body.add("file", resource);

        ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/data-import/csv", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testDuplicateBatchImportCountsAsDuplicates() {
        String csvContent = "student_number,period,course_code,attendance_pct,marks_obtained,max_marks\n" +
                "STU-999,2026-SEM1,CS303,80.0,40.0,50.0\n";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(adminToken);

        org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test_duplicate_batch.csv";
            }
        };
        body.add("file", resource);

        // First upload
        ResponseEntity<Map> response1 = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/data-import/csv", new HttpEntity<>(body, headers), Map.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map summary1 = (Map) response1.getBody().get("summary");
        assertThat(summary1.get("acceptedRecords")).isEqualTo(1);
        assertThat(summary1.get("duplicateRecords")).isEqualTo(0);

        // Second upload of exact same content
        ResponseEntity<Map> response2 = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/data-import/csv", new HttpEntity<>(body, headers), Map.class);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map summary2 = (Map) response2.getBody().get("summary");
        assertThat(summary2.get("acceptedRecords")).isEqualTo(0);
        assertThat(summary2.get("duplicateRecords")).isEqualTo(1);
    }

    @Test
    void testCsvImportWithInvalidRowsAndRangeErrors() {
        String csvContent = "student_number,period,course_code,attendance_pct,marks_obtained,max_marks\n" +
                "STU-NONEXISTENT,2026-SEM1,CS303,80.0,40.0,50.0\n" +
                "STU-999,2026-SEM1,CS303,150.0,40.0,50.0\n" +
                "STU-999,2026-SEM1,CS303,80.0,-5.0,50.0\n" +
                "STU-999,2026-SEM1,CS303,80.0,60.0,50.0\n";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(adminToken);

        org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test_invalid_rows.csv";
            }
        };
        body.add("file", resource);

        ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/data-import/csv", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("FAILED");
        Map summary = (Map) response.getBody().get("summary");
        assertThat(summary.get("rejectedRecords")).isEqualTo(4);
        assertThat(summary.get("acceptedRecords")).isEqualTo(0);

        java.util.List errors = (java.util.List) response.getBody().get("errors");
        assertThat(errors).hasSize(4);
    }

    @Test
    void testAtomicBatchRollbackOnFailure() {
        String csvContent = "student_number,period,course_code,attendance_pct,marks_obtained,max_marks\n" +
                "STU-999,AtomicPeriod,CS999,90.0,45.0,50.0\n" +
                "STU-NONEXISTENT,AtomicPeriod,CS999,90.0,45.0,50.0\n";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(adminToken);

        org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test_atomic.csv";
            }
        };
        body.add("file", resource);

        // Upload with atomic=true
        ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/data-import/csv?atomic=true", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Verify row 1 was rolled back and NOT persisted
        Integer courseCount = jdbcTemplate.queryForObject("SELECT count(*) FROM courses WHERE code = 'CS999'", Integer.class);
        assertThat(courseCount).isEqualTo(0);
    }

    @Test
    void testCrossStudentAccessForbidden() {
        // Create second student
        User otherUser = userRepository.findByUsername("otherstudent").orElseGet(() -> {
            User newUser = User.builder()
                .username("otherstudent")
                .email("otherstudent@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .name("Other Student")
                .build();
            return userRepository.save(newUser);
        });

        if (studentRepository.findById(otherUser.getId()).isEmpty()) {
            jdbcTemplate.update("INSERT INTO students (user_id, student_number, department, current_semester, current_cgpa) VALUES (?, ?, ?, ?, ?)",
                otherUser.getId(), "STU-888", "EE", 2, 8.5);
        }

        User studentUser = userRepository.findByUsername("teststudent").get();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken); // Logged in as teststudent

        // Accessing own record is allowed
        ResponseEntity<Map> ownResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/students/" + studentUser.getId(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class
        );
        assertThat(ownResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownResponse.getBody().get("studentNumber")).isEqualTo("STU-999");

        // Accessing another student's record is forbidden
        ResponseEntity<Map> otherResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/students/" + otherUser.getId(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class
        );
        assertThat(otherResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Accessing another student's interventions is forbidden
        ResponseEntity<String> interventionResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/interventions/student/" + otherUser.getId(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
        assertThat(interventionResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testAuditLogSafeDetailsAndFingerprint() {
        String csvContent = "student_number,period,course_code,attendance_pct,marks_obtained,max_marks\n" +
                "STU-999,AuditPeriod,CS404,80.0,40.0,50.0\n";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(adminToken);

        org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test_audit_fingerprint.csv";
            }
        };
        body.add("file", resource);

        ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/data-import/csv", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify audit log has fingerprint and no sensitive data
        java.util.List<com.edupulse.model.AuditLog> logs = auditLogRepository.findAll().stream()
            .filter(a -> "CSV_ACADEMIC_IMPORT".equals(a.getAction()))
            .toList();
        
        assertThat(logs).isNotEmpty();
        com.edupulse.model.AuditLog latestLog = logs.get(logs.size() - 1);
        
        assertThat(latestLog.getDetails()).contains("fingerprint=sha256:");
        assertThat(latestLog.getDetails()).contains("source=CSV_FACULTY_IMPORT");
        assertThat(latestLog.getDetails()).contains("batchId=");
        assertThat(latestLog.getDetails()).doesNotContain("password");
        assertThat(latestLog.getDetails()).doesNotContain("secret");
        assertThat(latestLog.getDetails()).doesNotContain("token");
    }
}
