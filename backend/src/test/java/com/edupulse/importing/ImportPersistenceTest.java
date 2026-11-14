package com.edupulse.importing;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end: consent, confirm persists dated snapshots + exam results, idempotency, deletion, and that
 *  imported attendance actually drives the risk score. Synthetic data only. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.seeder.enabled=true"})
class ImportPersistenceTest {

    private static final String PW = "DemoEduPulse2026!";
    private static final String IMPORT = """
        {"reportDate":"2099-02-01",
         "attendance":[{"subjectCode":"SYN 301","subjectName":"Imported One","classesConducted":"40","present":"20","absent":"20","odMlTaken":"0","presentPercentage":"50","odMlPercentage":"0","attendancePercentage":"50"}],
         "ledger":[{"semester":"Semester 1","monthYear":"DEC-2099","subjectCode":"SYN 301","subjectDescription":"Imported One","credit":"4.0","grade":"B","gradePoints":"8.0","result":"PASS","attempt":"1"}],
         "cgpa":"8.0","profile":{"registerNo":"STU-1001"}}
        """;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired private DataSeeder seeder;
    @Autowired private StudentRepository studentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AttendanceSnapshotRepository snapshotRepository;
    @Autowired private ExamResultRepository examResultRepository;
    @Autowired private AcademicImportBatchRepository batchRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private RiskAssessmentService riskAssessmentService;
    @Autowired private TransactionTemplate tx;

    @BeforeEach
    void seed() {
        seeder.seedDemoData();
    }

    private String token(String username) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(PW);
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("token").asText();
    }

    @Test
    @DisplayName("Confirm needs consent, persists snapshots + results, is idempotent, and deletes cleanly")
    void fullImportLifecycle() throws Exception {
        String t = token("demo.student.a");
        Long studentId = studentRepository.findByStudentNumber("STU-1001").orElseThrow().getId();

        // No consent yet -> 409, and nothing stored.
        mockMvc.perform(post("/api/students/me/imports/confirm").header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON).content(IMPORT))
                .andExpect(status().isConflict());
        assertTrue(batchRepository.findByStudentIdOrderByUploadedAtDesc(studentId).isEmpty());

        mockMvc.perform(post("/api/students/me/imports/consent").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));

        JsonNode c1 = om.readTree(mockMvc.perform(post("/api/students/me/imports/confirm").header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON).content(IMPORT))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertFalse(c1.get("duplicate").asBoolean());
        assertEquals(1, c1.get("acceptedAttendance").asInt());
        assertEquals(1, c1.get("acceptedLedger").asInt());
        assertEquals(8.0, c1.get("cgpa").asDouble());

        Long courseId = courseRepository.findByCode("SYN 301").orElseThrow().getId();
        AttendanceSnapshot snap = snapshotRepository.findByStudentIdAndCourseIdAndAsOfDate(studentId, courseId, LocalDate.parse("2099-02-01")).orElseThrow();
        assertEquals(20, snap.getPresent());
        assertEquals(40, snap.getConducted());
        assertEquals(1, examResultRepository.findByStudentId(studentId).stream().filter(e -> e.getGrade().equals("B")).count());

        // Same upload again -> idempotent (duplicate, still one batch).
        JsonNode c2 = om.readTree(mockMvc.perform(post("/api/students/me/imports/confirm").header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON).content(IMPORT))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertTrue(c2.get("duplicate").asBoolean());
        assertEquals(1, batchRepository.findByStudentIdOrderByUploadedAtDesc(studentId).size());

        // Delete the batch -> snapshot and result for this batch are gone.
        Long batchId = c1.get("batchId").asLong();
        mockMvc.perform(delete("/api/students/me/imports/" + batchId).header("Authorization", "Bearer " + t))
                .andExpect(status().isNoContent());
        assertTrue(snapshotRepository.findByStudentIdAndCourseIdAndAsOfDate(studentId, courseId, LocalDate.parse("2099-02-01")).isEmpty());
        assertTrue(batchRepository.findByStudentIdOrderByUploadedAtDesc(studentId).isEmpty());

        // A student can only touch their own imports.
        mockMvc.perform(get("/api/students/me/imports").header("Authorization", "Bearer " + token("demo.student.b")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Imported attendance snapshot drives the official risk score when there are no dated rows")
    void importedSnapshotDrivesRisk() {
        Student s = tx.execute(st -> {
            User u = userRepository.save(User.builder().username("snap.rt." + System.nanoTime()).passwordHash("x")
                    .role(Role.STUDENT).name("Snapshot RT").authProvider(AuthProvider.LOCAL).build());
            return studentRepository.save(Student.builder().user(u).studentNumber("SNAP-" + System.nanoTime()).build());
        });
        Course c = courseRepository.save(Course.builder().code("SYN RT1").name("Snapshot Course").credits(4).build());
        snapshotRepository.save(AttendanceSnapshot.builder().student(s).course(c)
                .asOfDate(LocalDate.now()).conducted(40).present(20).absent(20).odMl(0).build());

        // 50% attendance -> (75 - 50) * 1.5 = 37.5 -> MEDIUM, from the imported snapshot alone.
        RiskAssessment r = riskAssessmentService.calculateRiskForStudent(s.getId());
        assertEquals(37.5, r.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, r.getRiskLevel());
    }
}
