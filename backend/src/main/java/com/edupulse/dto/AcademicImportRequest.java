package com.edupulse.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Payload posted by the student-side export tool (frontend/src/features/import/parseReport.ts).
 * Every field is a raw string; the server's {@code AcademicImportValidator} decides what is valid.
 * {@code sourceType} from the client is ignored — the server always treats an upload as a manual import.
 */
@Data
public class AcademicImportRequest {

    private String sourceType;
    private String reportDate;
    private List<AttendanceRow> attendance;
    private List<LedgerRow> ledger;
    private String cgpa;
    private Map<String, String> profile;

    @Data
    public static class AttendanceRow {
        private String subjectCode;
        private String subjectName;
        private String classesConducted;
        private String present;
        private String absent;
        private String odMlTaken;
        private String presentPercentage;
        private String odMlPercentage;
        private String attendancePercentage;
    }

    @Data
    public static class LedgerRow {
        private String semester;
        private String monthYear;
        private String subjectCode;
        private String subjectDescription;
        private String credit;
        private String grade;
        private String gradePoints;
        private String result;
        private String attempt;
    }
}
