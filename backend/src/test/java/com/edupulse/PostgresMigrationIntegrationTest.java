package com.edupulse;

import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.service.RiskAssessmentService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the real Flyway migrations against PostgreSQL (same major version as docker-compose) and
 * boots the application with {@code ddl-auto=validate}. Fails if any entity drifts from the schema
 * that production will actually have. Skipped automatically when Docker is unavailable; always
 * runs in CI.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.show-sql=false",
        "app.seeder.enabled=false"
})
class PostgresMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private Flyway flyway;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private RiskAssessmentRepository riskAssessmentRepository;
    @Autowired private RiskAssessmentService riskAssessmentService;

    @Test
    @DisplayName("All Flyway migrations apply cleanly and Hibernate validates the schema")
    void migrationsApplyAndSchemaValidates() {
        var applied = flyway.info().applied();
        assertTrue(applied.length >= 1, "At least the V1 baseline must be applied");
        assertTrue(java.util.Arrays.stream(applied).allMatch(m -> m.getState().isApplied() && !m.getState().isFailed()),
                "No migration may be failed or pending");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("Core write/read path and risk snapshots work on real PostgreSQL")
    void coreRepositoriesAndRiskEngineWorkOnPostgres() {
        User user = userRepository.save(User.builder()
                .username("pg.it.student").passwordHash("x").role(Role.STUDENT)
                .name("PG IT Student").authProvider(AuthProvider.LOCAL).build());
        Student student = studentRepository.save(Student.builder()
                .user(user).studentNumber("PG-IT-0001").department("CSE").build());
        Course course = courseRepository.save(Course.builder().code("PGIT 101").name("PG IT").credits(3).build());
        for (int i = 0; i < 10; i++) {
            attendanceRepository.save(Attendance.builder()
                    .student(student).course(course).date(LocalDate.of(2026, 1, 1).plusDays(i))
                    .status(i < 6 ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT).build());
        }

        RiskAssessment first = riskAssessmentService.ensureCurrentSnapshot(student.getId());
        RiskAssessment second = riskAssessmentService.ensureCurrentSnapshot(student.getId());

        // 60% attendance -> (75-60)*1.5 = 22.5 -> LOW
        assertEquals(22.5, first.getRiskScore());
        assertEquals(RiskLevel.LOW, first.getRiskLevel());
        assertEquals(first.getId(), second.getId(), "Unchanged data must not create a new snapshot");
        assertEquals(1, riskAssessmentRepository.findAll().stream()
                .filter(r -> r.getStudent().getId().equals(student.getId())).count());
    }
}
