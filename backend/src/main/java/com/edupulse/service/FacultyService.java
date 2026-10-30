package com.edupulse.service;

import com.edupulse.dto.*;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FacultyService {

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Autowired
    private InterventionRepository interventionRepository;

    public List<FacultyDto> getAllFacultyProfiles() {
        List<Faculty> allFaculty = facultyRepository.findAll();
        List<FacultyDto> result = new ArrayList<>();
        int studentCount = (int) studentRepository.count();

        for (Faculty f : allFaculty) {
            result.add(mapFacultyToDto(f, studentCount));
        }

        // If table is empty or before seeder runs, provide standard verified faculty details
        if (result.isEmpty()) {
            result.add(buildFallbackFaculty(101L, "Faculty A", "demo.faculty.1", "24082", "Associate Professor",
                    "CSE 204", "DESIGN AND ANALYSIS OF ALGORITHMS", "(C 305), (S 404)", studentCount));
            result.add(buildFallbackFaculty(102L, "Faculty B", "demo.faculty.2", "25346", "Assistant Professor",
                    "CSE 213", "ADVANCED JAVA PROGRAMMING", "(C 305), (S 404)", studentCount));
        }

        return result;
    }

    public FacultyDto getFacultyById(Long id) {
        int studentCount = (int) studentRepository.count();
        Faculty faculty = facultyRepository.findById(id).orElse(null);
        if (faculty != null) {
            return mapFacultyToDto(faculty, studentCount);
        }

        User u = userRepository.findById(id).orElse(null);
        if (u != null) {
            boolean isFaculty1 = u.getUsername().contains("faculty.1") || u.getUsername().equalsIgnoreCase("roopa") || u.getName().contains("Faculty A");
            return isFaculty1
                    ? buildFallbackFaculty(id, "Faculty A", "demo.faculty.1", "24082", "Associate Professor", "CSE 204", "DESIGN AND ANALYSIS OF ALGORITHMS", "(C 305), (S 404)", studentCount)
                    : buildFallbackFaculty(id, "Faculty B", "demo.faculty.2", "25346", "Assistant Professor", "CSE 213", "ADVANCED JAVA PROGRAMMING", "(C 305), (S 404)", studentCount);
        }

        if (id == 101L) {
            return buildFallbackFaculty(101L, "Faculty A", "demo.faculty.1", "24082", "Associate Professor",
                    "CSE 204", "DESIGN AND ANALYSIS OF ALGORITHMS", "(C 305), (S 404)", studentCount);
        }
        return buildFallbackFaculty(102L, "Faculty B", "demo.faculty.2", "25346", "Assistant Professor",
                "CSE 213", "ADVANCED JAVA PROGRAMMING", "(C 305), (S 404)", studentCount);
    }

    public FacultyDashboardDto getFacultyDashboard(Long facultyUserId, String username) {
        FacultyDto faculty = null;
        if (facultyUserId != null) {
            faculty = getFacultyById(facultyUserId);
        }
        if (faculty == null && username != null) {
            boolean isFaculty1 = username.contains("faculty.1") || username.equalsIgnoreCase("roopa");
            faculty = getFacultyById(isFaculty1 ? 101L : 102L);
        }
        if (faculty == null) {
            faculty = getFacultyById(101L);
        }

        String courseCode = faculty.getAssignedCourseCode() != null ? faculty.getAssignedCourseCode() : "CSE 204";
        Course course = courseRepository.findByCode(courseCode).orElse(null);
        if (course == null) {
            course = courseRepository.findAll().stream()
                    .filter(c -> c.getCode().equalsIgnoreCase(courseCode))
                    .findFirst()
                    .orElse(null);
        }

        final FacultyDto finalFaculty = faculty;
        final String finalCourseCode = courseCode;
        final Course finalCourse = course;
        final Long finalCourseId = course != null ? course.getId() : null;

        List<Student> students = studentRepository.findAll();
        List<FacultyDashboardDto.StudentCoursePerformanceDto> studentRecords = new ArrayList<>();
        double sumCourseAttPct = 0.0;
        int countAtt = 0;
        double sumCourseMarksPct = 0.0;
        int countMarks = 0;
        int highRiskCount = 0;
        int mediumRiskCount = 0;
        int lowRiskCount = 0;

        List<Assessment> courseAssessments = finalCourseId != null ?
                assessmentRepository.findAll().stream()
                        .filter(a -> a.getCourse() != null && a.getCourse().getId().equals(finalCourseId))
                        .toList()
                : List.of();

        for (Student s : students) {
            String name = s.getUser() != null ? s.getUser().getName() : "Student";
            String provenance = (s.getStudentNumber() != null && s.getStudentNumber().equals("STU-1001"))
                    ? "STUDENT-SUPPLIED REFERENCE DATA"
                    : "SYNTHETIC DEMO DATA";

            // Course Attendance
            List<Attendance> courseAtt = finalCourseId != null
                    ? attendanceRepository.findByStudentIdAndCourseId(s.getId(), finalCourseId)
                    : List.of();
            long totalClasses = courseAtt.size();
            long presentClasses = courseAtt.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            long absentClasses = totalClasses - presentClasses;
            Double attPct = totalClasses > 0
                    ? Math.round(((double) presentClasses / totalClasses * 100.0) * 10.0) / 10.0
                    : null;
            if (attPct != null) {
                sumCourseAttPct += attPct;
                countAtt++;
            }
            String attStatus = (attPct != null && attPct < 75.0) ? "BELOW_THRESHOLD" : "NORMAL";

            // Overall Attendance
            List<Attendance> allAtt = attendanceRepository.findByStudentId(s.getId());
            Double overallAttPct = allAtt.size() > 0
                    ? Math.round(((double) allAtt.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count() / allAtt.size() * 100.0) * 10.0) / 10.0
                    : null;

            // Course Marks
            List<Mark> studentMarks = markRepository.findByStudentId(s.getId());
            List<AssessmentMarkDto> markDtos = new ArrayList<>();
            double sumMarkPcts = 0.0;
            int evaluatedAssessments = 0;

            for (Assessment a : courseAssessments) {
                Mark m = studentMarks.stream()
                        .filter(mk -> mk.getAssessment() != null && mk.getAssessment().getId().equals(a.getId()))
                        .findFirst()
                        .orElse(null);
                Double marksObtained = m != null ? m.getMarksObtained() : null;
                Double pct = (marksObtained != null && a.getMaxMarks() != null && a.getMaxMarks() > 0)
                        ? Math.round((marksObtained / a.getMaxMarks() * 100.0) * 10.0) / 10.0
                        : null;
                String status = marksObtained != null ? "COMPLETED" : "PENDING";
                if (marksObtained != null) {
                    sumMarkPcts += pct != null ? pct : 0.0;
                    evaluatedAssessments++;
                }

                Double convertedMarks = (marksObtained != null && a.getMaxMarks() > 0 && a.getWeightage() != null)
                        ? Math.round((marksObtained / a.getMaxMarks() * a.getWeightage()) * 100.0) / 100.0
                        : null;

                markDtos.add(AssessmentMarkDto.builder()
                        .assessmentId(a.getId())
                        .assessmentName(a.getName())
                        .assessmentType(a.getType() != null ? a.getType().name() : "INTERNAL")
                        .courseCode(courseCode)
                        .courseName(course != null ? course.getName() : "")
                        .marksObtained(marksObtained)
                        .maxMarks(a.getMaxMarks())
                        .weightage(a.getWeightage())
                        .convertedMarks(convertedMarks)
                        .maxConvertedMarks(a.getWeightage())
                        .percentage(pct)
                        .status(status)
                        .build());
            }

            Double courseAvgMarksPct = evaluatedAssessments > 0
                    ? Math.round((sumMarkPcts / evaluatedAssessments) * 10.0) / 10.0
                    : null;
            if (courseAvgMarksPct != null) {
                sumCourseMarksPct += courseAvgMarksPct;
                countMarks++;
            }

            // Course Missing Submissions
            List<AssignmentSubmission> allSub = submissionRepository.findByStudentId(s.getId());
            int courseMissing = (int) allSub.stream()
                    .filter(sub -> sub.getAssignment() != null && sub.getAssignment().getCourse() != null
                            && sub.getAssignment().getCourse().getCode().equalsIgnoreCase(courseCode)
                            && "MISSING".equalsIgnoreCase(sub.getStatus()))
                    .count();

            // Risk Assessment
            RiskAssessmentResponse risk = riskAssessmentService.getLatestRiskAssessment(s.getId());
            String riskLevel = (risk != null && risk.getRiskLevel() != null) ? risk.getRiskLevel().toString() : "LOW";
            Double riskScore = risk != null ? risk.getRiskScore() : 0.0;

            if ("HIGH".equalsIgnoreCase(riskLevel)) highRiskCount++;
            else if ("MEDIUM".equalsIgnoreCase(riskLevel)) mediumRiskCount++;
            else lowRiskCount++;

            String riskReason;
            if ("HIGH".equalsIgnoreCase(riskLevel)) {
                riskReason = String.format("Attendance (%.1f%%) and marks (%.1f%%) in %s fall critically below requirements.",
                        attPct != null ? attPct : 0.0, courseAvgMarksPct != null ? courseAvgMarksPct : 0.0, courseCode);
            } else if ("MEDIUM".equalsIgnoreCase(riskLevel)) {
                riskReason = String.format("Attendance (%.1f%%) is below the statutory 75.0%% requirement in %s.",
                        attPct != null ? attPct : 0.0, courseCode);
            } else {
                riskReason = String.format("Satisfactory performance maintained in %s.", courseCode);
            }

            studentRecords.add(FacultyDashboardDto.StudentCoursePerformanceDto.builder()
                    .studentId(s.getId())
                    .studentName(name)
                    .studentNumber(s.getStudentNumber())
                    .cgpa(s.getCurrentCgpa())
                    .provenance(provenance)
                    .courseTotalClasses(totalClasses)
                    .coursePresentClasses(presentClasses)
                    .courseAbsentClasses(absentClasses)
                    .courseAttendancePercentage(attPct)
                    .courseAttendanceStatus(attStatus)
                    .overallAttendancePercentage(overallAttPct)
                    .courseAverageMarksPercentage(courseAvgMarksPct)
                    .marks(markDtos)
                    .missingAssignmentsCount(courseMissing)
                    .riskLevel(riskLevel)
                    .riskScore(riskScore)
                    .riskReason(riskReason)
                    .build());
        }

        // Assessments overview
        List<FacultyDashboardDto.AssessmentOverviewDto> assessmentOverviews = new ArrayList<>();
        for (Assessment a : courseAssessments) {
            List<Mark> allAssessmentMarks = markRepository.findAll().stream()
                    .filter(mk -> mk.getAssessment() != null && mk.getAssessment().getId().equals(a.getId()))
                    .toList();
            Double classAvg = null;
            if (!allAssessmentMarks.isEmpty()) {
                double total = allAssessmentMarks.stream().mapToDouble(Mark::getMarksObtained).sum();
                classAvg = Math.round((total / allAssessmentMarks.size()) * 10.0) / 10.0;
            }
            assessmentOverviews.add(FacultyDashboardDto.AssessmentOverviewDto.builder()
                    .id(a.getId())
                    .name(a.getName())
                    .type(a.getType() != null ? a.getType().name() : "INTERNAL")
                    .maxMarks(a.getMaxMarks())
                    .weightage(a.getWeightage())
                    .status(classAvg != null ? "EVALUATED" : "PENDING")
                    .classAverage(classAvg)
                    .build());
        }

        // Active Interventions
        List<InterventionDto> activeInterventions = interventionRepository.findAll().stream()
                .filter(i -> i.getStatus() != InterventionStatus.CANCELLED)
                .map(i -> InterventionDto.builder()
                        .id(i.getId())
                        .studentId(i.getStudent() != null ? i.getStudent().getId() : null)
                        .studentName(i.getStudent() != null && i.getStudent().getUser() != null ? i.getStudent().getUser().getName() : "Student")
                        .type(i.getType())
                        .priority(i.getPriority())
                        .status(i.getStatus() != null ? i.getStatus() : InterventionStatus.PLANNED)
                        .description(i.getDescription())
                        .startDate(i.getStartDate())
                        .dueDate(i.getDueDate())
                        .assignedFacultyId(i.getAssignedFaculty() != null ? i.getAssignedFaculty().getId() : null)
                        .assignedFacultyName(i.getAssignedFaculty() != null && i.getAssignedFaculty().getUser() != null ? i.getAssignedFaculty().getUser().getName() : finalFaculty.getName())
                        .build())
                .toList();

        long totalConducted = courseCode.equalsIgnoreCase("CSE 204") ? 48L : 49L;

        return FacultyDashboardDto.builder()
                .faculty(faculty)
                .courseCode(courseCode)
                .courseName(course != null ? course.getName() : faculty.getAssignedCourseName())
                .credits(4)
                .classroom(faculty.getClassroom() != null ? faculty.getClassroom() : "(C 305), (S 404)")
                .slotDetails(courseCode.equalsIgnoreCase("CSE 204")
                        ? "Mon (09:00-10:50), Wed (10:00-10:50), Fri (01:00-02:50)"
                        : "Mon (01:00-01:50), Thu (09:00-10:50), Fri (09:00-10:50)")
                .totalConductedSessions(totalConducted)
                .courseAverageAttendance(countAtt > 0 ? Math.round((sumCourseAttPct / countAtt) * 10.0) / 10.0 : null)
                .courseAverageMarks(countMarks > 0 ? Math.round((sumCourseMarksPct / countMarks) * 10.0) / 10.0 : null)
                .enrolledStudentsCount(students.size())
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .students(studentRecords)
                .assessments(assessmentOverviews)
                .activeInterventions(activeInterventions)
                .riskEngineSummary(FacultyDashboardDto.RiskEngineSummaryDto.builder()
                        .formula("Total Risk Score = Min(Attendance Penalty [max 40] + Marks Penalty [max 50] + Missing Deliverables [5 pts each], 100.0)")
                        .attendanceThresholdRule("Statutory minimum attendance is 75.0%. Deficits incur penalty = min((75.0 - attendance) * 1.5, 40.0) points.")
                        .marksThresholdRule("Passing standard is 50.0% across evaluated assessments. Deficits incur penalty = min((50.0 - marks) * 1.0, 50.0) points.")
                        .missingAssignmentRule("Overdue deliverables incur a direct penalty of 5.0 points per missing submission (capped at 30.0 points).")
                        .pendingAssessmentRule("Unconducted and pending internal evaluation components (CLA 2, CLA 3, Projects) are strictly excluded from marks penalty and never treated as zero.")
                        .build())
                .build();
    }

    private FacultyDto mapFacultyToDto(Faculty f, int studentCount) {
        User u = f.getUser();
        String name = u != null ? u.getName() : "Faculty Member";
        String username = u != null ? u.getUsername() : "demo.faculty.1";
        String email = u != null ? u.getEmail() : "demo.faculty.1@edupulse.local";

        boolean isFaculty1 = name.contains("Faculty A") || name.contains("Roopa") || username.equalsIgnoreCase("demo.faculty.1") || username.equalsIgnoreCase("roopa");
        String courseCode = isFaculty1 ? "CSE 204" : "CSE 213";
        String courseName = isFaculty1 ? "DESIGN AND ANALYSIS OF ALGORITHMS" : "ADVANCED JAVA PROGRAMMING";
        String empCode = isFaculty1 ? "24082" : "25346";
        String classroom = isFaculty1 ? "(C 305), (S 404)" : "(C 305), (S 404)";

        return FacultyDto.builder()
                .id(f.getId())
                .name(name)
                .username(username)
                .email(email)
                .department(f.getDepartment() != null ? f.getDepartment() : "Computer Science & Engineering")
                .title(f.getTitle() != null ? f.getTitle() : "Faculty Advisor")
                .employeeCode(empCode)
                .assignedCourseCode(courseCode)
                .assignedCourseName(courseName)
                .classroom(classroom)
                .assignedCourses(List.of(
                        FacultyDto.CourseSummaryDto.builder()
                                .courseCode(courseCode)
                                .courseName(courseName)
                                .credits(4)
                                .classroom(classroom)
                                .slotDetails(isFaculty1 ? "Mon (09:00-10:50), Wed (10:00-10:50), Fri (01:00-02:50)" : "Mon (01:00-01:50), Thu (09:00-10:50), Fri (09:00-10:50)")
                                .build()
                ))
                .enrolledStudentsCount(studentCount)
                .atRiskStudentsCount(1) // Arun Kumar
                .build();
    }

    private FacultyDto buildFallbackFaculty(Long id, String name, String username, String empCode, String title,
                                            String courseCode, String courseName, String classroom, int studentCount) {
        return FacultyDto.builder()
                .id(id)
                .name(name)
                .username(username)
                .email(username + "@edupulse.local")
                .department("Computer Science & Engineering")
                .title(title)
                .employeeCode(empCode)
                .assignedCourseCode(courseCode)
                .assignedCourseName(courseName)
                .classroom(classroom)
                .assignedCourses(List.of(
                        FacultyDto.CourseSummaryDto.builder()
                                .courseCode(courseCode)
                                .courseName(courseName)
                                .credits(4)
                                .classroom(classroom)
                                .slotDetails(courseCode.equals("CSE 204") ? "Mon (09:00-10:50), Wed (10:00-10:50), Fri (01:00-02:50)" : "Mon (01:00-01:50), Thu (09:00-10:50), Fri (09:00-10:50)")
                                .build()
                ))
                .enrolledStudentsCount(studentCount)
                .atRiskStudentsCount(1)
                .build();
    }
}
