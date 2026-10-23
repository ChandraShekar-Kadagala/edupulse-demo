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
            Element table = doc.selectFirst("table#attendanceTable, table:has(th:contains(Course Code))");
            if (table == null) {
                errors.add("No attendance table found matching schema (expected #attendanceTable or headers with 'Course Code')");
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
                    // Try class-based selection first, fallback to positional
                    String courseCode = getCellText(row, ".course-code", cols, cols.size() >= 8 ? 1 : 0);
                    String courseName = getCellText(row, ".course-name", cols, cols.size() >= 8 ? 2 : 1);
                    String conductedStr = getCellText(row, ".hours-conducted", cols, cols.size() >= 8 ? 6 : 2);
                    String attendedStr = getCellText(row, ".hours-attended", cols, cols.size() >= 8 ? 7 : 3);

                    if (courseCode == null || courseCode.trim().isEmpty()) {
                        errors.add("Row " + rowsProcessed + ": Course code is empty");
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
            Element table = doc.selectFirst("table#marksTable, table:has(th:contains(Assessment Name)), table:has(th:contains(Marks Obtained))");
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
                    String courseCode = getCellText(row, ".course-code", cols, 1);
                    String courseName = getCellText(row, ".course-name", cols, 2);
                    String assessType = getCellText(row, "td:nth-child(4)", cols, 3);
                    String assessName = getCellText(row, ".assessment-name", cols, 4);
                    String maxMarksStr = getCellText(row, ".max-marks", cols, 5);
                    String marksObtainedStr = getCellText(row, ".marks-obtained", cols, 6);
                    String weightageStr = getCellText(row, ".weightage", cols, cols.size() >= 8 ? 7 : -1);

                    if (courseCode == null || courseCode.trim().isEmpty()) {
                        errors.add("Row " + rowsProcessed + ": Course code is empty");
                        continue;
                    }

                    double maxMarks = Double.parseDouble(maxMarksStr.trim());
                    if (maxMarks <= 0) {
                        errors.add("Row " + rowsProcessed + " (" + courseCode + "): Max marks must be positive, got " + maxMarks);
                        continue;
                    }

                    Double marksObtained = null;
                    Double percentage = null;
                    boolean isEvaluated = false;

                    String trimmedMarks = marksObtainedStr != null ? marksObtainedStr.trim() : "";
                    if (!trimmedMarks.isEmpty() && !"-".equals(trimmedMarks) && !"N/A".equalsIgnoreCase(trimmedMarks) && !"Pending".equalsIgnoreCase(trimmedMarks)) {
                        double parsed = Double.parseDouble(trimmedMarks);
                        if (parsed < 0 || parsed > (maxMarks * 1.10)) {
                            errors.add("Row " + rowsProcessed + " (" + courseCode + "): Marks obtained (" + parsed + ") outside plausible range (0-" + maxMarks + ")");
                            continue;
                        }
                        marksObtained = parsed;
                        percentage = Math.round(((marksObtained / maxMarks) * 100.0) * 100.0) / 100.0;
                        isEvaluated = true;
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
