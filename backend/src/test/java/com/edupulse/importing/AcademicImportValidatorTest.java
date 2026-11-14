package com.edupulse.importing;

import com.edupulse.service.importing.AcademicDataSourceType;
import com.edupulse.service.importing.AcademicImportRecords.*;
import com.edupulse.service.importing.AcademicImportValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic fixtures only: no real student, course-instructor or credential data. */
class AcademicImportValidatorTest {

    private final AcademicImportValidator validator = new AcademicImportValidator();

    private ValidationReport run(List<RawAttendanceRow> att, List<RawInternalComponent> internals,
                                 List<RawLedgerRow> ledger, List<RawTimetableRow> tt, String cgpa, Map<String, String> profile) {
        return validator.validate(att, internals, ledger, tt, cgpa, profile, AcademicDataSourceType.STUDENT_MANUAL_IMPORT);
    }

    private ValidationReport attendanceOnly(RawAttendanceRow... rows) {
        return run(List.of(rows), null, null, null, null, null);
    }

    private static RawAttendanceRow att(String code, String conducted, String present, String absent, String presentPct) {
        return new RawAttendanceRow(code, "Synthetic Subject", conducted, present, absent, "0", presentPct, "0.00", presentPct);
    }

    private static long rejected(ValidationReport r, String section) {
        return r.issues().stream().filter(i -> i.section().equals(section) && Issue.REJECTED.equals(i.severity())).count();
    }

    @Nested
    @DisplayName("Attendance")
    class Attendance {
        @Test
        void validRowIsNormalized() {
            ValidationReport r = attendanceOnly(att(" syn101 ", "40", "34", "6", "85.00"));
            AttendanceSummary a = r.acceptedAttendance().get(0);
            assertEquals("SYN 101", a.courseCode());
            assertEquals(40, a.conducted());
            assertEquals(85.0, a.computedPercentage());
            assertTrue(r.issues().isEmpty());
        }

        @Test
        void presentPlusAbsentMustEqualConducted() {
            ValidationReport r = attendanceOnly(att("SYN 101", "40", "30", "6", "75.00"));
            assertTrue(r.acceptedAttendance().isEmpty());
            assertEquals(1, rejected(r, "attendance"));
        }

        @Test
        void negativeOrNonNumericCountsRejected() {
            ValidationReport r = attendanceOnly(att("SYN 101", "-10", "5", "-15", "0"), att("SYN 102", "abc", "1", "1", "50"));
            assertTrue(r.acceptedAttendance().isEmpty());
            assertEquals(2, rejected(r, "attendance"));
        }

        @Test
        void missingSubjectCodeRejected() {
            ValidationReport r = attendanceOnly(att("", "30", "25", "5", "83.33"), att("Missing Code", "30", "25", "5", "83.33"));
            assertEquals(2, rejected(r, "attendance"));
        }

        @Test
        void mismatchedReportedPercentageIsAWarningAndComputedValueWins() {
            ValidationReport r = attendanceOnly(att("SYN 101", "40", "20", "20", "90.00"));
            assertEquals(50.0, r.acceptedAttendance().get(0).computedPercentage());
            assertEquals(Issue.WARNING, r.issues().get(0).severity());
        }

        @Test
        void duplicateSubjectRejectedFirstWins() {
            ValidationReport r = attendanceOnly(att("SYN 101", "40", "34", "6", "85.00"), att("syn101", "40", "10", "30", "25.00"));
            assertEquals(1, r.acceptedAttendance().size());
            assertEquals(34, r.acceptedAttendance().get(0).present());
            assertEquals(1, rejected(r, "attendance"));
        }

        @Test
        void zeroConductedIsAcceptedWithoutDividingByZero() {
            ValidationReport r = attendanceOnly(att("SYN 101", "0", "0", "0", "0"));
            assertEquals(0.0, r.acceptedAttendance().get(0).computedPercentage());
        }
    }

    @Nested
    @DisplayName("Internal assessments")
    class Internals {
        private ValidationReport internals(RawInternalComponent... rows) {
            return run(null, List.of(rows), null, null, null, null);
        }

        @Test
        void blankObtainedIsPendingNotZero() {
            ValidationReport r = internals(new RawInternalComponent("SYN 101", "Synthetic", "Quiz 2", "", "10", "", "5"));
            InternalComponent c = r.acceptedInternals().get(0);
            assertTrue(c.pending());
            assertNull(c.obtained(), "pending must never become 0");
            assertNull(c.convertedObtained());
        }

        @Test
        void obtainedAboveMaxRejected() {
            ValidationReport r = internals(new RawInternalComponent("SYN 101", "Synthetic", "Mid", "31", "30", "26", "25"));
            assertTrue(r.acceptedInternals().isEmpty());
            assertEquals(1, rejected(r, "internals"));
        }

        @Test
        void zeroOrMissingMaxRejected() {
            ValidationReport r = internals(new RawInternalComponent("SYN 101", "Synthetic", "Mid", "5", "0", "", ""));
            assertEquals(1, rejected(r, "internals"));
        }

        @Test
        void duplicateComponentRejected() {
            ValidationReport r = internals(
                    new RawInternalComponent("SYN 101", "Synthetic", "Quiz 1", "8", "10", "4", "5"),
                    new RawInternalComponent("SYN 101", "Synthetic", "quiz 1", "9", "10", "4.5", "5"));
            assertEquals(1, r.acceptedInternals().size());
            assertEquals(8.0, r.acceptedInternals().get(0).obtained());
        }
    }

    @Nested
    @DisplayName("CGPA")
    class Cgpa {
        private Double cgpa(String raw) {
            return run(null, null, null, null, raw, null).cgpa();
        }

        @Test
        void validValuesParsed() {
            assertEquals(8.85, cgpa("8.85"));
            assertEquals(7.2, cgpa("CGPA : 7.2"));
        }

        @Test
        void absentStaysNullNeverZero() {
            assertNull(cgpa(null));
            assertNull(cgpa("  "));
        }

        @Test
        void literalZeroTreatedAsUnavailableWithWarning() {
            ValidationReport r = run(null, null, null, null, "0", null);
            assertNull(r.cgpa());
            assertEquals(Issue.WARNING, r.issues().get(0).severity());
        }

        @Test
        void outOfRangeOrNonNumericRejected() {
            assertNull(cgpa("11.2"));
            assertNull(cgpa("N/A"));
            assertNull(cgpa("-1"));
        }
    }

    @Nested
    @DisplayName("Semester results (exam ledger)")
    class Ledger {
        private static RawLedgerRow row(String code, String grade, String points, String attempt) {
            return new RawLedgerRow("Semester 1", "DEC-2099", code, "Synthetic Subject", "4.0", grade, points, "PASS", attempt);
        }

        @Test
        void validRowAccepted() {
            ValidationReport r = run(null, null, List.of(row("SYN 101", "a+", "9", "1")), null, null, null);
            LedgerEntry e = r.acceptedLedger().get(0);
            assertEquals("A+", e.grade());
            assertEquals(9.0, e.gradePoints());
        }

        @Test
        void unknownGradeOrOutOfRangePointsRejected() {
            ValidationReport r = run(null, null, List.of(row("SYN 101", "Z", "9", "1"), row("SYN 102", "A", "12", "1")), null, null, null);
            assertTrue(r.acceptedLedger().isEmpty());
            assertEquals(2, rejected(r, "ledger"));
        }

        @Test
        void sameSubjectDifferentAttemptKeptButExactDuplicateRejected() {
            ValidationReport r = run(null, null, List.of(row("SYN 101", "F", "0", "1"), row("SYN 101", "B", "7", "2"),
                    row("SYN 101", "B", "7", "2")), null, null, null);
            assertEquals(2, r.acceptedLedger().size());
            assertEquals(1, rejected(r, "ledger"));
        }
    }

    @Nested
    @DisplayName("Timetable normalization")
    class Timetable {
        @Test
        void daysAndCourseCodesNormalized() {
            ValidationReport r = run(null, null, null, List.of(
                    new RawTimetableRow("MON", List.of("SYN 101-LEC", "", "syn102 (R 101)", "-")),
                    new RawTimetableRow("Tuesday", List.of("FREE", "SYN201L LAB"))), null, null);
            assertEquals(List.of(
                    new TimetableSlot("Monday", 1, "SYN 101"),
                    new TimetableSlot("Monday", 3, "SYN 102"),
                    new TimetableSlot("Tuesday", 2, "SYN 201L")), r.timetable());
        }

        @Test
        void unknownAndDuplicateDaysRejected() {
            ValidationReport r = run(null, null, null, List.of(
                    new RawTimetableRow("Funday", List.of("SYN 101")),
                    new RawTimetableRow("Wed", List.of("SYN 101")),
                    new RawTimetableRow("WEDNESDAY", List.of("SYN 102"))), null, null);
            assertEquals(1, r.timetable().size());
            assertEquals(2, rejected(r, "timetable"));
        }
    }

    @Nested
    @DisplayName("Provenance, privacy and idempotency")
    class ProvenanceAndPrivacy {
        @Test
        void manualImportIsNeverLabelledLive() {
            for (AcademicDataSourceType t : AcademicDataSourceType.values()) {
                if (t != AcademicDataSourceType.LIVE_AUTHORIZED_CONNECTION) {
                    assertFalse(t.isLive(), t + " must not be presented as live");
                    assertFalse(t.label().toLowerCase(Locale.ROOT).contains("live"), t.label());
                }
            }
            ValidationReport r = run(null, null, null, null, null, null);
            assertEquals(AcademicDataSourceType.STUDENT_MANUAL_IMPORT, r.sourceType());
            assertTrue(r.sourceType().label().contains("not independently verified"));
        }

        @Test
        void sourceTypeIsMandatory() {
            assertThrows(NullPointerException.class,
                    () -> validator.validate(null, null, null, null, null, null, null));
        }

        @Test
        void sensitiveProfileFieldsAreDropped() {
            Map<String, String> raw = new HashMap<>();
            raw.put("registerNo", "SYN-0000");
            raw.put("program", "B.Tech");
            raw.put("section", "Z");
            raw.put("dob", "01-01-2000");
            raw.put("gender", "X");
            raw.put("picture", "https://example.invalid/p.jpg");
            raw.put("password", "should-never-be-here");
            raw.put("studentName", "Synthetic Person");
            Map<String, String> kept = run(null, null, null, null, null, raw).profile();
            assertEquals(Set.of("registerNo", "program", "section"), kept.keySet());
            assertTrue(AcademicImportValidator.PROFILE_ALLOWLIST.stream().noneMatch(k -> k.matches("(?i).*(dob|gender|picture|password|name).*")));
        }

        @Test
        void validatingTheSameImportTwiceIsDeterministic() {
            List<RawAttendanceRow> rows = List.of(att("SYN 101", "40", "34", "6", "85.00"), att("SYN 102", "30", "15", "15", "50.00"));
            assertEquals(run(rows, null, null, null, "8.1", null), run(rows, null, null, null, "8.1", null));
        }

        @Test
        void nullSectionsAreEmptyNotErrors() {
            ValidationReport r = run(null, null, null, null, null, null);
            assertTrue(r.acceptedAttendance().isEmpty() && r.acceptedInternals().isEmpty()
                    && r.acceptedLedger().isEmpty() && r.timetable().isEmpty() && r.issues().isEmpty());
            assertNull(r.cgpa());
        }
    }
}
