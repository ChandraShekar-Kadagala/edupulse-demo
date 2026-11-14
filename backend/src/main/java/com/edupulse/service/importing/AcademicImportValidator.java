package com.edupulse.service.importing;

import com.edupulse.service.importing.AcademicImportRecords.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates and normalizes academic records from a student-supplied SRM AP portal report before anything is
 * persisted. Pure and stateless (no I/O, no database), so every rule is unit-testable with synthetic data.
 *
 * <p>Principles:
 * <ul>
 *   <li>Missing data stays missing: blank marks become "pending", absent CGPA stays null (never 0).</li>
 *   <li>Arithmetic that does not add up is rejected rather than silently repaired.</li>
 *   <li>Duplicate rows are rejected; the first occurrence wins and the duplicate is reported.</li>
 *   <li>Only an allowlist of profile fields is kept; date of birth, gender and photo are always dropped.</li>
 * </ul>
 * See docs/srmap-data-contract.md for the field-level rules.
 */
public class AcademicImportValidator {

    static final Pattern COURSE_CODE = Pattern.compile("^([A-Z]{2,5})\\s*(\\d{3})([A-Z]?)$");
    static final Pattern CODE_IN_TEXT = Pattern.compile("\\b([A-Z]{2,5})\\s?(\\d{3})([A-Z]?)\\b");
    static final Set<String> GRADES = Set.of("O", "A+", "A", "B+", "B", "C", "D", "P", "F", "AB", "I", "W");
    static final Set<String> RESULTS = Set.of("PASS", "FAIL", "P", "F", "ABSENT", "WITHHELD", "RA");
    static final Map<String, String> DAYS = Map.ofEntries(
            Map.entry("MON", "Monday"), Map.entry("MONDAY", "Monday"),
            Map.entry("TUE", "Tuesday"), Map.entry("TUESDAY", "Tuesday"),
            Map.entry("WED", "Wednesday"), Map.entry("WEDNESDAY", "Wednesday"),
            Map.entry("THU", "Thursday"), Map.entry("THURSDAY", "Thursday"),
            Map.entry("FRI", "Friday"), Map.entry("FRIDAY", "Friday"),
            Map.entry("SAT", "Saturday"), Map.entry("SATURDAY", "Saturday"));
    /** Profile fields EduPulse may keep. Everything else (e.g. dob, gender, picture) is dropped. */
    public static final Set<String> PROFILE_ALLOWLIST =
            Set.of("registerNo", "institution", "semester", "program", "section", "specialization");
    static final double PERCENT_TOLERANCE = 1.0;

    /** Validates one import. {@code sourceType} must describe how the data actually arrived. */
    public ValidationReport validate(List<RawAttendanceRow> attendance,
                                     List<RawInternalComponent> internals,
                                     List<RawLedgerRow> ledger,
                                     List<RawTimetableRow> timetable,
                                     String rawCgpa,
                                     Map<String, String> rawProfile,
                                     AcademicDataSourceType sourceType) {
        Objects.requireNonNull(sourceType, "sourceType is required");
        List<Issue> issues = new ArrayList<>();
        return new ValidationReport(
                validateAttendance(nullSafe(attendance), issues),
                validateInternals(nullSafe(internals), issues),
                validateLedger(nullSafe(ledger), issues),
                normalizeTimetable(nullSafe(timetable), issues),
                parseCgpa(rawCgpa, issues),
                filterProfile(rawProfile),
                List.copyOf(issues),
                sourceType);
    }

    // ------------------------------------------------------------------ attendance

    List<AttendanceSummary> validateAttendance(List<RawAttendanceRow> rows, List<Issue> issues) {
        List<AttendanceSummary> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            RawAttendanceRow r = rows.get(i);
            String code = normalizeCourseCode(r.subjectCode());
            if (code == null) {
                issues.add(new Issue("attendance", i, Issue.REJECTED, "Invalid or missing subject code: '" + r.subjectCode() + "'"));
                continue;
            }
            Integer conducted = parseNonNegativeInt(r.classesConducted());
            Integer present = parseNonNegativeInt(r.present());
            Integer absent = parseNonNegativeInt(r.absent());
            Integer odMl = blank(r.odMlTaken()) ? Integer.valueOf(0) : parseNonNegativeInt(r.odMlTaken());
            if (conducted == null || present == null || absent == null || odMl == null) {
                issues.add(new Issue("attendance", i, Issue.REJECTED, code + ": counts must be non-negative integers"));
                continue;
            }
            if (present + absent != conducted) {
                issues.add(new Issue("attendance", i, Issue.REJECTED,
                        code + ": present (" + present + ") + absent (" + absent + ") != conducted (" + conducted + ")"));
                continue;
            }
            if (!seen.add(code)) {
                issues.add(new Issue("attendance", i, Issue.REJECTED, code + ": duplicate subject row ignored"));
                continue;
            }
            double computed = conducted == 0 ? 0.0 : Math.round(present * 1000.0 / conducted) / 10.0;
            Double reported = parsePercent(r.presentPercentage());
            if (conducted > 0 && reported != null && Math.abs(reported - computed) > PERCENT_TOLERANCE) {
                issues.add(new Issue("attendance", i, Issue.WARNING,
                        code + ": reported present% " + reported + " differs from computed " + computed + "; computed value used"));
            }
            if (odMl > absent) {
                issues.add(new Issue("attendance", i, Issue.WARNING, code + ": OD/ML (" + odMl + ") exceeds absences (" + absent + ")"));
            }
            out.add(new AttendanceSummary(code, trimToNull(r.subjectName()), conducted, present, absent, odMl, computed));
        }
        return out;
    }

    // ------------------------------------------------------------------ internals

    List<InternalComponent> validateInternals(List<RawInternalComponent> rows, List<Issue> issues) {
        List<InternalComponent> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            RawInternalComponent r = rows.get(i);
            String code = normalizeCourseCode(r.subjectCode());
            String component = trimToNull(r.componentName());
            if (code == null || component == null) {
                issues.add(new Issue("internals", i, Issue.REJECTED, "Missing subject code or component name"));
                continue;
            }
            Double max = parseNonNegativeDouble(r.conductedMax());
            Double convMax = parseNonNegativeDouble(r.convertedMax());
            boolean pending = blank(r.conductedObtained()) || "-".equals(r.conductedObtained().trim());
            Double obtained = pending ? null : parseNonNegativeDouble(r.conductedObtained());
            Double convObtained = blank(r.convertedObtained()) ? null : parseNonNegativeDouble(r.convertedObtained());
            if (max == null || max <= 0) {
                issues.add(new Issue("internals", i, Issue.REJECTED, code + " " + component + ": max marks must be > 0"));
                continue;
            }
            if (!pending && obtained == null) {
                issues.add(new Issue("internals", i, Issue.REJECTED, code + " " + component + ": obtained marks are not a number"));
                continue;
            }
            if (obtained != null && obtained > max) {
                issues.add(new Issue("internals", i, Issue.REJECTED,
                        code + " " + component + ": obtained (" + obtained + ") exceeds max (" + max + ")"));
                continue;
            }
            if (convObtained != null && convMax != null && convObtained > convMax) {
                issues.add(new Issue("internals", i, Issue.REJECTED,
                        code + " " + component + ": converted obtained exceeds converted max"));
                continue;
            }
            if (!seen.add(code + "|" + component.toUpperCase(Locale.ROOT))) {
                issues.add(new Issue("internals", i, Issue.REJECTED, code + " " + component + ": duplicate component ignored"));
                continue;
            }
            out.add(new InternalComponent(code, trimToNull(r.subjectName()), component, obtained, max,
                    pending ? null : convObtained, convMax, pending));
        }
        return out;
    }

    // ------------------------------------------------------------------ ledger / semester results

    List<LedgerEntry> validateLedger(List<RawLedgerRow> rows, List<Issue> issues) {
        List<LedgerEntry> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            RawLedgerRow r = rows.get(i);
            String code = normalizeCourseCode(r.subjectCode());
            String grade = r.grade() == null ? null : r.grade().trim().toUpperCase(Locale.ROOT);
            Double credit = parseNonNegativeDouble(r.credit());
            Double points = parseNonNegativeDouble(r.gradePoints());
            Integer attempt = blank(r.attempt()) ? Integer.valueOf(1) : parseNonNegativeInt(r.attempt());
            String result = r.result() == null ? null : r.result().trim().toUpperCase(Locale.ROOT);
            String semester = trimToNull(r.semester());
            if (code == null || semester == null) {
                issues.add(new Issue("ledger", i, Issue.REJECTED, "Missing subject code or semester"));
                continue;
            }
            if (grade == null || !GRADES.contains(grade)) {
                issues.add(new Issue("ledger", i, Issue.REJECTED, code + ": unknown grade '" + r.grade() + "'"));
                continue;
            }
            if (credit == null || points == null || points > 10.0 || attempt == null || attempt < 1) {
                issues.add(new Issue("ledger", i, Issue.REJECTED, code + ": credit, grade points (0-10) or attempt invalid"));
                continue;
            }
            if (result != null && !result.isEmpty() && !RESULTS.contains(result)) {
                issues.add(new Issue("ledger", i, Issue.WARNING, code + ": unrecognised result '" + r.result() + "'"));
            }
            if (!seen.add(code + "|" + semester + "|" + attempt)) {
                issues.add(new Issue("ledger", i, Issue.REJECTED, code + ": duplicate ledger row ignored"));
                continue;
            }
            out.add(new LedgerEntry(semester, trimToNull(r.monthYear()), code, trimToNull(r.subjectDescription()),
                    credit, grade, points, result, attempt));
        }
        return out;
    }

    // ------------------------------------------------------------------ CGPA

    /**
     * Returns the CGPA, or null when it is absent or unusable. A literal "0" is treated as unavailable
     * because portal clients commonly substitute "0" when the CGPA element is missing.
     */
    Double parseCgpa(String raw, List<Issue> issues) {
        if (blank(raw)) {
            return null;
        }
        String cleaned = raw.trim().replaceFirst("(?i)^CGPA\\s*:?\\s*", "");
        Double value = parseNonNegativeDouble(cleaned);
        if (value == null || value > 10.0) {
            issues.add(new Issue("cgpa", 0, Issue.REJECTED, "CGPA '" + raw + "' is not a number between 0 and 10"));
            return null;
        }
        if (value == 0.0) {
            issues.add(new Issue("cgpa", 0, Issue.WARNING, "CGPA reported as 0 treated as unavailable"));
            return null;
        }
        return value;
    }

    // ------------------------------------------------------------------ timetable

    List<TimetableSlot> normalizeTimetable(List<RawTimetableRow> rows, List<Issue> issues) {
        List<TimetableSlot> out = new ArrayList<>();
        Set<String> seenDays = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            RawTimetableRow r = rows.get(i);
            String day = r.day() == null ? null : DAYS.get(r.day().trim().toUpperCase(Locale.ROOT));
            if (day == null) {
                issues.add(new Issue("timetable", i, Issue.REJECTED, "Unknown day '" + r.day() + "'"));
                continue;
            }
            if (!seenDays.add(day)) {
                issues.add(new Issue("timetable", i, Issue.REJECTED, day + ": duplicate day row ignored"));
                continue;
            }
            List<String> cells = r.periodCells() == null ? List.of() : r.periodCells();
            for (int p = 0; p < cells.size(); p++) {
                String cell = cells.get(p) == null ? "" : cells.get(p).trim().toUpperCase(Locale.ROOT);
                Matcher m = CODE_IN_TEXT.matcher(cell);
                if (m.find()) {
                    out.add(new TimetableSlot(day, p + 1, m.group(1) + " " + m.group(2) + m.group(3)));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ profile

    static Map<String, String> filterProfile(Map<String, String> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        for (String key : PROFILE_ALLOWLIST.stream().sorted().toList()) {
            String v = raw.get(key);
            if (!blank(v)) {
                out.put(key, v.trim());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    /** "cse204", "CSE 204", " CSE  204 " -> "CSE 204"; "CSE301L" -> "CSE 301L"; anything else -> null. */
    static String normalizeCourseCode(String raw) {
        if (blank(raw)) {
            return null;
        }
        Matcher m = COURSE_CODE.matcher(raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " "));
        return m.matches() ? m.group(1) + " " + m.group(2) + m.group(3) : null;
    }

    private static Integer parseNonNegativeInt(String raw) {
        if (blank(raw)) return null;
        try {
            int v = Integer.parseInt(raw.trim());
            return v < 0 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseNonNegativeDouble(String raw) {
        if (blank(raw)) return null;
        try {
            double v = Double.parseDouble(raw.trim());
            return (v < 0 || Double.isNaN(v) || Double.isInfinite(v)) ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parsePercent(String raw) {
        return blank(raw) ? null : parseNonNegativeDouble(raw.trim().replace("%", ""));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimToNull(String s) {
        return blank(s) ? null : s.trim();
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
