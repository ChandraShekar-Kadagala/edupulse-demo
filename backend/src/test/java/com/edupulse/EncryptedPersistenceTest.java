package com.edupulse;

import com.edupulse.dto.EncryptedRiskRecordDto;
import com.edupulse.model.Role;
import com.edupulse.model.Student;
import com.edupulse.model.User;
import com.edupulse.repository.EncryptedRiskRecordRepository;
import com.edupulse.repository.StudentRepository;
import com.edupulse.repository.UserRepository;
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
public class EncryptedPersistenceTest {

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

    @Autowired
    private EncryptedRiskRecordRepository encryptedRepo;

    private String studentToken1;
    private String studentToken2;

    @BeforeEach
    void setUp() {
        encryptedRepo.deleteAll();
        setupStudent("student_enc1", "STU-E1");
        setupStudent("student_enc2", "STU-E2");

        studentToken1 = login("student_enc1");
        studentToken2 = login("student_enc2");
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private void setupStudent(String username, String stuNo) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = User.builder()
                .username(username)
                .email(username + "@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .name(username)
                .build();
            User saved = userRepository.save(user);
            
            // Bypass hibernate mapsId bug
            jdbcTemplate.update(
                "INSERT INTO students (user_id, student_number, department, current_semester) VALUES (?, ?, ?, ?)",
                saved.getId(), stuNo, "CS", 1
            );
        }
    }

    private String login(String username) {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login",
            Map.of("username", username, "password", "password"),
            Map.class
        );
        return (String) resp.getBody().get("token");
    }

    @Test
    void testCreateAndRetrieveEncryptedRecord() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken1);
        
        EncryptedRiskRecordDto payload = EncryptedRiskRecordDto.builder()
            .ciphertextBase64("dGVzdF9jaXBoZXJ0ZXh0")
            .nonceBase64("dGVzdF9ub25jZQ==")
            .encryptionVersion("1.0")
            .schemaVersion("1.0")
            .build();

        ResponseEntity<EncryptedRiskRecordDto> saveResp = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/privacy/records",
            HttpMethod.POST,
            new HttpEntity<>(payload, headers),
            EncryptedRiskRecordDto.class
        );
        assertThat(saveResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long recordId = saveResp.getBody().getId();
        assertThat(recordId).isNotNull();

        // Retrieve
        ResponseEntity<EncryptedRiskRecordDto[]> getResp = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/privacy/records",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            EncryptedRiskRecordDto[].class
        );
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody()).hasSize(1);
        assertThat(getResp.getBody()[0].getCiphertextBase64()).isEqualTo("dGVzdF9jaXBoZXJ0ZXh0");

        // Cross-user test
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(studentToken2);
        ResponseEntity<EncryptedRiskRecordDto[]> getResp2 = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/privacy/records",
            HttpMethod.GET,
            new HttpEntity<>(headers2),
            EncryptedRiskRecordDto[].class
        );
        assertThat(getResp2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp2.getBody()).isEmpty(); // Should not see student1's record

        // Cross-user delete attempt
        ResponseEntity<Void> delResp2 = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/privacy/records/" + recordId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers2),
            Void.class
        );
        assertThat(delResp2.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
