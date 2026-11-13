package com.edupulse;

import com.edupulse.dto.LoginRequest;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.seeder.DataSeeder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Batch 2: data-driven faculty dashboards, student profiles, follow-up scoring, intervention analytics. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.seeder.enabled=true"})
class Batch2ApiTests {

    private static final String PW = "DemoEduPulse2026!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired private DataSeeder dataSeeder;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private FacultyRepository facultyRepository;
    @Autowired private CourseAssignmentRepository courseAssignmentRepository;
    @Autowired private InterventionRepository interventionRepository;
    @Autowired private FollowUpRepository followUpRepository;
    @Autowired private RiskAssessmentRepository riskAssessmentRepository;
    @Autowired private org.springframework.transaction.support.TransactionTemplate tx;

    @BeforeEach
    void seed() {
        dataSeeder.seedDemoData(); // idempotent
    }

    private String token(String username) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(PW);
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("token").asText();
    }

    private JsonNode getJson(String url, String token) throws Exception {
        var res = mockMvc.perform(get(url).header("Authorization", "Bearer " + token)).andReturn().getResponse();
        if (res.getStatus() != 200) System.out.println("DEBUG " + url + " -> " + res.getStatus() + " " + res.getContentAsString());
        return om.readTree(mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("All six public demo accounts authenticate")
    void sixDemoAccountsAuthenticate() throws Exception {
        for (String u : new String[]{"demo.student.a", "demo.student.b", "demo.student.c", "demo.student.d", "demo.faculty.1", "demo.faculty.2"}) {
            assertFalse(token(u).isBlank(), u);
        }
    }

    @Test
    @DisplayName("Faculty dashboards come from course_assignments with real roster data")
    void facultyDashboardsAreAssignmentDriven() throws Exception {
        JsonNode f1 = getJson("/api/faculty/me/dashboard", token("demo.faculty.1"));
        JsonNode f2 = getJson("/api/faculty/me/dashboard", token("demo.faculty.2"));
        assertEquals("CSE 204", f1.get("courseCode").asText());
        assertEquals("CSE 213", f2.get("courseCode").asText());
        for (JsonNode d : new JsonNode[]{f1, f2}) {
            assertEquals(4, d.get("enrolledStudentsCount").asInt());
            assertEquals(4, d.get("students").size());
            assertTrue(d.get("totalConductedSessions").asLong() > 0);
            assertTrue(d.get("faculty").get("employeeCode").isNull(), "Employee codes are not stored and never invented");
            int levels = d.get("highRiskCount").asInt() + d.get("mediumRiskCount").asInt() + d.get("lowRiskCount").asInt();
            assertEquals(4, levels);
            for (JsonNode s : d.get("students")) {
                long total = s.get("courseTotalClasses").asLong();
                long present = s.get("coursePresentClasses").asLong();
                assertEquals(total, present + s.get("courseAbsentClasses").asLong());
                if (total > 0) {
                    assertEquals(Math.round(present * 1000.0 / total) / 10.0, s.get("courseAttendancePercentage").asDouble(), 0.05);
                }
            }
        }
    }

    @Test
    @DisplayName("Unassigned course -> 403; unknown faculty -> 404; faculty without assignments -> honest empty dashboard")
    void facultyDashboardErrorStates() throws Exception {
        String t = token("demo.faculty.1");
        mockMvc.perform(get("/api/faculty/me/dashboard?courseCode=CSE 213").header("Authorization", "Bearer " + t))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/faculty/999999/dashboard").header("Authorization", "Bearer " + t))
                .andExpect(status().isNotFound());

        Long f2 = userRepository.findByUsername("demo.faculty.2").orElseThrow().getId();
        courseAssignmentRepository.findByFacultyIdWithCourse(f2).forEach(courseAssignmentRepository::delete);
        JsonNode empty = getJson("/api/faculty/me/dashboard", token("demo.faculty.2"));
        assertTrue(empty.get("courseCode").isNull());
        assertEquals(0, empty.get("students").size());
        dataSeeder.seedDemoData(); // restore the assignment
    }

    @Test
    @DisplayName("Four student profiles load with seven subjects, consistent risk breakdown and provenance")
    void fourStudentProfiles() throws Exception {
        String faculty = token("demo.faculty.1");
        String[][] expected = {
                {"STU-1001", "STUDENT-SUPPLIED REFERENCE DATA"}, {"STU-1002", "SYNTHETIC DEMO DATA"},
                {"STU-1003", "SYNTHETIC DEMO DATA"}, {"STU-1004", "SYNTHETIC DEMO DATA"}};
        for (String[] e : expected) {
            Student s = studentRepository.findByStudentNumber(e[0]).orElseThrow();
            JsonNode a = getJson("/api/students/" + s.getId() + "/analytics", faculty);
            JsonNode exp = a.get("riskEngineExplanation");
            assertEquals(e[1], exp.get("dataSourceProvenance").asText(), e[0]);
            double sum = exp.get("attendanceComponent").get("pointsContributed").asDouble()
                    + exp.get("marksComponent").get("pointsContributed").asDouble()
                    + exp.get("missingAssignmentsComponent").get("pointsContributed").asDouble();
            assertEquals(Math.min(sum, 100.0), exp.get("overallScore").asDouble(), 0.11, "components sum to the score for " + e[0]);
            assertEquals(7, a.get("courseAttendanceBreakdown").size(), "seven subjects for " + e[0]);

            for (JsonNode p : a.get("targetPlanner")) {
                double current = p.get("currentConvertedMarks").asDouble();
                double total = p.get("totalSubjectMaxMarks").asDouble();
                assertTrue(current <= total + 1e-6, "planner current <= subject total");
                assertTrue(p.get("remainingAssessmentsCount").asInt() > 0, "planner only for courses with pending work");
            }
        }
        // A student can open only their own profile.
        String studentA = token("demo.student.a");
        Long other = studentRepository.findByStudentNumber("STU-1004").orElseThrow().getId();
        mockMvc.perform(get("/api/students/" + other + "/analytics").header("Authorization", "Bearer " + studentA))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/students/999999/analytics").header("Authorization", "Bearer " + token("demo.faculty.1")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Follow-ups record previous/new risk from official snapshots, not from the client")
    void followUpScoresFromSnapshots() throws Exception {
        Student d = studentRepository.findByStudentNumber("STU-1004").orElseThrow();
        Faculty f1 = facultyRepository.findById(userRepository.findByUsername("demo.faculty.1").orElseThrow().getId()).orElseThrow();

        RiskAssessment baseline = RiskAssessment.builder().student(d)
                .calculatedAt(LocalDateTime.now().minusDays(20)).riskScore(88.0).riskLevel(RiskLevel.HIGH).build();
        riskAssessmentRepository.save(baseline);
        Intervention i = interventionRepository.save(Intervention.builder().student(d).assignedFaculty(f1)
                .type("Test mentoring").priority("HIGH").status(InterventionStatus.IN_PROGRESS)
                .startDate(LocalDate.now().minusDays(10)).createdAt(LocalDateTime.now().minusDays(10)).build());

        String body = "{\"facultyId\":" + f1.getId() + ",\"notes\":\"n\",\"outcome\":\"IMPROVED\",\"previousRiskScore\":1.0,\"newRiskScore\":2.0}";
        JsonNode fu = om.readTree(mockMvc.perform(post("/api/interventions/" + i.getId() + "/followups")
                        .header("Authorization", "Bearer " + token("demo.faculty.1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        double current = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(d.getId()).orElseThrow().getRiskScore();
        assertEquals(88.0, fu.get("previousRiskScore").asDouble(), "baseline = latest snapshot before the start");
        assertEquals(current, fu.get("newRiskScore").asDouble(), "new = current official snapshot");

        mockMvc.perform(post("/api/interventions/999999/followups").header("Authorization", "Bearer " + token("demo.faculty.1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Effectiveness analytics: observational, sample sizes, exclusions; students denied")
    void effectivenessAnalytics() throws Exception {
        // Isolated student so no seeded or computed snapshots interfere with the controlled timeline.
        Student c = tx.execute(st -> {
            User u = userRepository.save(User.builder().username("effect.test." + System.nanoTime()).passwordHash("x")
                    .role(Role.STUDENT).name("Effectiveness Test").authProvider(AuthProvider.LOCAL).build());
            return studentRepository.save(Student.builder().user(u).studentNumber("EFF-" + System.nanoTime()).build());
        });
        Faculty f2 = facultyRepository.findById(userRepository.findByUsername("demo.faculty.2").orElseThrow().getId()).orElseThrow();
        riskAssessmentRepository.save(RiskAssessment.builder().student(c)
                .calculatedAt(LocalDateTime.now().minusDays(30)).riskScore(50.0).riskLevel(RiskLevel.MEDIUM).build());
        Intervention evaluable = interventionRepository.save(Intervention.builder().student(c).assignedFaculty(f2)
                .type("Analytics test").priority("MEDIUM").status(InterventionStatus.COMPLETED)
                .startDate(LocalDate.now().minusDays(15)).createdAt(LocalDateTime.now().minusDays(15)).build());
        riskAssessmentRepository.save(RiskAssessment.builder().student(c)
                .calculatedAt(LocalDateTime.now().minusDays(1)).riskScore(30.0).riskLevel(RiskLevel.LOW).build());

        JsonNode r = getJson("/api/analytics/interventions/effectiveness", token("demo.faculty.2"));
        assertTrue(r.get("causalityNote").asText().contains("do not show that the intervention caused"));
        assertEquals(5, r.get("minGroupSize").asInt());

        JsonNode mine = null;
        for (JsonNode o : r.get("interventions")) if (o.get("interventionId").asLong() == evaluable.getId()) mine = o;
        assertNotNull(mine);
        assertTrue(mine.get("evaluable").asBoolean());
        assertEquals(50.0, mine.get("baselineRisk").asDouble());
        assertEquals(30.0, mine.get("outcomeRisk").asDouble());
        assertEquals(-20.0, mine.get("riskChange").asDouble());

        int total = r.get("totalInterventions").asInt();
        assertEquals(total, r.get("overall").get("interventions").asInt());
        int evaluated = r.get("overall").get("evaluable").asInt();
        long excluded = 0;
        for (var it = r.get("excluded").fields(); it.hasNext(); ) excluded += it.next().getValue().asLong();
        assertEquals(total, evaluated + excluded, "every intervention is either evaluable or explicitly excluded");
        JsonNode overall = r.get("overall");
        assertEquals(evaluated, overall.get("improved").asInt() + overall.get("unchanged").asInt() + overall.get("worsened").asInt());
        assertEquals(evaluated < 5, overall.get("smallSample").asBoolean());

        mockMvc.perform(get("/api/analytics/interventions/effectiveness").header("Authorization", "Bearer " + token("demo.student.c")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/analytics/interventions/effectiveness")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Risk history timeline is ordered and owner-scoped")
    void riskHistory() throws Exception {
        Student b = studentRepository.findByStudentNumber("STU-1002").orElseThrow();
        JsonNode h = getJson("/api/students/" + b.getId() + "/risk/history", token("demo.student.b"));
        assertTrue(h.get("snapshots").size() >= 1);
        String prev = "";
        for (JsonNode s : h.get("snapshots")) {
            assertTrue(s.get("calculatedAt").asText().compareTo(prev) >= 0);
            prev = s.get("calculatedAt").asText();
        }
        mockMvc.perform(get("/api/students/" + b.getId() + "/risk/history").header("Authorization", "Bearer " + token("demo.student.a")))
                .andExpect(status().isForbidden());
    }
}
