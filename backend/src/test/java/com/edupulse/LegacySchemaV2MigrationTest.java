package com.edupulse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rehearses V2 against the shape of the existing production database: a schema created by Hibernate
 * ddl-auto=update before Flyway existed (Hibernate/pg_dump constraint names), later baselined at V1.
 * The application is booted exactly like production (Flyway baseline-on-migrate, ddl-auto=validate).
 */
@Testcontainers(disabledWithoutDocker = true)
class LegacySchemaV2MigrationTest {

    private static final String[] TABLES = {
            "users", "students", "faculty", "courses", "attendance", "assessments", "marks",
            "interventions", "follow_ups", "risk_assessments", "risk_factors", "enrollments",
            "assignments", "assignment_submissions", "audit_logs", "password_reset_tokens", "encrypted_risk_records"};

    private PostgreSQLContainer<?> newLegacyDatabase() throws Exception {
        PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:15-alpine");
        pg.start();
        try (Connection c = connect(pg); Statement st = c.createStatement()) {
            String ddl = new String(getClass().getResourceAsStream("/fixtures/legacy-hibernate-schema-2026-10.sql")
                    .readAllBytes(), StandardCharsets.UTF_8);
            st.execute(ddl);
            // Columns production must already have: it runs today's entities with ddl-auto=validate.
            st.execute("ALTER TABLE users ADD COLUMN token_version bigint DEFAULT 1");
            st.execute("ALTER TABLE users ADD COLUMN auth_provider character varying(255) DEFAULT 'LOCAL'");
            seed(st);
        }
        return pg;
    }

    private static void seed(Statement st) throws Exception {
        st.execute("""
            INSERT INTO users (id, created_at, email, name, password_hash, role, username, token_version, auth_provider) VALUES
              (1, now(), 'real-admin@example.org', 'Real Admin', 'hash-real-admin', 'ADMIN', 'admin', 1, 'LOCAL'),
              (2, now(), 'f1@example.org', 'Faculty A', 'hash-demo', 'FACULTY', 'demo.faculty.1', 1, 'DEMO'),
              (3, now(), 'f2@example.org', 'Faculty B', 'hash-demo', 'FACULTY', 'demo.faculty.2', 1, 'DEMO'),
              (4, now(), 'a@example.org', 'Student A', 'hash-demo', 'STUDENT', 'demo.student.a', 1, 'DEMO'),
              (5, now(), 'b@example.org', 'Student B', 'hash-demo', 'STUDENT', 'demo.student.b', 1, 'DEMO'),
              (6, now(), 'c@example.org', 'Student C', 'hash-demo', 'STUDENT', 'demo.student.c', 1, 'DEMO'),
              (7, now(), 'd@example.org', 'Student D', 'hash-demo', 'STUDENT', 'demo.student.d', 1, 'DEMO'),
              (8, now(), 'r@example.org', 'Real Student', 'hash-real', 'STUDENT', 'real.student', 1, 'LOCAL'),
              (9, now(), 'rf@example.org', 'Real Faculty', 'hash-real-f', 'FACULTY', 'real.faculty', 1, NULL)
            """);
        st.execute("INSERT INTO faculty (user_id, department, title) VALUES (2,'CSE','Associate Professor'),(3,'CSE','Assistant Professor'),(9,'CSE','Professor')");
        st.execute("""
            INSERT INTO students (user_id, current_cgpa, current_semester, department, student_number) VALUES
              (4, 9.1, 3, 'CSE', 'STU-1001'), (5, 9.4, 3, 'CSE', 'STU-1002'), (6, 7.2, 3, 'CSE', 'STU-1003'),
              (7, 5.1, 3, 'CSE', 'STU-1004'), (8, 8.0, 3, 'CSE', 'REAL-0001')
            """);
        st.execute("INSERT INTO courses (id, code, credits, department, name) VALUES (1,'CSE 204',4,'CSE','DAA'),(2,'CSE 213',4,'CSE','AJP'),(3,'CSE 203',3,'CSE','DM')");
        st.execute("INSERT INTO attendance (date, status, course_id, student_id) SELECT DATE '2026-08-01' + g, CASE WHEN g % 4 = 0 THEN 'ABSENT' ELSE 'PRESENT' END, 1 + g % 3, 4 + g % 5 FROM generate_series(0, 59) g");
        st.execute("INSERT INTO assessments (id, max_marks, name, type, weightage, course_id) VALUES (1, 20, 'CLA 1', 'INTERNAL', 8, 1)");
        st.execute("INSERT INTO marks (marks_obtained, assessment_id, student_id) VALUES (15, 1, 4), (9, 1, 7), (17, 1, 8)");
        st.execute("INSERT INTO risk_assessments (id, calculated_at, risk_level, risk_score, student_id) VALUES (1, now(), 'HIGH', 74.8, 7)");
        st.execute("INSERT INTO interventions (id, created_at, description, status, type, assigned_faculty_id, student_id) VALUES (1, now(), 'Recovery plan', 'IN_PROGRESS', 'Tutoring', 2, 7)");
        st.execute("INSERT INTO follow_ups (created_at, notes, outcome, previous_risk_score, new_risk_score, faculty_id, intervention_id) VALUES (now(), 'n', 'IMPROVED', 74.8, 60.1, 2, 1)");
    }

    private static Connection connect(PostgreSQLContainer<?> pg) throws Exception {
        return DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
    }

    private static Map<String, String> fingerprint(Connection c) throws Exception {
        Map<String, String> out = new LinkedHashMap<>();
        try (Statement st = c.createStatement()) {
            for (String t : TABLES) {
                try (ResultSet rs = st.executeQuery("SELECT count(*) || ':' || md5(coalesce(string_agg(j, '|' ORDER BY j), '')) FROM "
                        + "(SELECT (to_jsonb(x) - 'data_provenance' - 'import_batch_id')::text j FROM " + t + " x) s")) {
                    rs.next();
                    out.put(t, rs.getString(1));
                }
            }
        }
        return out;
    }

    private static ConfigurableApplicationContext bootLikeProduction(PostgreSQLContainer<?> pg) {
        // Command-line arguments outrank application*.yml (builder.properties() would not).
        return new SpringApplicationBuilder(EdupulseApplication.class)
                .profiles("test")
                .run(
                        "--spring.datasource.url=" + pg.getJdbcUrl(),
                        "--spring.datasource.username=" + pg.getUsername(),
                        "--spring.datasource.password=" + pg.getPassword(),
                        "--spring.datasource.driver-class-name=org.postgresql.Driver",
                        "--spring.flyway.enabled=true",
                        "--spring.flyway.baseline-on-migrate=true",
                        "--spring.flyway.locations=classpath:db/migration",
                        "--spring.jpa.hibernate.ddl-auto=validate",
                        "--spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                        "--spring.jpa.show-sql=false",
                        "--app.seeder.enabled=false",
                        "--jwt.secret=======================EduPulseSecretKeyForTestingPurpose======================",
                        "--server.port=0");
    }

    @Test
    @DisplayName("V2 on a legacy (pre-Flyway, baselined) schema: applies, preserves all data, app validates")
    void v2AppliesToLegacySchemaAndPreservesData() throws Exception {
        PostgreSQLContainer<?> pg = newLegacyDatabase();
        try {
            Map<String, String> before;
            try (Connection c = connect(pg)) {
                before = fingerprint(c);
            }

            try (ConfigurableApplicationContext ctx = bootLikeProduction(pg)) {
                assertTrue(ctx.isActive(), "Application must start with ddl-auto=validate after V2");
            }

            try (Connection c = connect(pg); Statement st = c.createStatement()) {
                assertEquals(before, fingerprint(c), "Every pre-existing row must be byte-for-byte unchanged");

                try (ResultSet rs = st.executeQuery("SELECT version, type, success FROM flyway_schema_history ORDER BY installed_rank")) {
                    assertTrue(rs.next());
                    assertEquals("1", rs.getString(1));
                    assertEquals("BASELINE", rs.getString(2));
                    assertTrue(rs.next());
                    assertEquals("2", rs.getString(1));
                    assertEquals("SQL", rs.getString(2));
                    assertTrue(rs.getBoolean(3));
                    assertTrue(rs.next());
                    assertEquals("3", rs.getString(1));  // V3 academic imports
                    assertEquals("SQL", rs.getString(2));
                    assertTrue(rs.getBoolean(3));
                    assertFalse(rs.next());
                }
                try (ResultSet rs = st.executeQuery("SELECT u.username, c.code FROM course_assignments ca JOIN users u ON u.id = ca.faculty_id JOIN courses c ON c.id = ca.course_id ORDER BY 1")) {
                    assertTrue(rs.next()); assertEquals("demo.faculty.1", rs.getString(1)); assertEquals("CSE 204", rs.getString(2));
                    assertTrue(rs.next()); assertEquals("demo.faculty.2", rs.getString(1)); assertEquals("CSE 213", rs.getString(2));
                    assertFalse(rs.next(), "Real faculty must not receive invented assignments");
                }
                try (ResultSet rs = st.executeQuery("SELECT student_number, data_provenance FROM students ORDER BY student_number")) {
                    Map<String, String> prov = new LinkedHashMap<>();
                    while (rs.next()) prov.put(rs.getString(1), rs.getString(2));
                    assertEquals("STUDENT_SUPPLIED_REFERENCE", prov.get("STU-1001"));
                    assertEquals("SYNTHETIC_TEST_FIXTURE", prov.get("STU-1002"));
                    assertEquals("SYNTHETIC_TEST_FIXTURE", prov.get("STU-1004"));
                    assertNull(prov.get("REAL-0001"), "Real students keep NULL provenance");
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM pg_constraint WHERE conrelid = 'users'::regclass AND contype = 'c' AND pg_get_constraintdef(oid) ILIKE '%role%'")) {
                    rs.next();
                    assertEquals(1, rs.getInt(1), "Exactly one (widened) role constraint");
                }
                st.execute("BEGIN");
                st.execute("UPDATE users SET role = 'HOD' WHERE id = 9");
                st.execute("ROLLBACK");
            }
        } finally {
            pg.stop();
        }
    }

    @Test
    @DisplayName("A failed precondition rolls V2 back completely and the app does not start")
    void preconditionFailureRollsBackEverything() throws Exception {
        PostgreSQLContainer<?> pg = newLegacyDatabase();
        try {
            try (Connection c = connect(pg); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE course_assignments (id bigint)"); // unexpected pre-existing object
            }
            Map<String, String> before;
            try (Connection c = connect(pg)) { before = fingerprint(c); }

            assertThrows(Exception.class, () -> bootLikeProduction(pg).close());

            try (Connection c = connect(pg); Statement st = c.createStatement()) {
                assertEquals(before, fingerprint(c));
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_name = 'students' AND column_name = 'data_provenance'")) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "No partial V2 changes may remain");
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE version = '2' AND success")) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "Failed V2 must not be recorded as applied");
                }
            }
        } finally {
            pg.stop();
        }
    }

    @Test
    @DisplayName("The data-preservation guard aborts V2 if any pre-existing value changes")
    void dataPreservationGuardDetectsUnexpectedChanges() throws Exception {
        PostgreSQLContainer<?> pg = newLegacyDatabase();
        try {
            try (Connection c = connect(pg); Statement st = c.createStatement()) {
                // Simulate an unexpected side effect: a trigger that rewrites an existing column.
                st.execute("CREATE FUNCTION sneaky() RETURNS trigger AS $$ BEGIN NEW.department := 'CHANGED'; RETURN NEW; END $$ LANGUAGE plpgsql");
                st.execute("CREATE TRIGGER sneaky_trg BEFORE UPDATE ON students FOR EACH ROW EXECUTE FUNCTION sneaky()");
            }
            Map<String, String> before;
            try (Connection c = connect(pg)) { before = fingerprint(c); }

            Exception ex = assertThrows(Exception.class, () -> bootLikeProduction(pg).close());
            Throwable root = ex;
            while (root.getCause() != null) root = root.getCause();
            assertTrue(root.getMessage().contains("data-preservation check failed"), root.getMessage());

            try (Connection c = connect(pg)) {
                assertEquals(before, fingerprint(c), "Guard failure must leave the database untouched");
            }
        } finally {
            pg.stop();
        }
    }
}
