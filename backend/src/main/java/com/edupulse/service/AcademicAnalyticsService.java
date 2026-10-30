package com.edupulse.service;

import com.edupulse.dto.*;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
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
    private AssessmentRepository assessmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    @Autowired
    private InterventionRepository interventionRepository;

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TimetableService timetableService;

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

        // 1. Attendance Analysis across all 7 subjects
        List<Attendance> attendances = attendanceRepository.findByStudentId(studentId);
        long totalClasses = attendances.size();
        long presentClasses = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long absentClasses = totalClasses - presentClasses;
        boolean hasAttendanceData = totalClasses > 0;
        // Overall attendance calculated aggregate: total present / total conducted * 100
        Double overallAttendancePct = hasAttendanceData ? roundToTwoDecimal((double) presentClasses / totalClasses * 100.0) : null;

        Map<Course, List<Attendance>> byCourse = attendances.stream().collect(Collectors.groupingBy(Attendance::getCourse));
        List<CourseAttendanceDto> courseAttendanceBreakdown = new ArrayList<>();

        for (Map.Entry<Course, List<Attendance>> entry : byCourse.entrySet()) {
            Course c = entry.getKey();
            List<Attendance> courseAtt = entry.getValue();
            long cTotal = courseAtt.size();
            long cPresent = courseAtt.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            Double cPct = cTotal > 0 ? roundToTwoDecimal((double) cPresent / cTotal * 100.0) : null;
            String status = (cPct != null && cPct < ATTENDANCE_THRESHOLD) ? "BELOW_THRESHOLD" : "NORMAL";

            courseAttendanceBreakdown.add(CourseAttendanceDto.builder()
                    .courseId(c.getId())
                    .courseCode(c.getCode())
                    .courseName(c.getName())
                    .totalClasses(cTotal)
                    .presentClasses(cPresent)
                    .absentClasses(cTotal - cPresent)
                    .odMlClasses(0L)
                    .attendancePercentage(cPct)
                    .status(status)
                    .period("03-Aug-2026 to 29-Oct-2026")
                    .facultyName(TimetableService.SUBJECT_FACULTY.getOrDefault(c.getCode(), "Faculty"))
                    .classroom(TimetableService.SUBJECT_ROOMS.getOrDefault(c.getCode(), "Room"))
                    .build());
        }
        courseAttendanceBreakdown.sort(Comparator.comparing(CourseAttendanceDto::getCourseCode));

        // 2. Marks & Assessment Analysis (Completed vs. Pending)
        List<Mark> studentMarks = markRepository.findByStudentId(studentId);
        Map<Long, Mark> markByAssessmentId = studentMarks.stream()
                .filter(m -> m.getAssessment() != null)
                .collect(Collectors.toMap(m -> m.getAssessment().getId(), m -> m, (m1, m2) -> m1));

        List<Assessment> allAssessments = assessmentRepository.findAll();
        // If enrolled in specific courses, filter by student's courses; otherwise include all
        Set<Long> enrolledCourseIds = byCourse.keySet().stream().map(Course::getId).collect(Collectors.toSet());
        List<Assessment> relevantAssessments = allAssessments.stream()
                .filter(a -> a.getCourse() != null && (enrolledCourseIds.isEmpty() || enrolledCourseIds.contains(a.getCourse().getId())))
                .toList();

        List<AssessmentMarkDto> assessmentBreakdown = new ArrayList<>();
        double sumPercentages = 0.0;
        int evaluatedCount = 0;

        for (Assessment a : relevantAssessments) {
            Mark m = markByAssessmentId.get(a.getId());
            boolean hasEvaluatedScore = (m != null && m.getMarksObtained() != null);

            Double rawObtained = hasEvaluatedScore ? m.getMarksObtained() : null;
            Double maxMarks = a.getMaxMarks();
            Double weightage = a.getWeightage() != null ? a.getWeightage() : maxMarks;

            Double convertedMarks = null;
            Double percentage = null;
            String markStatus = hasEvaluatedScore ? "COMPLETED" : "PENDING";

            if (hasEvaluatedScore && maxMarks != null && maxMarks > 0) {
                percentage = roundToTwoDecimal((rawObtained / maxMarks) * 100.0);
                convertedMarks = roundToTwoDecimal((rawObtained / maxMarks) * weightage);
                sumPercentages += percentage;
                evaluatedCount++;
            }

            assessmentBreakdown.add(AssessmentMarkDto.builder()
                    .markId(m != null ? m.getId() : null)
                    .assessmentId(a.getId())
                    .courseCode(a.getCourse() != null ? a.getCourse().getCode() : "UNKNOWN")
                    .courseName(a.getCourse() != null ? a.getCourse().getName() : "Unknown Course")
                    .assessmentName(a.getName() != null ? a.getName() : "Assessment")
                    .assessmentType(a.getType() != null ? a.getType().name() : "INTERNAL")
                    .marksObtained(rawObtained)
                    .maxMarks(maxMarks)
                    .convertedMarks(convertedMarks)
                    .maxConvertedMarks(weightage)
                    .percentage(percentage)
                    .weightage(weightage)
                    .status(markStatus)
                    .facultyName(a.getCourse() != null ? TimetableService.SUBJECT_FACULTY.getOrDefault(a.getCourse().getCode(), "Faculty") : "Faculty")
                    .classroom(a.getCourse() != null ? TimetableService.SUBJECT_ROOMS.getOrDefault(a.getCourse().getCode(), "Room") : "Room")
                    .build());
        }

        assessmentBreakdown.sort(Comparator.comparing(AssessmentMarkDto::getCourseCode).thenComparing(AssessmentMarkDto::getAssessmentName));

        boolean hasMarksData = evaluatedCount > 0;
        Double overallAverageMarksPct = hasMarksData ? roundToOneDecimal(sumPercentages / evaluatedCount) : null;

        // 3. Coursework / Submissions Analysis
        List<AssignmentSubmission> submissions = submissionRepository.findByStudentId(studentId);
        int totalAssignments = submissions.size();
        int missingAssignments = (int) submissions.stream()
                .filter(s -> s.getSubmittedAt() == null || (s.getAssignment() != null && s.getSubmittedAt().isAfter(s.getAssignment().getDueDate())))
                .count();
        int submittedAssignments = totalAssignments - missingAssignments;
        boolean hasAssignmentData = totalAssignments > 0;

        // 4. Performance Trends
        List<PerformanceTrendPointDto> trendPoints = buildGenuineTrendPoints(attendances, studentMarks);
        boolean hasTrends = !trendPoints.isEmpty();

        // 5. Risk Assessment & Transparent Explanation
        riskAssessmentService.calculateRiskForStudent(studentId);
        RiskAssessmentResponse riskResponse = riskAssessmentService.getLatestRiskAssessment(studentId);
        RiskEngineExplanationDto riskExplanation = riskAssessmentService.getRiskExplanation(studentId);

        // 6. Next CLA / NTM Target Planner
        List<FutureTargetPlannerDto> targetPlanner = buildTargetPlanner(relevantAssessments, markByAssessmentId);

        // 7. Actionable Recommendations
        List<AcademicRecommendationDto> recommendations = generateRecommendations(
                hasAttendanceData, overallAttendancePct, totalClasses,
                hasMarksData, overallAverageMarksPct,
                hasAssignmentData, missingAssignments);

        // 8. Qualitative Insights
        List<String> strengths = new ArrayList<>();
        List<String> improvementAreas = new ArrayList<>();
        List<String> upcomingConcerns = new ArrayList<>();

        if (hasAttendanceData && overallAttendancePct != null) {
            if (overallAttendancePct >= HIGH_PERFORMANCE_THRESHOLD) {
                strengths.add(String.format(Locale.US, "Exemplary attendance of %.2f%% (%d of %d conducted classes attended).", overallAttendancePct, presentClasses, totalClasses));
            } else {
                improvementAreas.add(String.format(Locale.US, "Attendance (%.2f%%) is below the 75.0%% statutory threshold.", overallAttendancePct));
                upcomingConcerns.add("Risk of exam debarment in subjects with attendance shortfall.");
            }
        }

        if (hasMarksData && overallAverageMarksPct != null) {
            if (overallAverageMarksPct >= HIGH_PERFORMANCE_THRESHOLD) {
                strengths.add(String.format(Locale.US, "Solid evaluated internal average of %.1f%% across conducted assessments.", overallAverageMarksPct));
            } else if (overallAverageMarksPct < PASSING_MARKS_THRESHOLD) {
                improvementAreas.add(String.format(Locale.US, "Evaluated score of %.1f%% is below passing threshold (50.0%%).", overallAverageMarksPct));
                upcomingConcerns.add("Immediate catch-up needed in CLA 2 to avoid internal marks deficiency.");
            } else {
                strengths.add(String.format(Locale.US, "Passing standing (%.1f%%) maintained across graded evaluations.", overallAverageMarksPct));
            }
        }

        if (hasAssignmentData && missingAssignments > 0) {
            upcomingConcerns.add(String.format("%d assignment(s) are overdue or missing submission.", missingAssignments));
        }

        // 9. Provenance & Freshness
        String studentName = student.getUser() != null ? student.getUser().getName() : "Student";
        String provenance;
        if (overrideProvenance != null) {
            provenance = overrideProvenance.name();
        } else if ("STU-1001".equals(student.getStudentNumber()) 
                || studentName.contains("Student A") 
                || studentName.contains("Reference Profile")
                || (student.getUser() != null && ("demo.student.a".equalsIgnoreCase(student.getUser().getUsername()) || "student_a".equalsIgnoreCase(student.getUser().getUsername())))) {
            provenance = "STUDENT-SUPPLIED REFERENCE DATA";
        } else {
            provenance = "SYNTHETIC DEMO DATA";
        }

        LocalDateTime lastActivity = determineLastActivityDate(attendances, studentMarks, submissions);
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
                .name(studentName)
                .department(student.getDepartment())
                .currentSemester(student.getCurrentSemester())
                .currentCgpa(student.getCurrentCgpa())
                .provenance(provenance)
                .dataStatus(dataStatus)
                .lastActivityDate(lastActivity)
                .isStale(isStale)
                .attendancePeriod("03-Aug-2026 to 29-Oct-2026")
                .lastUpdated("29-Oct-2026")
                .disclaimer("Heuristic decision-support analysis based strictly on authorized, recorded data. Not an official institutional decision.")
                .hasAttendanceData(hasAttendanceData)
                .totalClasses(totalClasses)
                .presentClasses(presentClasses)
                .absentClasses(absentClasses)
                .overallAttendancePercentage(overallAttendancePct)
                .courseAttendanceBreakdown(courseAttendanceBreakdown)
                .hasMarksData(hasMarksData)
                .overallAverageMarksPercentage(overallAverageMarksPct)
                .totalAssessments(evaluatedCount)
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
                .targetPlanner(targetPlanner)
                .riskEngineExplanation(riskExplanation)
                .timetable(timetableService.getCommonTimetable())
                .build();
    }

    private List<FutureTargetPlannerDto> buildTargetPlanner(List<Assessment> assessments, Map<Long, Mark> markMap) {
        Map<Course, List<Assessment>> byCourse = assessments.stream()
                .filter(a -> a.getCourse() != null)
                .collect(Collectors.groupingBy(Assessment::getCourse));

        List<FutureTargetPlannerDto> planners = new ArrayList<>();

        for (Map.Entry<Course, List<Assessment>> entry : byCourse.entrySet()) {
            Course course = entry.getKey();
            List<Assessment> courseAssessments = entry.getValue();

            double currentObtainedConverted = 0.0;
            double currentEvaluatedMaxConverted = 0.0;
            List<String> pendingNames = new ArrayList<>();
            Double nextPendingMax = null;
            String nextPendingName = null;

            for (Assessment a : courseAssessments) {
                Mark m = markMap.get(a.getId());
                double weight = a.getWeightage() != null ? a.getWeightage() : a.getMaxMarks();

                if (m != null && m.getMarksObtained() != null && a.getMaxMarks() > 0) {
                    currentObtainedConverted += (m.getMarksObtained() / a.getMaxMarks()) * weight;
                    currentEvaluatedMaxConverted += weight;
                } else {
                    pendingNames.add(a.getName());
                    if (nextPendingName == null) {
                        nextPendingName = a.getName();
                        nextPendingMax = a.getMaxMarks();
                    }
                }
            }

            double totalSubjectMax = course.getCode().equals("SEC 203") ? 40.0 : 50.0;
            double nextMax = nextPendingMax != null ? nextPendingMax : 10.0;
            String pendingName = nextPendingName != null ? nextPendingName : "Next CLA";

            // Minimum scenario: Target >= 50%
            double minRequired = Math.max(0.0, Math.min(nextMax, nextMax * 0.50));
            double minExpectedTotal = currentObtainedConverted + minRequired;

            // Recommended scenario: Target >= 75%
            double recRequired = Math.max(0.0, Math.min(nextMax, nextMax * 0.75));
            double recExpectedTotal = currentObtainedConverted + recRequired;

            // Stretch scenario: Target >= 90%
            double stretchRequired = Math.max(0.0, Math.min(nextMax, nextMax * 0.95));
            double stretchExpectedTotal = currentObtainedConverted + stretchRequired;

            planners.add(FutureTargetPlannerDto.builder()
                    .courseCode(course.getCode())
                    .courseName(course.getName())
                    .currentObtainedMarks(roundToTwoDecimal(currentObtainedConverted))
                    .currentEvaluatedMaxMarks(roundToTwoDecimal(currentEvaluatedMaxConverted))
                    .currentConvertedMarks(roundToTwoDecimal(currentObtainedConverted))
                    .totalSubjectMaxMarks(totalSubjectMax)
                    .pendingAssessmentName(pendingName)
                    .pendingAssessmentMaxMarks(nextMax)
                    .remainingAssessmentsCount(pendingNames.size())
                    .remainingAssessmentsList(String.join(", ", pendingNames))
                    .minimumTarget(FutureTargetPlannerDto.TargetScenario.builder()
                            .scenarioName("Minimum Target (Passing)")
                            .targetScore(roundToTwoDecimal(minRequired))
                            .expectedConvertedTotal(roundToTwoDecimal(minExpectedTotal))
                            .projectedPercentage(roundToTwoDecimal((minExpectedTotal / totalSubjectMax) * 100.0))
                            .projectedRiskLevel("LOW")
                            .rationale("Secures minimum required threshold without risking penalty.")
                            .build())
                    .recommendedTarget(FutureTargetPlannerDto.TargetScenario.builder()
                            .scenarioName("Recommended Target (Good Standing)")
                            .targetScore(roundToTwoDecimal(recRequired))
                            .expectedConvertedTotal(roundToTwoDecimal(recExpectedTotal))
                            .projectedPercentage(roundToTwoDecimal((recExpectedTotal / totalSubjectMax) * 100.0))
                            .projectedRiskLevel("LOW")
                            .rationale("Builds strong academic buffer for end-semester examinations.")
                            .build())
                    .stretchTarget(FutureTargetPlannerDto.TargetScenario.builder()
                            .scenarioName("Stretch Target (Distinction)")
                            .targetScore(roundToTwoDecimal(stretchRequired))
                            .expectedConvertedTotal(roundToTwoDecimal(stretchExpectedTotal))
                            .projectedPercentage(roundToTwoDecimal((stretchExpectedTotal / totalSubjectMax) * 100.0))
                            .projectedRiskLevel("LOW")
                            .rationale("Places student in top cohort percentile for departmental honors.")
                            .build())
                    .riskImpactDescription("Scoring at or above the recommended target keeps subject risk at zero.")
                    .verifiedWeightage(true)
                    .projectionDisclaimer("Simulated projections only; does not alter actual institutional records.")
                    .build());
        }

        planners.sort(Comparator.comparing(FutureTargetPlannerDto::getCourseCode));
        return planners;
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

            List<Mark> evaluatedMarks = marks.stream()
                    .filter(m -> m.getAssessment() != null && m.getAssessment().getMaxMarks() > 0 && m.getMarksObtained() != null)
                    .toList();

            if (!evaluatedMarks.isEmpty()) {
                double avgM = evaluatedMarks.stream()
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
        Map<String, List<Attendance>> attByPeriod = new TreeMap<>();
        for (Attendance a : attendances) {
            if (a.getDate() != null) {
                String period = a.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                attByPeriod.computeIfAbsent(period, k -> new ArrayList<>()).add(a);
            }
        }

        List<PerformanceTrendPointDto> points = new ArrayList<>();
        int monthIdx = 1;

        for (Map.Entry<String, List<Attendance>> entry : attByPeriod.entrySet()) {
            String period = entry.getKey();
            List<Attendance> pAtt = entry.getValue();
            Double attPct = null;
            if (!pAtt.isEmpty()) {
                long pres = pAtt.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
                attPct = roundToOneDecimal((double) pres / pAtt.size() * 100.0);
            }

            Double marksPct = null;
            int markCount = 0;
            List<Mark> evaluatedMarks = marks.stream()
                    .filter(m -> m.getAssessment() != null && m.getAssessment().getMaxMarks() > 0 && m.getMarksObtained() != null)
                    .toList();

            if (!evaluatedMarks.isEmpty()) {
                double sum = 0.0;
                for (Mark m : evaluatedMarks) {
                    sum += (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0;
                    markCount++;
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

    private Double roundToTwoDecimal(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
