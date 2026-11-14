package com.edupulse.controller;

import com.edupulse.dto.AcademicImportRequest;
import com.edupulse.service.importing.AcademicDataSourceType;
import com.edupulse.service.importing.AcademicImportRecords.*;
import com.edupulse.service.importing.AcademicImportValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Student-controlled academic import. This controller only VALIDATES and PREVIEWS an upload — it
 * persists nothing. Storing imported data requires the staged V3 migration (see
 * docs/account-linking-architecture.md); until that is approved, the student can see exactly what
 * would be imported and what is rejected, with no schema change and no production risk.
 */
@RestController
@RequestMapping("/api/students/me/imports")
public class StudentImportController {

    private final AcademicImportValidator validator = new AcademicImportValidator();

    /** Response is purely informational; nothing is stored. {@code live} is always false for an upload. */
    public record ImportPreviewResponse(
            String sourceLabel,
            boolean live,
            int acceptedAttendance,
            int acceptedLedger,
            Double cgpa,
            long rejected,
            long warnings,
            List<Issue> issues,
            List<AttendanceSummary> attendance,
            List<LedgerEntry> ledger) {
    }

    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImportPreviewResponse> preview(@RequestBody AcademicImportRequest req) {
        // The server never trusts a client-supplied source type; an upload is always a manual import.
        AcademicDataSourceType source = AcademicDataSourceType.STUDENT_MANUAL_IMPORT;

        ValidationReport report = validator.validate(
                toAttendance(req.getAttendance()),
                List.of(),            // internal components not in the first export slice
                toLedger(req.getLedger()),
                List.of(),            // timetable not in the first export slice
                req.getCgpa(),
                req.getProfile(),
                source);

        long warnings = report.issues().stream().filter(i -> Issue.WARNING.equals(i.severity())).count();

        return ResponseEntity.ok(new ImportPreviewResponse(
                source.label(),
                source.isLive(),
                report.acceptedAttendance().size(),
                report.acceptedLedger().size(),
                report.cgpa(),
                report.rejectedCount(),
                warnings,
                report.issues(),
                report.acceptedAttendance(),
                report.acceptedLedger()));
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
