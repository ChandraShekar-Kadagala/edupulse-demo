package com.edupulse.service;

import com.edupulse.dto.*;
import com.edupulse.exception.ResourceNotFoundException;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.service.risk.RiskFormula;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Faculty profiles and course dashboards built only from stored data:
 * course_assignments (V2) decide which courses a faculty member teaches, enrollments and recorded
 * attendance/marks decide the roster. Nothing is guessed from usernames and nothing is invented —
 * missing values are returned as null so the UI can show an explicit "no data" state.
 */
@Service
@RequiredArgsConstructor
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final InterventionRepository interventionRepository;

    @Transactional(readOnly = true)
    public List<FacultyDto> getAllFacultyProfiles() {
        return facultyRepository.findAll().stream()
                .sorted(Comparator.comparing(f -> f.getUser() != null ? f.getUser().getUsername() : ""))
                .map(this::mapFacultyToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public FacultyDto getFacultyById(Long id) {
        return mapFacultyToDto(requireFaculty(id));
    }

    private Faculty requireFaculty(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty profile", id));
    }

    /** Course roster: enrolled students plus any student with recorded attendance in the course. */
    List<Student> rosterFor(Course course) {
        Map<Long, Student> roster = new LinkedHashMap<>();
        enrollmentRepository.findByCourseId(course.getId()).stream()
                .map(Enrollment::getStudent).filter(Objects::nonNull)
                .forEach(s -> roster.putIfAbsent(s.getId(), s));
        attendanceRepository.findByCourseId(course.getId()).stream()
                .map(Attendance::getStudent).filter(Objects::nonNull)
                .forEach(s -> roster.putIfAbsent(s.getId(), s));
        return roster.values().stream()
                .sorted(Comparator.comparing(Student::getStudentNumber))
                .toList();
    }

    /**
     * Dashboard for one of the faculty member's assigned courses.
     *
     * @param courseCode optional; must be one of the faculty's assigned courses. Defaults to the first assignment.
     */
    @Transactional
    public FacultyDashboardDto getFacultyDashboard(Long facultyUserId, String courseCode) {
        Faculty faculty = requireFaculty(facultyUserId);
        FacultyDto facultyDto = mapFacultyToDto(faculty);
        List<CourseAssignment> assignments = courseAssignmentRepository.findByFacultyIdWithCourse(faculty.getId());

        if (assignments.isEmpty()) {
            // Honest empty state: the faculty exists but teaches no assigned course yet.
            return FacultyDashboardDto.builder()
                    .faculty(facultyDto)
                    .students(List.of())
                    .assessments(List.of())
                    .activeInterventions(List.of())
                    .riskEngineSummary(riskEngineSummary())
                    .build();
        }

        Course course = assignments.stream()
                .map(CourseAssignment::getCourse)
                .filter(c -> courseCode == null || c.getCode().equalsIgnoreCase(courseCode))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Course " + courseCode + " is not assigned to this faculty member"));

        List<Student> roster = rosterFor(course);
        List<Assessment> courseAssessments = assessmentRepository.findByCourseId(course.getId()).stream()
                .sorted(Comparator.comparing(Assessment::getId)).toList();
        List<Attendance> courseAttendance = attendanceRepository.findByCourseId(course.getId());
        long conductedSessions = courseAttendance.stream().map(Attendance::getDate).filter(Objects::nonNull).distinct().count();

        List<FacultyDashboardDto.StudentCoursePerformanceDto> records = new ArrayList<>();
        double sumAtt = 0; int nAtt = 0; double sumMarks = 0; int nMarks = 0;
        int high = 0, medium = 0, low = 0;

        for (Student s : roster) {
            List<Attendance> att = courseAttendance.stream()
                    .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(s.getId())).toList();
            long total = att.size();
            long present = att.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            Double attPct = total > 0 ? RiskFormula.round1((double) present / total * 100.0) : null;
            if (attPct != null) { sumAtt += attPct; nAtt++; }

            List<Attendance> allAtt = attendanceRepository.findByStudentId(s.getId());
            Double overallAtt = allAtt.isEmpty() ? null : RiskFormula.round1(
                    (double) allAtt.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count() / allAtt.size() * 100.0);

            List<Mark> studentMarks = markRepository.findByStudentId(s.getId());
            List<AssessmentMarkDto> markDtos = new ArrayList<>();
            double pctSum = 0; int evaluated = 0;
            for (Assessment a : courseAssessments) {
                Mark m = studentMarks.stream()
                        .filter(mk -> mk.getAssessment() != null && mk.getAssessment().getId().equals(a.getId()))
                        .findFirst().orElse(null);
                Double obtained = m != null ? m.getMarksObtained() : null;
                boolean gradable = obtained != null && a.getMaxMarks() != null && a.getMaxMarks() > 0;
                Double pct = gradable ? RiskFormula.round1(obtained / a.getMaxMarks() * 100.0) : null;
                if (pct != null) { pctSum += pct; evaluated++; }
                markDtos.add(AssessmentMarkDto.builder()
                        .assessmentId(a.getId())
                        .assessmentName(a.getName())
                        .assessmentType(a.getType() != null ? a.getType().name() : null)
                        .courseCode(course.getCode())
                        .courseName(course.getName())
                        .marksObtained(obtained)
                        .maxMarks(a.getMaxMarks())
                        .weightage(a.getWeightage())
                        .convertedMarks(gradable && a.getWeightage() != null
                                ? Math.round(obtained / a.getMaxMarks() * a.getWeightage() * 100.0) / 100.0 : null)
                        .maxConvertedMarks(a.getWeightage())
                        .percentage(pct)
                        .status(obtained != null ? "COMPLETED" : "PENDING")
                        .build());
            }
            Double courseAvg = evaluated > 0 ? RiskFormula.round1(pctSum / evaluated) : null;
            if (courseAvg != null) { sumMarks += courseAvg; nMarks++; }

            int missing = (int) submissionRepository.findByStudentId(s.getId()).stream()
                    .filter(sub -> sub.getAssignment() != null && sub.getAssignment().getCourse() != null
                            && sub.getAssignment().getCourse().getId().equals(course.getId()))
                    .filter(sub -> sub.getSubmittedAt() == null
                            || (sub.getAssignment().getDueDate() != null && sub.getSubmittedAt().isAfter(sub.getAssignment().getDueDate())))
                    .count();

            RiskAssessmentResponse risk = riskAssessmentService.getLatestRiskAssessment(s.getId());
            String level = risk.getRiskLevel() != null ? risk.getRiskLevel().name() : null;
            if ("HIGH".equals(level)) high++; else if ("MEDIUM".equals(level)) medium++; else if ("LOW".equals(level)) low++;

            records.add(FacultyDashboardDto.StudentCoursePerformanceDto.builder()
                    .studentId(s.getId())
                    .studentName(s.getUser() != null ? s.getUser().getName() : null)
                    .studentNumber(s.getStudentNumber())
                    .cgpa(s.getCurrentCgpa())
                    .provenance(RuleBasedRiskEngineImpl.provenanceLabel(s))
                    .courseTotalClasses(total)
                    .coursePresentClasses(present)
                    .courseAbsentClasses(total - present)
                    .courseAttendancePercentage(attPct)
                    .courseAttendanceStatus(attPct == null ? "NO_DATA" : attPct < RiskFormula.ATTENDANCE_THRESHOLD ? "BELOW_THRESHOLD" : "NORMAL")
                    .overallAttendancePercentage(overallAtt)
                    .courseAverageMarksPercentage(courseAvg)
                    .marks(markDtos)
                    .missingAssignmentsCount(missing)
                    .riskLevel(level)
                    .riskScore(risk.getRiskScore())
                    .riskReason(courseRiskReason(course.getCode(), attPct, courseAvg, missing))
                    .build());
        }

        Set<Long> rosterIds = roster.stream().map(Student::getId).collect(Collectors.toSet());
        List<InterventionDto> interventions = interventionRepository.findAll().stream()
                .filter(i -> i.getStudent() != null && rosterIds.contains(i.getStudent().getId()))
                .filter(i -> i.getStatus() != InterventionStatus.CANCELLED)
                .sorted(Comparator.comparing(Intervention::getId))
                .map(i -> InterventionDto.builder()
                        .id(i.getId())
                        .studentId(i.getStudent().getId())
                        .studentName(i.getStudent().getUser() != null ? i.getStudent().getUser().getName() : null)
                        .type(i.getType())
                        .priority(i.getPriority())
                        .status(i.getStatus())
                        .description(i.getDescription())
                        .startDate(i.getStartDate())
                        .dueDate(i.getDueDate())
                        .assignedFacultyId(i.getAssignedFaculty() != null ? i.getAssignedFaculty().getId() : null)
                        .assignedFacultyName(i.getAssignedFaculty() != null && i.getAssignedFaculty().getUser() != null
                                ? i.getAssignedFaculty().getUser().getName() : null)
                        .build())
                .toList();

        List<FacultyDashboardDto.AssessmentOverviewDto> overviews = courseAssessments.stream().map(a -> {
            List<Double> pcts = markRepository.findByAssessmentId(a.getId()).stream()
                    .filter(m -> m.getStudent() != null && rosterIds.contains(m.getStudent().getId()))
                    .filter(m -> m.getMarksObtained() != null && a.getMaxMarks() != null && a.getMaxMarks() > 0)
                    .map(m -> m.getMarksObtained() / a.getMaxMarks() * 100.0)
                    .toList();
            Double avg = pcts.isEmpty() ? null : RiskFormula.round1(pcts.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
            return FacultyDashboardDto.AssessmentOverviewDto.builder()
                    .id(a.getId())
                    .name(a.getName())
                    .type(a.getType() != null ? a.getType().name() : null)
                    .maxMarks(a.getMaxMarks())
                    .weightage(a.getWeightage())
                    .status(avg != null ? "EVALUATED" : "PENDING")
                    .classAverage(avg) // average percentage across the roster's recorded marks
                    .build();
        }).toList();

        return FacultyDashboardDto.builder()
                .faculty(facultyDto)
                .courseCode(course.getCode())
                .courseName(course.getName())
                .credits(course.getCredits())
                .classroom(TimetableService.SUBJECT_ROOMS.get(course.getCode()))
                .slotDetails(slotSummary(course.getCode()))
                .totalConductedSessions(conductedSessions)
                .courseAverageAttendance(nAtt > 0 ? RiskFormula.round1(sumAtt / nAtt) : null)
                .courseAverageMarks(nMarks > 0 ? RiskFormula.round1(sumMarks / nMarks) : null)
                .enrolledStudentsCount(roster.size())
                .highRiskCount(high)
                .mediumRiskCount(medium)
                .lowRiskCount(low)
                .students(records)
                .assessments(overviews)
                .activeInterventions(interventions)
                .riskEngineSummary(riskEngineSummary())
                .build();
    }

    static String courseRiskReason(String courseCode, Double attPct, Double marksPct, int missing) {
        List<String> issues = new ArrayList<>();
        if (attPct != null && attPct < RiskFormula.ATTENDANCE_THRESHOLD) {
            issues.add(String.format(Locale.US, "attendance %.1f%% is below 75.0%%", attPct));
        }
        if (marksPct != null && marksPct < RiskFormula.MARKS_THRESHOLD) {
            issues.add(String.format(Locale.US, "evaluated marks average %.1f%% is below 50.0%%", marksPct));
        }
        if (missing > 0) {
            issues.add(missing + " deliverable(s) missing or late");
        }
        if (issues.isEmpty()) {
            if (attPct == null && marksPct == null) {
                return "No attendance or marks recorded yet in " + courseCode + ".";
            }
            return "No threshold breaches recorded in " + courseCode + ".";
        }
        return "In " + courseCode + ": " + String.join("; ", issues) + ".";
    }

    /** Weekly slots from the reference timetable, e.g. "Monday 09:00-09:50, ...". Null if the course is not in it. */
    static String slotSummary(String courseCode) {
        String summary = new TimetableService().getCommonTimetable().stream()
                .filter(s -> courseCode.equals(s.getCourseCode()))
                .map(s -> s.getDay().substring(0, 3) + " " + s.getTimeRange().replace(" To ", "-"))
                .collect(Collectors.joining(", "));
        return summary.isEmpty() ? null : summary;
    }

    private FacultyDashboardDto.RiskEngineSummaryDto riskEngineSummary() {
        return FacultyDashboardDto.RiskEngineSummaryDto.builder()
                .formula(RiskFormula.DESCRIPTION)
                .attendanceThresholdRule("Statutory minimum attendance is 75.0%. Deficits incur penalty = min((75.0 - attendance) * 1.5, 40.0) points.")
                .marksThresholdRule("Passing standard is 50.0% across evaluated assessments. Deficits incur penalty = min((50.0 - marks) * 1.0, 50.0) points.")
                .missingAssignmentRule("Each missing or late deliverable adds 5.0 points; only the overall score is capped at 100.")
                .pendingAssessmentRule("Pending (not yet evaluated) assessments are excluded from the marks penalty and never treated as zero.")
                .build();
    }

    private FacultyDto mapFacultyToDto(Faculty f) {
        User u = f.getUser();
        List<CourseAssignment> assignments = courseAssignmentRepository.findByFacultyIdWithCourse(f.getId());
        List<FacultyDto.CourseSummaryDto> courses = assignments.stream()
                .map(CourseAssignment::getCourse)
                .map(c -> FacultyDto.CourseSummaryDto.builder()
                        .courseCode(c.getCode())
                        .courseName(c.getName())
                        .credits(c.getCredits())
                        .classroom(TimetableService.SUBJECT_ROOMS.get(c.getCode()))
                        .slotDetails(slotSummary(c.getCode()))
                        .build())
                .toList();

        Set<Long> students = new HashSet<>();
        assignments.forEach(a -> rosterFor(a.getCourse()).forEach(s -> students.add(s.getId())));
        int atRisk = (int) students.stream()
                .map(riskAssessmentRepository::findFirstByStudentIdOrderByCalculatedAtDesc)
                .flatMap(Optional::stream)
                .filter(r -> r.getRiskLevel() == RiskLevel.HIGH)
                .count();

        FacultyDto.CourseSummaryDto primary = courses.isEmpty() ? null : courses.get(0);
        return FacultyDto.builder()
                .id(f.getId())
                .name(u != null ? u.getName() : null)
                .username(u != null ? u.getUsername() : null)
                .email(u != null ? u.getEmail() : null)
                .department(f.getDepartment())
                .title(f.getTitle())
                .employeeCode(null) // not stored; never invented
                .assignedCourseCode(primary != null ? primary.getCourseCode() : null)
                .assignedCourseName(primary != null ? primary.getCourseName() : null)
                .classroom(primary != null ? primary.getClassroom() : null)
                .assignedCourses(courses)
                .enrolledStudentsCount(students.size())
                .atRiskStudentsCount(atRisk)
                .build();
    }

}
