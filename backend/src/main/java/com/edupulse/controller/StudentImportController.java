package com.edupulse.controller;

import com.edupulse.dto.AcademicImportRequest;
import com.edupulse.model.AcademicImportBatch;
import com.edupulse.security.UserDetailsImpl;
import com.edupulse.service.importing.*;
import com.edupulse.service.importing.AcademicImportRecords.*;
import com.edupulse.service.importing.StudentImportService.ConfirmResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Student-controlled academic import. A student logs into the SRM portal themselves and brings their own
 * report; EduPulse never handles the portal password, session or CAPTCHA. Preview validates only;
 * confirm persists (consent required) as dated snapshots and exam results, then recomputes risk.
 * Every endpoint acts on the authenticated caller's own student record.
 */
@RestController
@RequestMapping("/api/students/me/imports")
@RequiredArgsConstructor
public class StudentImportController {

    private final StudentImportService importService;
    private final ConsentService consentService;
    private final ImportedDataDeletionService deletionService;

    public record ImportPreviewResponse(
            String sourceLabel, boolean live, int acceptedAttendance, int acceptedLedger,
            Double cgpa, long rejected, long warnings, List<Issue> issues,
            List<AttendanceSummary> attendance, List<LedgerEntry> ledger) {
    }

    /** Validate an upload without storing anything. */
    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImportPreviewResponse> preview(@RequestBody AcademicImportRequest req) {
        ValidationReport r = importService.validate(req);
        long warnings = r.issues().stream().filter(i -> Issue.WARNING.equals(i.severity())).count();
        return ResponseEntity.ok(new ImportPreviewResponse(
                AcademicDataSourceType.STUDENT_MANUAL_IMPORT.label(), false,
                r.acceptedAttendance().size(), r.acceptedLedger().size(), r.cgpa(),
                r.rejectedCount(), warnings, r.issues(), r.acceptedAttendance(), r.acceptedLedger()));
    }

    /** Save a validated upload into the student's own records (consent required). */
    @PostMapping("/confirm")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ConfirmResult> confirm(@AuthenticationPrincipal UserDetailsImpl me,
                                                 @RequestBody AcademicImportRequest req) {
        return ResponseEntity.ok(importService.confirm(me.getId(), req));
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Map<String, Object>>> myBatches(@AuthenticationPrincipal UserDetailsImpl me) {
        List<Map<String, Object>> out = importService.listBatches(me.getId()).stream()
                .map(StudentImportController::batchSummary).toList();
        return ResponseEntity.ok(out);
    }

    @DeleteMapping("/{batchId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> deleteBatch(@AuthenticationPrincipal UserDetailsImpl me, @PathVariable Long batchId) {
        deletionService.deleteBatch(me.getId(), batchId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/consent")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> consentStatus(@AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(Map.of("active", consentService.hasActiveConsent(me.getId())));
    }

    @PostMapping("/consent")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> grantConsent(@AuthenticationPrincipal UserDetailsImpl me) {
        consentService.grant(me.getId());
        return ResponseEntity.ok(Map.of("active", true));
    }

    /** Revoke consent and delete all of the student's imported data. */
    @DeleteMapping("/consent")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> revokeConsent(@AuthenticationPrincipal UserDetailsImpl me) {
        consentService.revoke(me.getId());
        return ResponseEntity.ok(Map.of("active", false));
    }

    private static Map<String, Object> batchSummary(AcademicImportBatch b) {
        return Map.of(
                "id", b.getId(),
                "reportType", b.getReportType() == null ? "" : b.getReportType(),
                "reportDate", b.getReportDate() == null ? "" : b.getReportDate().toString(),
                "uploadedAt", b.getUploadedAt() == null ? "" : b.getUploadedAt().toString(),
                "status", b.getStatus(),
                "acceptedRows", b.getAcceptedRows(),
                "rejectedRows", b.getRejectedRows());
    }
}
