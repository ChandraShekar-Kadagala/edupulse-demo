package com.edupulse;

import com.edupulse.model.AuthProvider;
import com.edupulse.model.Role;
import com.edupulse.model.User;
import com.edupulse.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1.1: admin-triggered provisioning is idempotent and never modifies real (non-DEMO) accounts.
 * No DEMO_ADMIN_PASSWORD is configured here, so the admin account must not be created or touched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.seeder.enabled=true"})
class DemoProvisioningIdempotencyTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private FacultyRepository facultyRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private com.edupulse.repository.EnrollmentRepository enrollmentRepository;
    @Autowired private com.edupulse.repository.CourseAssignmentRepository courseAssignmentRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private MarkRepository markRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentSubmissionRepository submissionRepository;
    @Autowired private InterventionRepository interventionRepository;
    @Autowired private RiskAssessmentRepository riskAssessmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        interventionRepository.deleteAll();
        riskAssessmentRepository.deleteAll();
        submissionRepository.deleteAll();
        assignmentRepository.deleteAll();
        markRepository.deleteAll();
        assessmentRepository.deleteAll();
        attendanceRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseAssignmentRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        facultyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Repeated provisioning is idempotent and preserves real accounts")
    void provisioningIsIdempotentAndPreservesRealUsers() throws Exception {
        String realHash = passwordEncoder.encode("a-real-admins-private-password");
        userRepository.save(User.builder()
                .username("admin").passwordHash(realHash).role(Role.ADMIN)
                .name("Real Admin").email("real-admin@example.org")
                .authProvider(AuthProvider.LOCAL).build());

        mockMvc.perform(post("/api/admin/demo/provision")).andExpect(status().isOk());
        long usersAfterFirst = userRepository.count();
        long studentsAfterFirst = studentRepository.count();

        mockMvc.perform(post("/api/admin/demo/provision")).andExpect(status().isOk());
        assertEquals(usersAfterFirst, userRepository.count(), "Second run must not create users");
        assertEquals(studentsAfterFirst, studentRepository.count(), "Second run must not create students");
        assertEquals(4, studentsAfterFirst);

        User admin = userRepository.findByUsername("admin").orElseThrow();
        assertEquals(realHash, admin.getPasswordHash(), "Real admin password must be untouched");
        assertEquals(AuthProvider.LOCAL, admin.getAuthProvider());
        assertEquals("Real Admin", admin.getName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Without DEMO_ADMIN_PASSWORD no admin account is created; only 6 demo accounts exist")
    void noAdminCreatedWithoutDedicatedPassword() throws Exception {
        mockMvc.perform(post("/api/admin/demo/provision")).andExpect(status().isOk());
        assertTrue(userRepository.findByUsername("admin").isEmpty());
        assertEquals(6, userRepository.count(), "Exactly 4 demo students + 2 demo faculty");
        userRepository.findAll().forEach(u -> assertEquals(AuthProvider.DEMO, u.getAuthProvider()));
    }
}
