package com.edupulse.service.provider.portal;

import com.edupulse.dto.AssessmentMarkDto;
import com.edupulse.dto.CourseAttendanceDto;
import com.edupulse.dto.StudentAcademicAnalyticsDto;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.service.AcademicAnalyticsService;
import com.edupulse.service.RiskAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SrmPortalIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(SrmPortalIntegrationService.class);

    @Autowired
    private SrmPortalParserService parserService;

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
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AcademicAnalyticsService analyticsService;

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Autowired
    private ResourceLoader resourceLoader;

    private LocalDateTime lastSyncTime = null;
    private String lastSyncStatus = "IDLE";
    private String lastSyncMessage = "No synchronization performed yet";
    private int lastSyncedAttendanceRows = 0;
    private int lastSyncedMarksRows = 0;

    public Map<String, Object> getIntegrationStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider", "SRM AP Academic Integration");
        status.put("mode", "PROTOTYPE_FIXTURE");
        status.put("status", "UNSUPPORTED_ACCESS");
        status.put("prototypeStatus", "CONFIGURED");
        status.put("liveAvailable", false);
        status.put("liveRequirement", "Official Institutional OAuth2/OIDC SSO Authorization");
        status.put("liveBlocker", "No public API exists for SRM AP. The student portal gates access with image CAPTCHAs. Automated CAPTCHA bypass is prohibited for student privacy & policy compliance.");
        status.put("lastSync", lastSyncTime != null ? lastSyncTime.toString() : "N/A");
        status.put("lastSyncStatus", lastSyncStatus);
        status.put("lastSyncMessage", lastSyncMessage);
        status.put("provenance", DataProvenance.INSTITUTIONAL_PORTAL_FIXTURE.name());
        status.put("availableDataStreams", List.of(
                "Course Attendance (Conducted, Attended, Absent, OD/ML, Percentage)",
                "Internal Assessment Marks (Granular Tests, Quizzes, Totals)",
                "Semester Examination Ledger (Grades, Grade Points, Credits, Pass/Fail)",
                "Cumulative Grade Point Average (CGPA)",
                "Weekly Timetable & Classroom Allocation",
                "Student Profile & Academic Registration"
        ));
        status.put("supportedModes", List.of(
                "OFFICIAL_OAUTH2_READY (Pending University Agreement)",
                "AUTHENTIC_REPORT_SYNC (Verified Student HTML Ingestion)",
                "PORTAL_FIXTURE_PIPELINE (Verified DOM Test Suite)",
                "DEMO_SANDBOX (Synthetic Simulation)"
        ));
        status.put("message", "No official public API exists for SRM AP. Account linking is blocked. Verified HTML prototype pipeline is active.");
        return status;
    }

    @Transactional
    public StudentAcademicAnalyticsDto synchronizeFromHtmlReport(Long studentUserId, String attendanceHtml, String marksHtml, String ledgerHtml) {
        Student student = studentRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with user ID: " + studentUserId));

        log.info("Starting authentic SRM AP portal report synchronization for student: {} ({})", student.getStudentNumber(), studentUserId);

        int attendanceCount = 0;
        int marksCount = 0;

        // 1. Process Exam Ledger if provided
        if (ledgerHtml != null && !ledgerHtml.trim().isEmpty()) {
            SrmPortalParserService.ExamLedgerParseResult ledgerResult = parserService.parseExamLedgerHtml(ledgerHtml);
            if (ledgerResult.overallCgpa() != null && ledgerResult.overallCgpa() > 0.0) {
                student.setCurrentCgpa(ledgerResult.overallCgpa());
                studentRepository.save(student);
            }
        }

        // 2. Process Attendance if provided
        if (attendanceHtml != null && !attendanceHtml.trim().isEmpty()) {
            SrmPortalParserService.AttendanceParseResult attResult = parserService.parseAttendanceHtml(attendanceHtml);
            for (CourseAttendanceDto dto : attResult.records()) {
                Course course = findOrCreateCourse(dto.getCourseCode(), dto.getCourseName());
                syncAttendanceRecords(student, course, (int) dto.getTotalHours(), (int) dto.getAttendedHours());
            }
            attendanceCount = attResult.validCount();
        }

        // 3. Process Marks if provided
        if (marksHtml != null && !marksHtml.trim().isEmpty()) {
            SrmPortalParserService.MarksParseResult marksResult = parserService.parseMarksHtml(marksHtml);
            for (AssessmentMarkDto dto : marksResult.records()) {
                Course course = findOrCreateCourse(dto.getCourseCode(), dto.getCourseName());
                Assessment assessment = findOrCreateAssessment(course, dto.getAssessmentName(), dto.getAssessmentType(), dto.getMaxMarks(), dto.getWeightage());
                syncMarkRecord(student, assessment, dto.getMarksObtained());
            }
            marksCount = marksResult.validCount();
        }

        // 4. Calculate Risk Score using EduPulse's real risk engine
        riskAssessmentService.calculateRiskForStudent(studentUserId);

        // 5. Audit Log
        AuditLog auditLog = AuditLog.builder()
                .action("PORTAL_REPORT_SYNC")
                .userId(studentUserId)
                .details(String.format("Synchronized authentic SRM AP portal report: %d attendance courses, %d assessment marks. Provenance: %s",
                        attendanceCount, marksCount, DataProvenance.INSTITUTIONAL_PORTAL_REPORT.name()))
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        this.lastSyncTime = LocalDateTime.now();
        this.lastSyncStatus = "SUCCESS";
        this.lastSyncedAttendanceRows = attendanceCount;
        this.lastSyncedMarksRows = marksCount;
        this.lastSyncMessage = String.format("Successfully synchronized authentic report: %d courses and %d assessments.",
                attendanceCount, marksCount);

        return analyticsService.calculateStudentAnalytics(student.getStudentNumber(), DataProvenance.INSTITUTIONAL_PORTAL_REPORT);
    }

    @Transactional
    public StudentAcademicAnalyticsDto synchronizeFromFixtures(Long studentUserId) {
        Student student = studentRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with user ID: " + studentUserId));

        log.info("Starting SRM portal fixture synchronization for student: {} ({})", student.getStudentNumber(), studentUserId);

        try {
            String attendanceHtml = loadFixtureContent("classpath:fixtures/srm_attendance_sample.html");
            String marksHtml = loadFixtureContent("classpath:fixtures/srm_marks_sample.html");

            SrmPortalParserService.AttendanceParseResult attResult = parserService.parseAttendanceHtml(attendanceHtml);
            SrmPortalParserService.MarksParseResult marksResult = parserService.parseMarksHtml(marksHtml);

            // Persist Courses & Attendance
            for (CourseAttendanceDto dto : attResult.records()) {
                Course course = findOrCreateCourse(dto.getCourseCode(), dto.getCourseName());
                // Represent conducted attendance as daily records
                syncAttendanceRecords(student, course, (int) dto.getTotalHours(), (int) dto.getAttendedHours());
            }

            // Persist Assessments & Marks
            for (AssessmentMarkDto dto : marksResult.records()) {
                Course course = findOrCreateCourse(dto.getCourseCode(), dto.getCourseName());
                Assessment assessment = findOrCreateAssessment(course, dto.getAssessmentName(), dto.getAssessmentType(), dto.getMaxMarks(), dto.getWeightage());
                syncMarkRecord(student, assessment, dto.getMarksObtained());
            }

            // Write Audit Log
            AuditLog auditLog = AuditLog.builder()
                    .action("PORTAL_FIXTURE_SYNC")
                    .userId(studentUserId)
                    .details(String.format("Synced %d attendance courses, %d assessment marks with provenance %s",
                            attResult.validCount(), marksResult.validCount(), DataProvenance.INSTITUTIONAL_PORTAL_FIXTURE.name()))
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);

            this.lastSyncTime = LocalDateTime.now();
            this.lastSyncStatus = "SUCCESS";
            this.lastSyncedAttendanceRows = attResult.validCount();
            this.lastSyncedMarksRows = marksResult.validCount();
            this.lastSyncMessage = String.format("Successfully parsed and synced %d courses and %d assessments from verified portal fixtures.",
                    attResult.validCount(), marksResult.validCount());

            // Compute and return real analytics via AcademicAnalyticsService
            return analyticsService.calculateStudentAnalytics(student.getStudentNumber(), DataProvenance.INSTITUTIONAL_PORTAL_FIXTURE);

        } catch (Exception e) {
            log.error("Failed to synchronize SRM portal fixtures for student {}", student.getStudentNumber(), e);
            this.lastSyncStatus = "FAILED";
            this.lastSyncMessage = "Synchronization failed: " + e.getMessage();
            throw new RuntimeException("Portal fixture synchronization failed: " + e.getMessage(), e);
        }
    }

    private Course findOrCreateCourse(String code, String name) {
        return courseRepository.findByCode(code)
                .orElseGet(() -> courseRepository.save(Course.builder()
                        .code(code)
                        .name(name != null ? name : code)
                        .credits(4)
                        .department("Computer Science and Engineering")
                        .build()));
    }

    private void syncAttendanceRecords(Student student, Course course, int totalHours, int attendedHours) {
        // Delete previous attendance for idempotency on re-sync
        List<Attendance> existing = attendanceRepository.findByStudentIdAndCourseId(student.getId(), course.getId());
        if (!existing.isEmpty()) {
            attendanceRepository.deleteAll(existing);
        }

        // Create representative daily attendance records over the semester
        LocalDate baseDate = LocalDate.now().minusDays(totalHours);
        for (int i = 0; i < totalHours; i++) {
            boolean wasPresent = i < attendedHours;
            Attendance att = Attendance.builder()
                    .student(student)
                    .course(course)
                    .date(baseDate.plusDays(i))
                    .status(wasPresent ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT)
                    .build();
            attendanceRepository.save(att);
        }
    }

    private Assessment findOrCreateAssessment(Course course, String name, String typeStr, Double maxMarks, Double weightage) {
        AssessmentType type = AssessmentType.INTERNAL;
        if (typeStr != null) {
            try {
                type = AssessmentType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        final AssessmentType finalType = type;
        return assessmentRepository.findByCourseId(course.getId()).stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> assessmentRepository.save(Assessment.builder()
                        .course(course)
                        .name(name)
                        .type(finalType)
                        .maxMarks(maxMarks != null ? maxMarks : 100.0)
                        .weightage(weightage != null ? weightage : 15.0)
                        .build()));
    }

    private void syncMarkRecord(Student student, Assessment assessment, Double marksObtained) {
        if (marksObtained == null) {
            // Missing / Pending assessment: do not fabricate 0.0!
            return;
        }

        Mark existing = markRepository.findByStudentId(student.getId()).stream()
                .filter(m -> m.getAssessment().getId().equals(assessment.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setMarksObtained(marksObtained);
            markRepository.save(existing);
        } else {
            markRepository.save(Mark.builder()
                    .student(student)
                    .assessment(assessment)
                    .marksObtained(marksObtained)
                    .build());
        }
    }

    private String loadFixtureContent(String resourcePath) throws Exception {
        Resource resource = resourceLoader.getResource(resourcePath);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
