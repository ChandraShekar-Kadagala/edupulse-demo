package com.edupulse.service.importing;

import java.util.List;
import java.util.Map;

/**
 * Raw (string-typed) academic records as they appear in an SRM AP portal report, mirroring the
 * normalized shapes documented in docs/srmap-data-contract.md, plus the validated outputs.
 * Raw values stay strings so the validator, not the parser, decides what is acceptable.
 */
public final class AcademicImportRecords {

    private AcademicImportRecords() {
    }

    // ---------- raw inputs (as parsed from a report) ----------

    /** Subject-wise attendance row (report "Attendance", 9 columns). */
    public record RawAttendanceRow(String subjectCode, String subjectName, String classesConducted, String present,
                                   String absent, String odMlTaken, String presentPercentage,
                                   String odMlPercentage, String attendancePercentage) {
    }

    /** One internal assessment component of a subject (report "Internal Marks"). */
    public record RawInternalComponent(String subjectCode, String subjectName, String componentName,
                                       String conductedObtained, String conductedMax,
                                       String convertedObtained, String convertedMax) {
    }

    /** Exam ledger row (report "Exam Ledger"). */
    public record RawLedgerRow(String semester, String monthYear, String subjectCode, String subjectDescription,
                               String credit, String grade, String gradePoints, String result, String attempt) {
    }

    /** Timetable grid row: a day label followed by one cell per period. */
    public record RawTimetableRow(String day, List<String> periodCells) {
    }

    // ---------- validated outputs ----------

    public record AttendanceSummary(String courseCode, String courseName, int conducted, int present, int absent,
                                    int odMl, double computedPercentage) {
    }

    public record InternalComponent(String courseCode, String courseName, String componentName,
                                    Double obtained, Double max, Double convertedObtained, Double convertedMax,
                                    boolean pending) {
    }

    public record LedgerEntry(String semester, String monthYear, String courseCode, String courseTitle,
                              double credit, String grade, double gradePoints, String result, int attempt) {
    }

    public record TimetableSlot(String day, int period, String courseCode) {
    }

    /** A finding attached to one input row (index is 0-based within its section). */
    public record Issue(String section, int index, String severity, String message) {
        public static final String REJECTED = "REJECTED";
        public static final String WARNING = "WARNING";
    }

    /**
     * Result of validating one import. Only {@code accepted*} collections may be persisted.
     * {@code cgpa} is null when absent or unusable; it is never defaulted to zero.
     */
    public record ValidationReport(List<AttendanceSummary> acceptedAttendance,
                                   List<InternalComponent> acceptedInternals,
                                   List<LedgerEntry> acceptedLedger,
                                   List<TimetableSlot> timetable,
                                   Double cgpa,
                                   Map<String, String> profile,
                                   List<Issue> issues,
                                   AcademicDataSourceType sourceType) {

        public long rejectedCount() {
            return issues.stream().filter(i -> Issue.REJECTED.equals(i.severity())).count();
        }
    }
}
