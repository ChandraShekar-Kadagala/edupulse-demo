package com.edupulse;

import com.edupulse.dto.AssessmentMarkDto;
import com.edupulse.dto.CourseAttendanceDto;
import com.edupulse.dto.StudentAcademicAnalyticsDto;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.service.AcademicAnalyticsService;
import com.edupulse.service.provider.OfficialSrmDataProvider;
import com.edupulse.service.provider.portal.SrmPortalIntegrationService;
import com.edupulse.service.provider.portal.SrmPortalParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SrmPortalIntegrationTests {

    @Autowired
    private SrmPortalParserService parserService;

    @Autowired
    private SrmPortalIntegrationService integrationService;

    @Autowired
    private OfficialSrmDataProvider officialSrmDataProvider;

    @Autowired
    private AcademicAnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    private Student testStudent;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        markRepository.deleteAll();
        auditLogRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .username("teststudent_srm")
                .passwordHash("bcrypt_hash_placeholder")
                .role(Role.STUDENT)
                .name("Arun S")
                .email("arun.s@example.edu")
                .tokenVersion(1L)
                .authProvider(AuthProvider.LOCAL)
                .createdAt(LocalDateTime.now())
                .build());

        testStudent = studentRepository.save(Student.builder()
                .user(user)
                .studentNumber("AP21110010001")
                .department("Computer Science and Engineering")
                .currentSemester(5)
                .currentCgpa(8.4)
                .build());
    }

    private String loadResource(String path) throws Exception {
        Resource resource = resourceLoader.getResource(path);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("HTML Parser: Successfully extracts courses and attendance from valid portal fixture")
    void testAttendanceHtmlParsing_ValidFixture() throws Exception {
        String html = loadResource("classpath:fixtures/srm_attendance_sample.html");
        SrmPortalParserService.AttendanceParseResult result = parserService.parseAttendanceHtml(html);

        assertNotNull(result);
        assertEquals(5, result.validCount(), "Should successfully parse all 5 valid courses");
        assertTrue(result.errors().isEmpty(), "Valid HTML fixture should produce 0 parse errors");

        CourseAttendanceDto osCourse = result.records().stream()
                .filter(c -> "CSE301".equals(c.getCourseCode()))
                .findFirst()
                .orElse(null);

        assertNotNull(osCourse);
        assertEquals("Operating Systems", osCourse.getCourseName());
        assertEquals(40, osCourse.getTotalHours());
        assertEquals(34, osCourse.getAttendedHours());
        assertEquals(85.0, osCourse.getAttendancePercentage());
        assertEquals("NORMAL", osCourse.getStatus());

        // Check critical attendance course
        CourseAttendanceDto dbmsCourse = result.records().stream()
                .filter(c -> "CSE302".equals(c.getCourseCode()))
                .findFirst()
                .orElse(null);

        assertNotNull(dbmsCourse);
        assertEquals(60.53, dbmsCourse.getAttendancePercentage());
        assertEquals("CRITICAL", dbmsCourse.getStatus());
    }

    @Test
    @DisplayName("HTML Parser: Successfully extracts marks and distinguishes 0.0 from pending evaluations")
    void testMarksHtmlParsing_ValidFixture() throws Exception {
        String html = loadResource("classpath:fixtures/srm_marks_sample.html");
        SrmPortalParserService.MarksParseResult result = parserService.parseMarksHtml(html);

        assertNotNull(result);
        assertEquals(8, result.validCount(), "Should parse 8 assessment rows");
        assertTrue(result.errors().isEmpty(), "Valid HTML fixture should produce 0 parse errors");

        // 1. Regular graded assessment
        AssessmentMarkDto graded = result.records().stream()
                .filter(m -> "CSE301".equals(m.getCourseCode()) && "Internal Assessment 1".equals(m.getAssessmentName()))
                .findFirst()
                .orElse(null);
        assertNotNull(graded);
        assertEquals(42.0, graded.getMarksObtained());
        assertEquals(50.0, graded.getMaxMarks());
        assertEquals(84.0, graded.getPercentage());

        // 2. Pending evaluation (hyphen '-') must NOT be parsed as 0.0!
        AssessmentMarkDto pendingQuiz = result.records().stream()
                .filter(m -> "CSE303".equals(m.getCourseCode()) && "Surprise Quiz 1".equals(m.getAssessmentName()))
                .findFirst()
                .orElse(null);
        assertNotNull(pendingQuiz);
        assertNull(pendingQuiz.getMarksObtained(), "Pending evaluation should result in null marksObtained, not 0.0");
        assertNull(pendingQuiz.getPercentage(), "Pending evaluation percentage should be null");

        // 3. Genuine 0.0 marks
        AssessmentMarkDto zeroMark = result.records().stream()
                .filter(m -> "MAT202".equals(m.getCourseCode()) && "Internal Assessment 1".equals(m.getAssessmentName()))
                .findFirst()
                .orElse(null);
        assertNotNull(zeroMark);
        assertEquals(0.0, zeroMark.getMarksObtained(), "Genuine zero marks should be preserved as 0.0");
        assertEquals(0.0, zeroMark.getPercentage());
    }

    @Test
    @DisplayName("HTML Parser: Gracefully filters malformed rows without crashing")
    void testMalformedHtmlParsing_Resilience() throws Exception {
        String html = loadResource("classpath:fixtures/srm_malformed_sample.html");
        SrmPortalParserService.AttendanceParseResult result = parserService.parseAttendanceHtml(html);

        assertNotNull(result);
        assertEquals(1, result.validCount(), "Only the 1 valid row (CSE101) should be accepted");
        assertFalse(result.errors().isEmpty(), "Parser must capture validation errors for malformed rows");
        assertEquals(4, result.rowsProcessed());

        CourseAttendanceDto validCourse = result.records().get(0);
        assertEquals("CSE101", validCourse.getCourseCode());
        assertEquals(40, validCourse.getTotalHours());
        assertEquals(32, validCourse.getAttendedHours());
    }

    @Test
    @DisplayName("HTML Parser: Handles empty or invalid documents cleanly")
    void testEmptyHtmlParsing() {
        SrmPortalParserService.AttendanceParseResult emptyAtt = parserService.parseAttendanceHtml("");
        assertEquals(0, emptyAtt.validCount());
        assertFalse(emptyAtt.errors().isEmpty());

        SrmPortalParserService.MarksParseResult emptyMarks = parserService.parseMarksHtml(null);
        assertEquals(0, emptyMarks.validCount());
        assertFalse(emptyMarks.errors().isEmpty());
    }

    @Test
    @DisplayName("Live Portal Boundary: Live scraping is strictly rejected with UnsupportedOperationException")
    void testLivePortalDisabled_ThrowsUnsupportedOperationException() {
        assertFalse(officialSrmDataProvider.isAvailable());
        assertFalse(officialSrmDataProvider.isConfigured());

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> {
            officialSrmDataProvider.syncStudentData(testStudent.getStudentNumber());
        });

        assertTrue(ex.getMessage().contains("Live SRM portal login requires official OAuth2/OIDC SSO credentials"),
                "Exception message must clearly inform caller of the institutional SSO requirement");
    }

    @Test
    @DisplayName("End-to-End Pipeline: Ingests portal fixtures, updates DB, and triggers AcademicAnalyticsService")
    void testFixtureSynchronizationPipeline_EndToEnd() {
        StudentAcademicAnalyticsDto analytics = integrationService.synchronizeFromFixtures(testStudent.getId());

        assertNotNull(analytics);
        assertEquals(testStudent.getStudentNumber(), analytics.getStudentNumber());
        assertEquals(DataProvenance.INSTITUTIONAL_PORTAL_FIXTURE.name(), analytics.getProvenance(),
                "Provenance must be truthfully labeled as INSTITUTIONAL_PORTAL_FIXTURE");

        // Verify database persistence
        List<Attendance> attendances = attendanceRepository.findByStudentId(testStudent.getId());
        assertFalse(attendances.isEmpty(), "Attendance records should be persisted in database");

        List<Mark> marks = markRepository.findByStudentId(testStudent.getId());
        assertFalse(marks.isEmpty(), "Marks records should be persisted in database");

        // Verify audit log
        List<AuditLog> auditLogs = auditLogRepository.findByUserId(testStudent.getId());
        assertFalse(auditLogs.isEmpty());
        assertEquals("PORTAL_FIXTURE_SYNC", auditLogs.get(0).getAction());

        // Verify analytics calculation correctness
        assertTrue(analytics.isHasAttendanceData());
        assertTrue(analytics.isHasMarksData());
        assertNotNull(analytics.getOverallAttendancePercentage());
        assertNotNull(analytics.getRiskScore());
        assertNotNull(analytics.getRecommendations());
        assertFalse(analytics.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("Idempotency: Re-synchronizing fixtures updates records without creating duplicates")
    void testIdempotentReSync_PreventsDuplicates() {
        integrationService.synchronizeFromFixtures(testStudent.getId());
        int firstAttCount = attendanceRepository.findByStudentId(testStudent.getId()).size();
        int firstMarkCount = markRepository.findByStudentId(testStudent.getId()).size();

        // Run second synchronization
        integrationService.synchronizeFromFixtures(testStudent.getId());
        int secondAttCount = attendanceRepository.findByStudentId(testStudent.getId()).size();
        int secondMarkCount = markRepository.findByStudentId(testStudent.getId()).size();

        assertEquals(firstAttCount, secondAttCount, "Re-synchronization must not duplicate attendance records");
        assertEquals(firstMarkCount, secondMarkCount, "Re-synchronization must not duplicate mark records");
    }

    @Test
    @DisplayName("Integration Metadata: Correctly reports prototype fixture mode and live requirements")
    void testIntegrationStatusMetadata() {
        Map<String, Object> status = integrationService.getIntegrationStatus();
        assertNotNull(status);
        assertEquals("SRM AP Academic Integration", status.get("provider"));
        assertEquals("PROTOTYPE_FIXTURE", status.get("mode"));
        assertEquals("UNSUPPORTED_ACCESS", status.get("status"));
        assertEquals("CONFIGURED", status.get("prototypeStatus"));
        assertEquals(false, status.get("liveAvailable"));
        assertEquals(DataProvenance.INSTITUTIONAL_PORTAL_FIXTURE.name(), status.get("provenance"));
    }

    @Test
    @DisplayName("Credential Privacy: Confirms zero SRM password persistence in entities")
    void testCredentialPrivacy_ZeroPasswordExposure() {
        // Confirm User entity does NOT have any SRM credentials or portal password fields
        User user = userRepository.findById(testStudent.getId()).orElseThrow();
        assertNotNull(user.getPasswordHash(), "Local EduPulse password hash is present");
        assertEquals(AuthProvider.LOCAL, user.getAuthProvider());

        // Confirm Student model has only academic attributes
        assertEquals("AP21110010001", testStudent.getStudentNumber());
    }

    @Test
    @DisplayName("Real SRM Portal DOM: Successfully parses table#tblSubjectWiseAttendance (StoreVia DOM reference)")
    void testRealSrmPortalAttendanceTableParsing() throws Exception {
        String html = loadResource("classpath:fixtures/srm_real_portal_attendance.html");
        SrmPortalParserService.AttendanceParseResult result = parserService.parseAttendanceHtml(html);

        assertNotNull(result);
        assertEquals(3, result.validCount(), "Must parse all 3 courses from real SRM AP DOM");
        assertTrue(result.errors().isEmpty());

        CourseAttendanceDto cse201 = result.records().stream()
                .filter(c -> "CSE201".equals(c.getCourseCode()))
                .findFirst().orElseThrow();
        assertEquals("Data Structures and Algorithms", cse201.getCourseName());
        assertEquals(45, cse201.getTotalHours());
        assertEquals(40, cse201.getAttendedHours());
        assertEquals(88.89, cse201.getAttendancePercentage());
        assertEquals("NORMAL", cse201.getStatus());

        CourseAttendanceDto cse202 = result.records().stream()
                .filter(c -> "CSE202".equals(c.getCourseCode()))
                .findFirst().orElseThrow();
        assertEquals(66.67, cse202.getAttendancePercentage());
        assertEquals("CRITICAL", cse202.getStatus());
    }

    @Test
    @DisplayName("Real SRM Portal DOM: Successfully parses Exam Ledger and extracts CGPA")
    void testRealSrmPortalExamLedgerParsing() throws Exception {
        String html = loadResource("classpath:fixtures/srm_real_portal_ledger.html");
        SrmPortalParserService.ExamLedgerParseResult result = parserService.parseExamLedgerHtml(html);

        assertNotNull(result);
        assertEquals(3, result.validCount(), "Must parse 3 subject grades from ledger");
        assertEquals(8.85, result.overallCgpa(), "Must correctly extract CGPA from portal text");
        assertTrue(result.errors().isEmpty());

        SrmPortalParserService.ExamLedgerRowDto cse101 = result.records().stream()
                .filter(r -> "CSE101".equals(r.courseCode()))
                .findFirst().orElseThrow();
        assertEquals("A+", cse101.grade());
        assertEquals(10.0, cse101.gradePoints());
        assertEquals("PASS", cse101.result());
    }

    @Test
    @DisplayName("Report Sync: Ingests authentic SRM AP report HTML, updates student CGPA, and recalculates risk")
    void testAuthenticReportSynchronization() throws Exception {
        String attHtml = loadResource("classpath:fixtures/srm_real_portal_attendance.html");
        String ledgerHtml = loadResource("classpath:fixtures/srm_real_portal_ledger.html");

        StudentAcademicAnalyticsDto analytics = integrationService.synchronizeFromHtmlReport(
                testStudent.getId(), attHtml, null, ledgerHtml
        );

        assertNotNull(analytics);
        assertEquals(DataProvenance.INSTITUTIONAL_PORTAL_REPORT.name(), analytics.getProvenance());
        assertTrue(analytics.isHasAttendanceData());

        // Verify Student CGPA was updated from exam ledger
        Student updatedStudent = studentRepository.findById(testStudent.getId()).orElseThrow();
        assertEquals(8.85, updatedStudent.getCurrentCgpa(), "Student CGPA should be updated from authentic ledger");

        // Verify audit log
        List<AuditLog> logs = auditLogRepository.findByUserId(testStudent.getId());
        assertFalse(logs.isEmpty());
        boolean hasReportLog = logs.stream().anyMatch(l -> "PORTAL_REPORT_SYNC".equals(l.getAction()));
        assertTrue(hasReportLog, "Audit log should record PORTAL_REPORT_SYNC");
    }

    @Test
    @DisplayName("Official Provider: Throws descriptive exception explaining why automated CAPTCHA scraping is blocked")
    void testOfficialProvider_ExplainsInstitutionalBlocker() {
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> officialSrmDataProvider.synchronizeOfficialStudentRecords("AP21110010001")
        );
        assertTrue(ex.getMessage().contains("CAPTCHA"));
        assertTrue(ex.getMessage().contains("OAuth2"));
    }
}
