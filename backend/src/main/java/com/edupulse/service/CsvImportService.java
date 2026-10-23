package com.edupulse.service;

import com.edupulse.model.*;
import com.edupulse.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CsvImportService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> processCsv(MultipartFile file) throws Exception {
        return processCsv(file, null, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> processCsv(MultipartFile file, Long uploaderUserId) throws Exception {
        return processCsv(file, uploaderUserId, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> processCsv(MultipartFile file, Long uploaderUserId, boolean atomic) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty or missing");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit (5 MB)");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("File must be a valid CSV file with .csv extension");
        }

        byte[] fileBytes = file.getBytes();
        String fingerprint = computeSha256(fileBytes);

        int total = 0;
        int accepted = 0;
        int duplicate = 0;
        int rejected = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IllegalArgumentException("CSV file contains no header row");
            }

            // Remove potential UTF-8 BOM
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String[] rawHeaders = headerLine.split(",");
            Map<String, Integer> headerMap = parseHeaders(rawHeaders);

            validateRequiredHeaders(headerMap);

            int studentIdx = getHeaderIndex(headerMap, "studentid", "studentnumber", "student");
            int periodIdx = getHeaderIndex(headerMap, "academicperiod", "period", "semester");
            int courseIdx = getHeaderIndex(headerMap, "coursecode", "course");
            int attIdx = getHeaderIndex(headerMap, "attendancepercentage", "attendancepct", "attendance");
            int marksIdx = getHeaderIndex(headerMap, "marksobtained", "marks", "mark");
            int maxMarksIdx = getHeaderIndex(headerMap, "maxmarks", "max");
            int dateIdx = getHeaderIndex(headerMap, "date", "attendancedate", "sessiondate");

            String line;
            int rowNum = 1;

            while ((line = br.readLine()) != null) {
                rowNum++;
                if (line.trim().isEmpty()) continue;
                total++;

                String[] columns = line.split(",", -1);
                int minExpected = Math.max(Math.max(studentIdx, periodIdx), Math.max(Math.max(courseIdx, attIdx), Math.max(marksIdx, maxMarksIdx)));
                if (columns.length <= minExpected) {
                    rejected++;
                    errors.add(Map.of("row", rowNum, "reason", "Insufficient columns in row"));
                    continue;
                }

                String studentStrId = columns[studentIdx].trim();
                String period = columns[periodIdx].trim();
                String courseCode = columns[courseIdx].trim();
                String attStr = columns[attIdx].trim();
                String marksStr = columns[marksIdx].trim();
                String maxMarksStr = columns[maxMarksIdx].trim();

                if (studentStrId.isEmpty() || period.isEmpty() || courseCode.isEmpty() || attStr.isEmpty() || marksStr.isEmpty() || maxMarksStr.isEmpty()) {
                    rejected++;
                    errors.add(Map.of("row", rowNum, "studentId", studentStrId, "reason", "Required cell value is blank"));
                    continue;
                }

                double attendancePct;
                double marksObtained;
                double maxMarks;

                try {
                    attendancePct = Double.parseDouble(attStr);
                    marksObtained = Double.parseDouble(marksStr);
                    maxMarks = Double.parseDouble(maxMarksStr);
                } catch (NumberFormatException e) {
                    rejected++;
                    errors.add(Map.of("row", rowNum, "studentId", studentStrId, "reason", "Invalid numeric value"));
                    continue;
                }

                if (attendancePct < 0.0 || attendancePct > 100.0) {
                    rejected++;
                    errors.add(Map.of("row", rowNum, "studentId", studentStrId, "reason", "Attendance must be between 0 and 100"));
                    continue;
                }

                if (marksObtained < 0.0) {
                    rejected++;
                    errors.add(Map.of("row", rowNum, "studentId", studentStrId, "reason", "Marks obtained cannot be negative"));
                    continue;
                }

                if (maxMarks <= 0.0) {
                    rejected++;
                    errors.add(Map.of("row", rowNum, "studentId", studentStrId, "reason", "Max marks must be greater than zero"));
                    continue;
                }

                if (marksObtained > maxMarks) {
                    rejected++;
                    errors.add(Map.of("row", rowNum, "studentId", studentStrId, "reason", "Marks obtained (" + marksObtained + ") cannot exceed max marks (" + maxMarks + ")"));
                    continue;
                }

                // Explicit date if present
                LocalDate explicitDate = null;
                if (dateIdx != -1 && dateIdx < columns.length && !columns[dateIdx].trim().isEmpty()) {
                    try {
                        explicitDate = LocalDate.parse(columns[dateIdx].trim());
                    } catch (Exception e) {
                        rejected++;
                        errors.add(Map.of("row", rowNum, "studentId", studentStrId, "reason", "Invalid date format, expected YYYY-MM-DD"));
                        continue;
                    }
                }

                // Student existence lookup
                Optional<Student> studentOpt = studentRepository.findByStudentNumber(studentStrId);
                if (studentOpt.isEmpty()) {
                    try {
                        Long uid = Long.parseLong(studentStrId);
                        studentOpt = studentRepository.findById(uid);
                    } catch (Exception ignored) {}
                }

                if (studentOpt.isEmpty()) {
                    rejected++;
                    errors.add(Map.of("row", rowNum, "studentId", studentStrId, "reason", "Student not found in system: " + studentStrId));
                    continue;
                }

                Student student = studentOpt.get();

                // Find or create course
                Course course = courseRepository.findByCode(courseCode).orElseGet(() -> {
                    Course newCourse = Course.builder()
                            .code(courseCode)
                            .name("Imported Course " + courseCode)
                            .credits(3)
                            .build();
                    return courseRepository.save(newCourse);
                });

                // Assessment reuse / lookup
                String assessmentName = "Imported " + period;
                Assessment assessment = assessmentRepository.findByCourseIdAndName(course.getId(), assessmentName).orElseGet(() -> {
                    Assessment newAssessment = Assessment.builder()
                            .name(assessmentName)
                            .maxMarks(maxMarks)
                            .course(course)
                            .type(AssessmentType.INTERNAL)
                            .build();
                    return assessmentRepository.save(newAssessment);
                });

                // Duplicate mark handling
                boolean markUnchanged = false;
                Optional<Mark> existingMarkOpt = markRepository.findByStudentIdAndAssessmentId(student.getId(), assessment.getId());
                if (existingMarkOpt.isPresent()) {
                    Mark existingMark = existingMarkOpt.get();
                    if (Double.compare(existingMark.getMarksObtained(), marksObtained) == 0) {
                        markUnchanged = true;
                    } else {
                        existingMark.setMarksObtained(marksObtained);
                        markRepository.save(existingMark);
                    }
                } else {
                    Mark m = Mark.builder()
                            .student(student)
                            .assessment(assessment)
                            .marksObtained(marksObtained)
                            .build();
                    markRepository.save(m);
                }

                // Idempotent attendance persistence
                boolean attendanceModified = false;
                if (explicitDate != null) {
                    AttendanceStatus status = (attendancePct >= 50.0) ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT;
                    Optional<Attendance> existingAtt = attendanceRepository.findByStudentIdAndCourseIdAndDate(student.getId(), course.getId(), explicitDate);
                    if (existingAtt.isEmpty()) {
                        attendanceRepository.save(Attendance.builder().student(student).course(course).date(explicitDate).status(status).build());
                        attendanceModified = true;
                    } else if (existingAtt.get().getStatus() != status) {
                        existingAtt.get().setStatus(status);
                        attendanceRepository.save(existingAtt.get());
                        attendanceModified = true;
                    }
                } else {
                    LocalDate anchorDate = derivePeriodBaseDate(period);
                    int presentDays = (int) Math.round(attendancePct / 10.0);
                    if (presentDays > 10) presentDays = 10;
                    int absentDays = 10 - presentDays;

                    for (int i = 0; i < presentDays; i++) {
                        LocalDate d = anchorDate.minusDays(i);
                        Optional<Attendance> existing = attendanceRepository.findByStudentIdAndCourseIdAndDate(student.getId(), course.getId(), d);
                        if (existing.isEmpty()) {
                            attendanceRepository.save(Attendance.builder().student(student).course(course).date(d).status(AttendanceStatus.PRESENT).build());
                            attendanceModified = true;
                        } else if (existing.get().getStatus() != AttendanceStatus.PRESENT) {
                            existing.get().setStatus(AttendanceStatus.PRESENT);
                            attendanceRepository.save(existing.get());
                            attendanceModified = true;
                        }
                    }

                    for (int i = 0; i < absentDays; i++) {
                        LocalDate d = anchorDate.minusDays(i + 10);
                        Optional<Attendance> existing = attendanceRepository.findByStudentIdAndCourseIdAndDate(student.getId(), course.getId(), d);
                        if (existing.isEmpty()) {
                            attendanceRepository.save(Attendance.builder().student(student).course(course).date(d).status(AttendanceStatus.ABSENT).build());
                            attendanceModified = true;
                        } else if (existing.get().getStatus() != AttendanceStatus.ABSENT) {
                            existing.get().setStatus(AttendanceStatus.ABSENT);
                            attendanceRepository.save(existing.get());
                            attendanceModified = true;
                        }
                    }
                }

                if (markUnchanged && !attendanceModified) {
                    duplicate++;
                } else {
                    accepted++;
                }
            }
        }

        if (atomic && rejected > 0) {
            throw new IllegalArgumentException("Batch processing failed in atomic mode: " + rejected + " rows failed validation. Entire batch rolled back.");
        }

        String batchId = "batch-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime now = LocalDateTime.now();

        // Safe immutable audit log record without sensitive row data or passwords
        String auditDetails = String.format("batchId=%s; source=CSV_FACULTY_IMPORT; fingerprint=sha256:%s; total=%d; accepted=%d; duplicate=%d; rejected=%d; importedBy=%s",
                batchId, fingerprint, total, accepted, duplicate, rejected, (uploaderUserId != null ? uploaderUserId : "SYSTEM"));

        auditLogRepository.save(AuditLog.builder()
                .action("CSV_ACADEMIC_IMPORT")
                .userId(uploaderUserId)
                .details(auditDetails)
                .timestamp(now)
                .build());

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRecords", total);
        summary.put("acceptedRecords", accepted);
        summary.put("duplicateRecords", duplicate);
        summary.put("rejectedRecords", rejected);

        Map<String, Object> response = new HashMap<>();
        response.put("status", (rejected == 0) ? "SUCCESS" : (accepted > 0) ? "PARTIAL_SUCCESS" : "FAILED");
        response.put("batchId", batchId);
        response.put("fingerprint", "sha256:" + fingerprint);
        response.put("provenance", DataProvenance.CSV_FACULTY_IMPORT.name());
        response.put("importedBy", uploaderUserId != null ? uploaderUserId : "SYSTEM");
        response.put("timestamp", now);
        response.put("summary", summary);
        response.put("errors", errors);

        return response;
    }

    private LocalDate derivePeriodBaseDate(String period) {
        if (period == null || period.trim().isEmpty()) {
            return LocalDate.of(2026, 1, 15);
        }
        int year = 2026;
        Matcher m = Pattern.compile("(20\\d\\d)").matcher(period);
        if (m.find()) {
            year = Integer.parseInt(m.group(1));
        }
        int offset = Math.abs(period.trim().toLowerCase().hashCode() % 60);
        return LocalDate.of(year, 1, 1).plusDays(offset + 30);
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Map<String, Integer> parseHeaders(String[] rawHeaders) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < rawHeaders.length; i++) {
            String normalized = rawHeaders[i].trim().toLowerCase().replaceAll("[\\s_-]", "");
            map.put(normalized, i);
        }
        return map;
    }

    private void validateRequiredHeaders(Map<String, Integer> map) {
        List<String> missing = new ArrayList<>();
        if (getHeaderIndex(map, "studentid", "studentnumber", "student") == -1) missing.add("student_id/student_number");
        if (getHeaderIndex(map, "academicperiod", "period", "semester") == -1) missing.add("academic_period/period");
        if (getHeaderIndex(map, "coursecode", "course") == -1) missing.add("course_code/course");
        if (getHeaderIndex(map, "attendancepercentage", "attendancepct", "attendance") == -1) missing.add("attendance_percentage/attendance_pct");
        if (getHeaderIndex(map, "marksobtained", "marks", "mark") == -1) missing.add("marks_obtained");
        if (getHeaderIndex(map, "maxmarks", "max") == -1) missing.add("max_marks");

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Invalid CSV format. Missing required columns: " + String.join(", ", missing));
        }
    }

    private int getHeaderIndex(Map<String, Integer> map, String... aliases) {
        for (String alias : aliases) {
            if (map.containsKey(alias)) {
                return map.get(alias);
            }
        }
        return -1;
    }
}
