package com.edupulse.service.provider.portal;

import com.edupulse.dto.AssessmentMarkDto;
import com.edupulse.dto.CourseAttendanceDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SrmPortalParserService {

    private static final Logger log = LoggerFactory.getLogger(SrmPortalParserService.class);

    public record AttendanceParseResult(
            List<CourseAttendanceDto> records,
            List<String> errors,
            int rowsProcessed,
            int validCount
    ) {}

    public record MarksParseResult(
            List<AssessmentMarkDto> records,
            List<String> errors,
            int rowsProcessed,
            int validCount
    ) {}

    public record ExamLedgerRowDto(
            String semester,
            String monthYear,
            String courseCode,
            String courseName,
            Double credit,
            String grade,
            Double gradePoints,
            String result,
            String attempt
    ) {}

    public record ExamLedgerParseResult(
            List<ExamLedgerRowDto> records,
            Double overallCgpa,
            List<String> errors,
            int rowsProcessed,
            int validCount
    ) {}

    public AttendanceParseResult parseAttendanceHtml(String htmlContent) {
        List<CourseAttendanceDto> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int rowsProcessed = 0;

        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            errors.add("Empty or null HTML content provided");
            return new AttendanceParseResult(records, errors, 0, 0);
        }

        try {
            Document doc = Jsoup.parse(htmlContent);
            Element table = doc.selectFirst("table#tblSubjectWiseAttendance, table#attendanceTable, table:has(th:contains(Course Code)), table:has(th:contains(Subject Code)), table:has(th:contains(Total Conducted))");
            if (table == null) {
                errors.add("No attendance table found matching schema (expected #tblSubjectWiseAttendance, #attendanceTable or headers with 'Course Code' / 'Subject Code')");
                return new AttendanceParseResult(records, errors, 0, 0);
            }

            Elements rows = table.select("tbody tr");
            if (rows.isEmpty()) {
                rows = table.select("tr:has(td)");
            }

            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() < 4) {
                    continue; // skip header or separator rows
                }
                rowsProcessed++;

                try {
                    String courseCode = null;
                    String courseName = null;
                    String conductedStr = null;
                    String attendedStr = null;

                    // 1. Try class-based selection first
                    if (row.selectFirst(".course-code") != null) {
                        courseCode = getCellText(row, ".course-code", cols, -1);
                        courseName = getCellText(row, ".course-name", cols, -1);
                        conductedStr = getCellText(row, ".hours-conducted", cols, -1);
                        attendedStr = getCellText(row, ".hours-attended", cols, -1);
                    }

                    // 2. If not class-based, determine column mapping
                    if (courseCode == null) {
                        String firstColText = cols.get(0).text().trim();
                        boolean firstColIsNumber = firstColText.matches("^\\d+$");

                        if (firstColIsNumber && cols.size() >= 8) {
                            // Format: S.No (0), Course Code (1), Course Name (2), ..., Conducted (6), Attended (7), ...
                            courseCode = cols.get(1).text().trim();
                            courseName = cols.get(2).text().trim();
                            conductedStr = cols.get(6).text().trim();
                            attendedStr = cols.get(7).text().trim();
                        } else if (cols.size() >= 4) {
                            // SRM AP portal format (tblSubjectWiseAttendance):
                            // Subject Code (0), Subject Name (1), Total Conducted (2), Attended (3), ...
                            courseCode = cols.get(0).text().trim();
                            courseName = cols.get(1).text().trim();
                            conductedStr = cols.get(2).text().trim();
                            attendedStr = cols.get(3).text().trim();
                        }
                    }

                    if (courseCode == null || courseCode.trim().isEmpty() ||
                            "Subject Code".equalsIgnoreCase(courseCode) || "Course Code".equalsIgnoreCase(courseCode)) {
                        continue;
                    }

                    int conducted = Integer.parseInt(conductedStr.trim());
                    int attended = Integer.parseInt(attendedStr.trim());

                    if (conducted < 0 || attended < 0) {
                        errors.add("Row " + rowsProcessed + " (" + courseCode + "): Hours cannot be negative (conducted: " + conducted + ", attended: " + attended + ")");
                        continue;
                    }

                    if (attended > conducted) {
                        errors.add("Row " + rowsProcessed + " (" + courseCode + "): Attended hours (" + attended + ") exceeds conducted hours (" + conducted + ")");
                        continue;
                    }

                    double pct = conducted > 0 ? (attended * 100.0) / conducted : 100.0;
                    String status = pct >= 85.0 ? "NORMAL" : pct >= 75.0 ? "WARNING" : "CRITICAL";

                    records.add(CourseAttendanceDto.builder()
                            .courseCode(courseCode.trim().toUpperCase())
                            .courseName(courseName != null ? courseName.trim() : "Unknown Course")
                            .totalClasses((long) conducted)
                            .presentClasses((long) attended)
                            .absentClasses((long) (conducted - attended))
                            .totalHours((long) conducted)
                            .attendedHours((long) attended)
                            .attendancePercentage(Math.round(pct * 100.0) / 100.0)
                            .status(status)
                            .build());

                } catch (NumberFormatException e) {
                    errors.add("Row " + rowsProcessed + ": Invalid numeric format for hours");
                } catch (Exception e) {
                    errors.add("Row " + rowsProcessed + ": Unexpected parse error: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse attendance HTML", e);
            errors.add("Document parsing failure: " + e.getMessage());
        }

        return new AttendanceParseResult(records, errors, rowsProcessed, records.size());
    }

    public MarksParseResult parseMarksHtml(String htmlContent) {
        List<AssessmentMarkDto> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int rowsProcessed = 0;

        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            errors.add("Empty or null HTML content provided");
            return new MarksParseResult(records, errors, 0, 0);
        }

        try {
            Document doc = Jsoup.parse(htmlContent);
            Element table = doc.selectFirst("table#marksTable, table.table-striped:has(th:contains(Marks)), table:has(th:contains(Assessment Name)), table:has(th:contains(Marks Obtained)), table:has(th:contains(Max Marks))");
            if (table == null) {
                errors.add("No marks table found matching schema (expected #marksTable or headers with 'Marks Obtained')");
                return new MarksParseResult(records, errors, 0, 0);
            }

            Elements rows = table.select("tbody tr");
            if (rows.isEmpty()) {
                rows = table.select("tr:has(td)");
            }

            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() < 4) {
                    continue;
                }
                rowsProcessed++;

                try {
                    String courseCode;
                    String courseName;
                    String assessType;
                    String assessName;
                    String maxMarksStr;
                    String marksObtainedStr;
                    String weightageStr = null;

                    if (cols.size() == 4) {
                        // SRM AP internal marks format: Subject Code (0), Subject Name (1), Marks Obtained (2), Max Marks (3)
                        courseCode = cols.get(0).text();
                        courseName = cols.get(1).text();
                        assessType = "INTERNAL";
                        assessName = "Internal Assessment";
                        marksObtainedStr = cols.get(2).text();
                        maxMarksStr = cols.get(3).text();
                    } else {
                        courseCode = getCellText(row, ".course-code", cols, 1);
                        courseName = getCellText(row, ".course-name", cols, 2);
                        assessType = getCellText(row, "td:nth-child(4)", cols, 3);
                        assessName = getCellText(row, ".assessment-name", cols, 4);
                        maxMarksStr = getCellText(row, ".max-marks", cols, 5);
                        marksObtainedStr = getCellText(row, ".marks-obtained", cols, 6);
                        weightageStr = getCellText(row, ".weightage", cols, cols.size() >= 8 ? 7 : -1);
                    }

                    if (courseCode == null || courseCode.trim().isEmpty() || "Subject Code".equalsIgnoreCase(courseCode) || "Course Code".equalsIgnoreCase(courseCode)) {
                        continue;
                    }

                    double maxMarks = Double.parseDouble(maxMarksStr.trim());
                    if (maxMarks <= 0) {
                        errors.add("Row " + rowsProcessed + " (" + courseCode + "): Max marks must be positive, got " + maxMarks);
                        continue;
                    }

                    Double marksObtained = null;
                    Double percentage = null;

                    String trimmedMarks = marksObtainedStr != null ? marksObtainedStr.trim() : "";
                    if (!trimmedMarks.isEmpty() && !"-".equals(trimmedMarks) && !"N/A".equalsIgnoreCase(trimmedMarks) && !"Pending".equalsIgnoreCase(trimmedMarks)) {
                        double parsed = Double.parseDouble(trimmedMarks);
                        if (parsed < 0 || parsed > (maxMarks * 1.10)) {
                            errors.add("Row " + rowsProcessed + " (" + courseCode + "): Marks obtained (" + parsed + ") outside plausible range (0-" + maxMarks + ")");
                            continue;
                        }
                        marksObtained = parsed;
                        percentage = Math.round(((marksObtained / maxMarks) * 100.0) * 100.0) / 100.0;
                    }

                    Double weightage = null;
                    if (weightageStr != null && !weightageStr.trim().isEmpty()) {
                        try {
                            weightage = Double.parseDouble(weightageStr.trim());
                        } catch (NumberFormatException ignored) {}
                    }

                    records.add(AssessmentMarkDto.builder()
                            .courseCode(courseCode.trim().toUpperCase())
                            .courseName(courseName != null ? courseName.trim() : "Unknown Course")
                            .assessmentType(assessType != null ? assessType.trim().toUpperCase() : "INTERNAL")
                            .assessmentName(assessName != null ? assessName.trim() : "Assessment")
                            .maxMarks(maxMarks)
                            .marksObtained(marksObtained)
                            .percentage(percentage)
                            .weightage(weightage)
                            .build());

                } catch (NumberFormatException e) {
                    errors.add("Row " + rowsProcessed + ": Invalid numeric format for marks");
                } catch (Exception e) {
                    errors.add("Row " + rowsProcessed + ": Parse error: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse marks HTML", e);
            errors.add("Document parsing failure: " + e.getMessage());
        }

        return new MarksParseResult(records, errors, rowsProcessed, records.size());
    }

    public ExamLedgerParseResult parseExamLedgerHtml(String htmlContent) {
        List<ExamLedgerRowDto> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int rowsProcessed = 0;
        Double cgpa = null;

        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            errors.add("Empty or null HTML content provided");
            return new ExamLedgerParseResult(records, null, errors, 0, 0);
        }

        try {
            Document doc = Jsoup.parse(htmlContent);

            // Extract CGPA if present in text (handles "CGPA : 8.85", "CGPA: 8.85", "(CGPA) : 8.85")
            Pattern cgpaPattern = Pattern.compile("CGPA[^0-9:]*:\\s*([0-9]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = cgpaPattern.matcher(doc.text());
            if (matcher.find()) {
                try {
                    cgpa = Double.parseDouble(matcher.group(1));
                } catch (NumberFormatException ignored) {}
            }

            Elements rows = doc.select("div.subTable table tr, table:has(th:contains(Grade)) tr");
            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() < 8) {
                    continue; // Skip header or subheader rows
                }
                rowsProcessed++;

                try {
                    String semester = cols.get(0).text().trim();
                    String monthYear = cols.get(1).text().trim();
                    String courseCode = cols.get(2).text().trim();
                    String courseName = cols.get(3).text().trim();
                    String creditStr = cols.get(4).text().trim();
                    String grade = cols.get(5).text().trim();
                    String gradePointsStr = cols.get(6).text().trim();
                    String result = cols.get(7).text().trim();
                    String attempt = cols.size() >= 9 ? cols.get(8).text().trim() : "1";

                    if (courseCode.isEmpty() || "Subject Code".equalsIgnoreCase(courseCode)) {
                        continue;
                    }

                    Double credit = null;
                    try {
                        credit = Double.parseDouble(creditStr);
                    } catch (NumberFormatException ignored) {}

                    Double gradePoints = null;
                    try {
                        gradePoints = Double.parseDouble(gradePointsStr);
                    } catch (NumberFormatException ignored) {}

                    records.add(new ExamLedgerRowDto(
                            semester, monthYear, courseCode.toUpperCase(), courseName,
                            credit, grade, gradePoints, result, attempt
                    ));
                } catch (Exception e) {
                    errors.add("Row " + rowsProcessed + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse exam ledger HTML", e);
            errors.add("Document parsing failure: " + e.getMessage());
        }

        return new ExamLedgerParseResult(records, cgpa, errors, rowsProcessed, records.size());
    }

    private String getCellText(Element row, String cssSelector, Elements cols, int fallbackIndex) {
        Element el = row.selectFirst(cssSelector);
        if (el != null) {
            return el.text();
        }
        if (fallbackIndex >= 0 && fallbackIndex < cols.size()) {
            return cols.get(fallbackIndex).text();
        }
        return null;
    }
}
