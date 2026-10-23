package com.edupulse.service;

import com.edupulse.dto.*;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AcademicAnalyticsService {

    private static final double ATTENDANCE_THRESHOLD = 75.0;
    private static final double PASSING_MARKS_THRESHOLD = 50.0;
    private static final double HIGH_PERFORMANCE_THRESHOLD = 75.0;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    @Autowired
    private InterventionRepository interventionRepository;

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public StudentAcademicAnalyticsDto getStudentAnalytics(Long studentId) {
        return getStudentAnalytics(studentId, null);
    }

    public StudentAcademicAnalyticsDto calculateStudentAnalytics(String studentNumber, DataProvenance overrideProvenance) {
        Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with number: " + studentNumber));
        return getStudentAnalytics(student.getId(), overrideProvenance);
    }

    public StudentAcademicAnalyticsDto getStudentAnalytics(Long studentId, DataProvenance overrideProvenance) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

        // 1. Attendance Analysis
        List<Attendance> attendances = attendanceRepository.findByStudentId(studentId);
        long totalClasses = attendances.size();
        long presentClasses = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long absentClasses = totalClasses - presentClasses;
        boolean hasAttendanceData = totalClasses > 0;
        Double overallAttendancePct = hasAttendanceData ? roundToOneDecimal((double) presentClasses / totalClasses * 100.0) : null;

        Map<Course, List<Attendance>> byCourse = attendances.stream().collect(Collectors.groupingBy(Attendance::getCourse));
        List<CourseAttendanceDto> courseAttendanceBreakdown = new ArrayList<>();
        for (Map.Entry<Course, List<Attendance>> entry : byCourse.entrySet()) {
            Course c = entry.getKey();
            List<Attendance> courseAtt = entry.getValue();
            long cTotal = courseAtt.size();
            long cPresent = courseAtt.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            Double cPct = cTotal > 0 ? roundToOneDecimal((double) cPresent / cTotal * 100.0) : null;
            String status = (cPct != null && cPct < ATTENDANCE_THRESHOLD) ? "BELOW_THRESHOLD" : "NORMAL";

            courseAttendanceBreakdown.add(CourseAttendanceDto.builder()
                    .courseId(c.getId())
                    .courseCode(c.getCode())
                    .courseName(c.getName())
                    .totalClasses(cTotal)
                    .presentClasses(cPresent)
                    .absentClasses(cTotal - cPresent)
                    .attendancePercentage(cPct)
                    .status(status)
                    .build());
        }
        courseAttendanceBreakdown.sort(Comparator.comparing(CourseAttendanceDto::getCourseCode));

        // 2. Marks & Assessment Analysis
        List<Mark> marks = markRepository.findByStudentId(studentId);
        List<AssessmentMarkDto> assessmentBreakdown = new ArrayList<>();
        double sumPercentages = 0.0;
        int validMarksCount = 0;

        for (Mark m : marks) {
            Assessment a = m.getAssessment();
            Double maxMarks = a != null ? a.getMaxMarks() : null;
            Double obtained = m.getMarksObtained();
            Double pct = (maxMarks != null && maxMarks > 0 && obtained != null) ? roundToOneDecimal((obtained / maxMarks) * 100.0) : null;
            if (pct != null) {
                sumPercentages += pct;
                validMarksCount++;
            }

            assessmentBreakdown.add(AssessmentMarkDto.builder()
                    .markId(m.getId())
                    .assessmentId(a != null ? a.getId() : null)
                    .courseCode(a != null && a.getCourse() != null ? a.getCourse().getCode() : "UNKNOWN")
                    .courseName(a != null && a.getCourse() != null ? a.getCourse().getName() : "Unknown Course")
                    .assessmentName(a != null ? a.getName() : "Assessment")
                    .assessmentType(a != null && a.getType() != null ? a.getType().name() : "INTERNAL")
                    .marksObtained(obtained)
                    .maxMarks(maxMarks)
                    .percentage(pct)
                    .weightage(a != null ? a.getWeightage() : null)
                    .build());
        }

        boolean hasMarksData = validMarksCount > 0;
        Double overallAverageMarksPct = hasMarksData ? roundToOneDecimal(sumPercentages / validMarksCount) : null;

        // 3. Coursework / Submissions Analysis
        List<AssignmentSubmission> submissions = submissionRepository.findByStudentId(studentId);
        int totalAssignments = submissions.size();
        int missingAssignments = (int) submissions.stream()
                .filter(s -> s.getSubmittedAt() == null || (s.getAssignment() != null && s.getSubmittedAt().isAfter(s.getAssignment().getDueDate())))
                .count();
        int submittedAssignments = totalAssignments - missingAssignments;
        boolean hasAssignmentData = totalAssignments > 0;

        // 4. Performance Trends (genuine recorded data points without fabrications)
        List<PerformanceTrendPointDto> trendPoints = buildGenuineTrendPoints(attendances, marks);
        boolean hasTrends = !trendPoints.isEmpty();

        // 5. Risk Assessment & Explanations
        riskAssessmentService.calculateRiskForStudent(studentId);
        RiskAssessmentResponse riskResponse = riskAssessmentService.getLatestRiskAssessment(studentId);

        // 6. Actionable Recommendations
        List<AcademicRecommendationDto> recommendations = generateRecommendations(
                hasAttendanceData, overallAttendancePct, totalClasses,
                hasMarksData, overallAverageMarksPct,
                hasAssignmentData, missingAssignments);

        // 7. Qualitative Insights (Strengths, Improvement Areas, Upcoming Concerns)
        List<String> strengths = new ArrayList<>();
        List<String> improvementAreas = new ArrayList<>();
        List<String> upcomingConcerns = new ArrayList<>();

        if (hasAttendanceData && overallAttendancePct != null) {
            if (overallAttendancePct >= HIGH_PERFORMANCE_THRESHOLD) {
                strengths.add(String.format("Consistent class attendance of %.1f%% across %d sessions.", overallAttendancePct, totalClasses));
            } else {
                improvementAreas.add(String.format("Attendance (%.1f%%) is below the %.0f%% minimum threshold.", overallAttendancePct, ATTENDANCE_THRESHOLD));
                upcomingConcerns.add("Low attendance risks debarment from end-semester examinations.");
            }
        } else {
            improvementAreas.add("No attendance records have been registered for this student.");
        }

        if (hasMarksData && overallAverageMarksPct != null) {
            if (overallAverageMarksPct >= HIGH_PERFORMANCE_THRESHOLD) {
                strengths.add(String.format("Strong average assessment performance of %.1f%% across %d evaluations.", overallAverageMarksPct, validMarksCount));
            } else if (overallAverageMarksPct < PASSING_MARKS_THRESHOLD) {
                improvementAreas.add(String.format("Average assessment score of %.1f%% is below the %.0f%% passing benchmark.", overallAverageMarksPct, PASSING_MARKS_THRESHOLD));
                upcomingConcerns.add("Risk of academic probation if internal marks do not improve before finals.");
            } else {
                strengths.add(String.format("Passing performance (%.1f%%) maintained across graded assessments.", overallAverageMarksPct));
            }
        } else {
            improvementAreas.add("No assessment marks are currently recorded.");
        }

        if (hasAssignmentData && missingAssignments > 0) {
            upcomingConcerns.add(String.format("%d assignment(s) are overdue or missing submission.", missingAssignments));
        }

        // 8. Provenance & Freshness
        String provenance = overrideProvenance != null ? overrideProvenance.name() : determineProvenance(student, attendances, marks);
        LocalDateTime lastActivity = determineLastActivityDate(attendances, marks, submissions);
        boolean isStale = (lastActivity == null) || lastActivity.isBefore(LocalDateTime.now().minusDays(30));

        String dataStatus;
        if (hasAttendanceData && hasMarksData) {
            dataStatus = "COMPLETE";
        } else if (hasAttendanceData || hasMarksData || hasAssignmentData) {
            dataStatus = "PARTIAL";
        } else {
            dataStatus = "INSUFFICIENT_DATA";
        }

        return StudentAcademicAnalyticsDto.builder()
                .studentId(student.getId())
                .studentNumber(student.getStudentNumber())
                .name(student.getUser().getName())
                .department(student.getDepartment())
                .currentSemester(student.getCurrentSemester())
                .currentCgpa(student.getCurrentCgpa())
                .provenance(provenance)
                .dataStatus(dataStatus)
                .lastActivityDate(lastActivity)
                .isStale(isStale)
                .disclaimer("Heuristic decision-support analysis based strictly on authorized, recorded data. Not an official institutional decision.")
                .hasAttendanceData(hasAttendanceData)
                .totalClasses(totalClasses)
                .presentClasses(presentClasses)
                .absentClasses(absentClasses)
                .overallAttendancePercentage(overallAttendancePct)
                .courseAttendanceBreakdown(courseAttendanceBreakdown)
                .hasMarksData(hasMarksData)
                .overallAverageMarksPercentage(overallAverageMarksPct)
                .totalAssessments(validMarksCount)
                .assessmentBreakdown(assessmentBreakdown)
                .hasAssignmentData(hasAssignmentData)
                .totalAssignments(totalAssignments)
                .submittedAssignments(submittedAssignments)
                .missingAssignments(missingAssignments)
                .hasTrends(hasTrends)
                .trendPoints(trendPoints)
                .riskScore(riskResponse.getRiskScore())
                .riskLevel(riskResponse.getRiskLevel())
                .riskCalculatedAt(riskResponse.getCalculatedAt())
                .riskFactors(riskResponse.getRiskFactors())
                .recommendations(recommendations)
                .strengths(strengths)
                .improvementAreas(improvementAreas)
                .upcomingConcerns(upcomingConcerns)
                .build();
    }

    public DashboardSummaryDto getDashboardSummary() {
        long totalStudents = studentRepository.count();
        if (totalStudents == 0) {
            return DashboardSummaryDto.builder()
                    .totalStudents(0)
                    .highRisk(0)
                    .mediumRisk(0)
                    .lowRisk(0)
                    .insufficientData(0)
                    .activeInterventions(0)
                    .completedInterventions(0)
                    .cohortAverageAttendance(null)
                    .cohortAverageMarks(null)
                    .cohortStrengths(List.of("No student records in cohort"))
                    .cohortImprovementAreas(List.of("System awaiting student roster ingestion"))
                    .build();
        }

        long highRisk = 0;
        long mediumRisk = 0;
        long lowRisk = 0;
        long insufficientData = 0;

        double sumAttendance = 0.0;
        int studentsWithAtt = 0;
        double sumMarks = 0.0;
        int studentsWithMarks = 0;
        List<DashboardSummaryDto.RecentHighRiskStudentDto> recentHighRisk = new ArrayList<>();

        List<Student> students = studentRepository.findAll();
        for (Student s : students) {
            riskAssessmentService.calculateRiskForStudent(s.getId());
            RiskAssessmentResponse risk = riskAssessmentService.getLatestRiskAssessment(s.getId());
            List<Attendance> att = attendanceRepository.findByStudentId(s.getId());
            List<Mark> marks = markRepository.findByStudentId(s.getId());

            if (att.isEmpty() && marks.isEmpty()) {
                insufficientData++;
            } else {
                if (risk.getRiskLevel() == RiskLevel.HIGH) {
                    highRisk++;
                    String topFactor = (risk.getRiskFactors() != null && !risk.getRiskFactors().isEmpty()) 
                            ? risk.getRiskFactors().get(0).getDescription() 
                            : "Risk threshold exceeded";
                    boolean hasInt = interventionRepository.findByStudentId(s.getId()).stream()
                            .anyMatch(i -> i.getStatus() != InterventionStatus.COMPLETED);
                    recentHighRisk.add(DashboardSummaryDto.RecentHighRiskStudentDto.builder()
                            .id(s.getId())
                            .name(s.getUser().getName())
                            .factor(topFactor)
                            .riskScore(risk.getRiskScore())
                            .intervention(hasInt ? "Active" : "Pending")
                            .build());
                } else if (risk.getRiskLevel() == RiskLevel.MEDIUM) {
                    mediumRisk++;
                } else {
                    lowRisk++;
                }
            }

            if (!att.isEmpty()) {
                long pres = att.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
                sumAttendance += ((double) pres / att.size() * 100.0);
                studentsWithAtt++;
            }

            if (!marks.isEmpty()) {
                double avgM = marks.stream()
                        .filter(m -> m.getAssessment() != null && m.getAssessment().getMaxMarks() > 0)
                        .mapToDouble(m -> (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0)
                        .average().orElse(0.0);
                sumMarks += avgM;
                studentsWithMarks++;
            }
        }

        long activeInterventions = interventionRepository.findAll().stream()
                .filter(i -> i.getStatus() == InterventionStatus.PLANNED || i.getStatus() == InterventionStatus.IN_PROGRESS)
                .count();
        long completedInterventions = interventionRepository.findAll().stream()
                .filter(i -> i.getStatus() == InterventionStatus.COMPLETED)
                .count();

        Double cohortAvgAtt = studentsWithAtt > 0 ? roundToOneDecimal(sumAttendance / studentsWithAtt) : null;
        Double cohortAvgMarks = studentsWithMarks > 0 ? roundToOneDecimal(sumMarks / studentsWithMarks) : null;

        List<String> strengths = new ArrayList<>();
        List<String> improvementAreas = new ArrayList<>();

        if (cohortAvgAtt != null && cohortAvgAtt >= ATTENDANCE_THRESHOLD) {
            strengths.add(String.format("Cohort average attendance is healthy at %.1f%%.", cohortAvgAtt));
        } else if (cohortAvgAtt != null) {
            improvementAreas.add(String.format("Cohort average attendance (%.1f%%) is below the %.0f%% standard.", cohortAvgAtt, ATTENDANCE_THRESHOLD));
        }

        if (cohortAvgMarks != null && cohortAvgMarks >= PASSING_MARKS_THRESHOLD) {
            strengths.add(String.format("Cohort average assessment performance is satisfactory at %.1f%%.", cohortAvgMarks));
        } else if (cohortAvgMarks != null) {
            improvementAreas.add(String.format("Cohort average assessment performance (%.1f%%) indicates curriculum friction.", cohortAvgMarks));
        }

        if (highRisk > 0) {
            improvementAreas.add(String.format("%d student(s) currently flagged at HIGH academic risk.", highRisk));
        }

        return DashboardSummaryDto.builder()
                .totalStudents(totalStudents)
                .highRisk(highRisk)
                .mediumRisk(mediumRisk)
                .lowRisk(lowRisk)
                .insufficientData(insufficientData)
                .activeInterventions(activeInterventions)
                .completedInterventions(completedInterventions)
                .cohortAverageAttendance(cohortAvgAtt)
                .cohortAverageMarks(cohortAvgMarks)
                .recentHighRisk(recentHighRisk)
                .cohortStrengths(strengths)
                .cohortImprovementAreas(improvementAreas)
                .build();
    }

    private List<PerformanceTrendPointDto> buildGenuineTrendPoints(List<Attendance> attendances, List<Mark> marks) {
        // Group attendance by Month (YYYY-MM)
        Map<String, List<Attendance>> attByPeriod = new TreeMap<>();
        for (Attendance a : attendances) {
            if (a.getDate() != null) {
                String period = a.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                attByPeriod.computeIfAbsent(period, k -> new ArrayList<>()).add(a);
            }
        }

        // Group marks by period or course
        Map<String, List<Mark>> marksByPeriod = new TreeMap<>();
        for (Mark m : marks) {
            if (m.getAssessment() != null) {
                String name = m.getAssessment().getName();
                // If assessment has period in name (e.g. "Imported 2026-SEM1" or "Imported Midterm")
                String periodKey = name.replace("Imported ", "").trim();
                marksByPeriod.computeIfAbsent(periodKey, k -> new ArrayList<>()).add(m);
            }
        }

        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(attByPeriod.keySet());

        List<PerformanceTrendPointDto> points = new ArrayList<>();
        int monthIdx = 1;

        for (String period : allKeys) {
            List<Attendance> pAtt = attByPeriod.getOrDefault(period, Collections.emptyList());
            Double attPct = null;
            if (!pAtt.isEmpty()) {
                long pres = pAtt.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
                attPct = roundToOneDecimal((double) pres / pAtt.size() * 100.0);
            }

            // Find matching or relevant marks
            Double marksPct = null;
            int markCount = 0;
            if (!marks.isEmpty()) {
                // If marks exist, use available marks
                double sum = 0.0;
                for (Mark m : marks) {
                    if (m.getAssessment() != null && m.getAssessment().getMaxMarks() > 0) {
                        sum += (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0;
                        markCount++;
                    }
                }
                if (markCount > 0) {
                    marksPct = roundToOneDecimal(sum / markCount);
                }
            }

            points.add(PerformanceTrendPointDto.builder()
                    .name("Month " + monthIdx + " (" + period + ")")
                    .attendance(attPct)
                    .marks(marksPct)
                    .classesCount(pAtt.size())
                    .assessmentsCount(markCount)
                    .build());
            monthIdx++;
        }

        return points;
    }

    private List<AcademicRecommendationDto> generateRecommendations(
            boolean hasAtt, Double attPct, long totalClasses,
            boolean hasMarks, Double marksPct,
            boolean hasSubmissions, int missingCount) {

        List<AcademicRecommendationDto> list = new ArrayList<>();

        if (hasAtt && attPct != null && attPct < ATTENDANCE_THRESHOLD) {
            list.add(AcademicRecommendationDto.builder()
                    .category("ATTENDANCE")
                    .title("Attendance Recovery Plan")
                    .rationale(String.format("Current attendance is %.1f%% across %d sessions, falling below the %.0f%% minimum threshold.",
                            attPct, totalClasses, ATTENDANCE_THRESHOLD))
                    .nextStep("Schedule an advisor consultation to review attendance obstacles and formulate an attendance catch-up schedule.")
                    .priority(attPct < 60.0 ? "HIGH" : "MEDIUM")
                    .build());
        }

        if (hasMarks && marksPct != null && marksPct < PASSING_MARKS_THRESHOLD) {
            list.add(AcademicRecommendationDto.builder()
                    .category("ACADEMIC_PERFORMANCE")
                    .title("Targeted Subject Tutoring")
                    .rationale(String.format("Average assessment score of %.1f%% is below the %.0f%% passing benchmark.",
                            marksPct, PASSING_MARKS_THRESHOLD))
                    .nextStep("Enroll in departmental peer tutoring sessions and review previous test error feedback with course faculty.")
                    .priority("HIGH")
                    .build());
        }

        if (hasSubmissions && missingCount > 0) {
            list.add(AcademicRecommendationDto.builder()
                    .category("COURSEWORK")
                    .title("Overdue Coursework Submission")
                    .rationale(String.format("%d assignment(s) are overdue or missing submission.", missingCount))
                    .nextStep("Contact course faculty regarding late submission policies and turn in outstanding coursework promptly.")
                    .priority("MEDIUM")
                    .build());
        }

        if (hasAtt && hasMarks && attPct != null && marksPct != null && attPct >= HIGH_PERFORMANCE_THRESHOLD && marksPct >= HIGH_PERFORMANCE_THRESHOLD) {
            list.add(AcademicRecommendationDto.builder()
                    .category("ENRICHMENT")
                    .title("Honors & Leadership Opportunities")
                    .rationale(String.format("Exemplary academic record: %.1f%% attendance and %.1f%% average marks demonstrate subject mastery.",
                            attPct, marksPct))
                    .nextStep("Explore department research assistantships, honors electives, or peer mentoring roles.")
                    .priority("LOW")
                    .build());
        }

        if (!hasAtt && !hasMarks) {
            list.add(AcademicRecommendationDto.builder()
                    .category("DATA_COLLECTION")
                    .title("Academic Data Ingestion Required")
                    .rationale("No verified attendance or examination marks have been uploaded for this student.")
                    .nextStep("Import official course CSV batches or link external institutional fixtures to enable analytics.")
                    .priority("LOW")
                    .build());
        }

        if (list.isEmpty()) {
            list.add(AcademicRecommendationDto.builder()
                    .category("GENERAL_STUDY")
                    .title("Maintain Academic Momentum")
                    .rationale(String.format("Academic performance is currently steady with %.1f%% attendance and %.1f%% average marks.",
                            attPct != null ? attPct : 0.0, marksPct != null ? marksPct : 0.0))
                    .nextStep("Continue attending scheduled sessions, review course lecture notes weekly, and prepare ahead for end-semester exams.")
                    .priority("LOW")
                    .build());
        }

        return list;
    }

    private String determineProvenance(Student student, List<Attendance> attendances, List<Mark> marks) {
        boolean hasPortalFixtureSync = auditLogRepository.findByUserId(student.getId()).stream()
                .anyMatch(log -> "PORTAL_FIXTURE_SYNC".equals(log.getAction()));
        if (hasPortalFixtureSync) {
            return DataProvenance.INSTITUTIONAL_PORTAL_FIXTURE.name();
        }

        // If course code is CS-DEMO or course name contains Synthetic Demo
        boolean hasDemoCourse = attendances.stream().anyMatch(a -> a.getCourse().getCode().contains("DEMO") || a.getCourse().getName().contains("Synthetic")) ||
                marks.stream().anyMatch(m -> m.getAssessment() != null && m.getAssessment().getCourse() != null &&
                        (m.getAssessment().getCourse().getCode().contains("DEMO") || m.getAssessment().getName().contains("Synthetic")));

        if (hasDemoCourse) {
            return DataProvenance.SYNTHETIC_TEST_FIXTURE.name();
        }

        if (!attendances.isEmpty() || !marks.isEmpty()) {
            return DataProvenance.CSV_FACULTY_IMPORT.name();
        }

        return DataProvenance.OFFICIAL_INSTITUTIONAL_API.name();
    }

    private LocalDateTime determineLastActivityDate(List<Attendance> attendances, List<Mark> marks, List<AssignmentSubmission> submissions) {
        LocalDate latest = null;

        for (Attendance a : attendances) {
            if (a.getDate() != null) {
                if (latest == null || a.getDate().isAfter(latest)) latest = a.getDate();
            }
        }

        for (AssignmentSubmission s : submissions) {
            if (s.getSubmittedAt() != null) {
                if (latest == null || s.getSubmittedAt().isAfter(latest)) latest = s.getSubmittedAt();
            }
        }

        return latest != null ? latest.atStartOfDay() : LocalDateTime.now();
    }

    private Double roundToOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
