package com.edupulse.service.importing;

import com.edupulse.dto.AcademicImportRequest;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.service.RiskAssessmentService;
import com.edupulse.service.importing.AcademicImportRecords.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Validates and (on confirm) persists a student-controlled academic import. Attendance is stored as a
 * dated {@link AttendanceSnapshot} (never fabricated per-day rows); ledger rows as {@link ExamResult}.
 * Confirm requires active consent and is idempotent per upload (same content hash → same batch).
 */
@Service
@RequiredArgsConstructor
public class StudentImportService {

    private final AcademicImportValidator validator = new AcademicImportValidator();

    private final ConsentService consentService;
    private final AcademicImportBatchRepository batchRepository;
    private final AttendanceSnapshotRepository snapshotRepository;
    private final ExamResultRepository examResultRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final RiskAssessmentService riskAssessmentService;

    public record ConfirmResult(Long batchId, boolean duplicate, int acceptedAttendance, int acceptedLedger,
                                Double cgpa, Double riskScore, String riskLevel) {
    }

    /** Validates an upload without persisting anything. Always treated as a manual import. */
    public ValidationReport validate(AcademicImportRequest req) {
        return validator.validate(
                toAttendance(req.getAttendance()),
                List.of(),
                toLedger(req.getLedger()),
                List.of(),
                req.getCgpa(),
                req.getProfile(),
                AcademicDataSourceType.STUDENT_MANUAL_IMPORT);
    }

    @Transactional
    public ConfirmResult confirm(Long studentId, AcademicImportRequest req) {
        if (!consentService.hasActiveConsent(studentId)) {
            throw new IllegalStateException("Import consent is required before saving academic data.");
        }
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new com.edupulse.exception.ResourceNotFoundException("Student", studentId));

        ValidationReport report = validate(req);
        String sha = sha256(report);

        Optional<AcademicImportBatch> existing = batchRepository.findByStudentIdAndContentSha256(studentId, sha);
        if (existing.isPresent() && "CONFIRMED".equals(existing.get().getStatus())) {
            RiskAssessment latest = riskAssessmentService.ensureCurrentSnapshot(studentId);
            return new ConfirmResult(existing.get().getId(), true, report.acceptedAttendance().size(),
                    report.acceptedLedger().size(), report.cgpa(), latest.getRiskScore(),
                    latest.getRiskLevel() != null ? latest.getRiskLevel().name() : null);
        }

        LocalDate asOf = parseDate(req.getReportDate());
        AcademicImportBatch batch = batchRepository.save(AcademicImportBatch.builder()
                .student(student)
                .sourceType(AcademicDataSourceType.STUDENT_MANUAL_IMPORT.name())
                .reportType(reportType(report))
                .reportDate(asOf)
                .contentSha256(sha)
                .status("CONFIRMED")
                .acceptedRows(report.acceptedAttendance().size() + report.acceptedLedger().size())
                .rejectedRows((int) report.rejectedCount())
                .build());

        for (AttendanceSummary a : report.acceptedAttendance()) {
            Course course = findOrCreateCourse(a.courseCode(), a.courseName());
            AttendanceSnapshot snap = snapshotRepository
                    .findByStudentIdAndCourseIdAndAsOfDate(studentId, course.getId(), asOf)
                    .orElseGet(() -> AttendanceSnapshot.builder().student(student).course(course).asOfDate(asOf).build());
            snap.setBatchId(batch.getId());
            snap.setConducted(a.conducted());
            snap.setPresent(a.present());
            snap.setAbsent(a.absent());
            snap.setOdMl(a.odMl());
            snapshotRepository.save(snap);
        }

        for (LedgerEntry l : report.acceptedLedger()) {
            Course course = findOrCreateCourse(l.courseCode(), l.courseTitle());
            ExamResult er = examResultRepository
                    .findByStudentIdAndCourseIdAndSemesterAndAttempt(studentId, course.getId(), l.semester(), l.attempt())
                    .orElseGet(() -> ExamResult.builder().student(student).course(course)
                            .semester(l.semester()).attempt(l.attempt()).build());
            er.setBatchId(batch.getId());
            er.setMonthYear(l.monthYear());
            er.setCredit(l.credit());
            er.setGrade(l.grade());
            er.setGradePoints(l.gradePoints());
            er.setResult(l.result());
            examResultRepository.save(er);
        }

        if (report.cgpa() != null) {
            student.setCurrentCgpa(report.cgpa());
            studentRepository.save(student);
        }

        RiskAssessment risk = riskAssessmentService.calculateRiskForStudent(studentId);
        return new ConfirmResult(batch.getId(), false, report.acceptedAttendance().size(),
                report.acceptedLedger().size(), report.cgpa(), risk.getRiskScore(),
                risk.getRiskLevel() != null ? risk.getRiskLevel().name() : null);
    }

    public List<AcademicImportBatch> listBatches(Long studentId) {
        return batchRepository.findByStudentIdOrderByUploadedAtDesc(studentId);
    }

    private Course findOrCreateCourse(String code, String name) {
        return courseRepository.findByCode(code)
                .orElseGet(() -> courseRepository.save(Course.builder().code(code).name(name != null ? name : code).build()));
    }

    private static String reportType(ValidationReport r) {
        boolean att = !r.acceptedAttendance().isEmpty();
        boolean led = !r.acceptedLedger().isEmpty();
        return att && led ? "MIXED" : led ? "EXAM_LEDGER" : "ATTENDANCE";
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    /** Deterministic hash of the accepted content, for duplicate-upload detection. */
    static String sha256(ValidationReport r) {
        StringBuilder sb = new StringBuilder();
        r.acceptedAttendance().stream().sorted(Comparator.comparing(AttendanceSummary::courseCode))
                .forEach(a -> sb.append(a.courseCode()).append(':').append(a.conducted()).append('/').append(a.present()).append('|'));
        sb.append("##");
        r.acceptedLedger().stream()
                .sorted(Comparator.comparing(LedgerEntry::courseCode).thenComparing(LedgerEntry::semester).thenComparingInt(LedgerEntry::attempt))
                .forEach(l -> sb.append(l.courseCode()).append(':').append(l.semester()).append(':').append(l.grade()).append('|'));
        sb.append("##cgpa=").append(r.cgpa());
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : d) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static List<RawAttendanceRow> toAttendance(List<AcademicImportRequest.AttendanceRow> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream().map(a -> new RawAttendanceRow(
                a.getSubjectCode(), a.getSubjectName(), a.getClassesConducted(), a.getPresent(), a.getAbsent(),
                a.getOdMlTaken(), a.getPresentPercentage(), a.getOdMlPercentage(), a.getAttendancePercentage())).toList();
    }

    private static List<RawLedgerRow> toLedger(List<AcademicImportRequest.LedgerRow> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream().map(l -> new RawLedgerRow(
                l.getSemester(), l.getMonthYear(), l.getSubjectCode(), l.getSubjectDescription(), l.getCredit(),
                l.getGrade(), l.getGradePoints(), l.getResult(), l.getAttempt())).toList();
    }
}
