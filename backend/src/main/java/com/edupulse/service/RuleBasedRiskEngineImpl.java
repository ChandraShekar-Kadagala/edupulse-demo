package com.edupulse.service;

import com.edupulse.dto.RiskAssessmentResponse;
import com.edupulse.dto.RiskEngineExplanationDto;
import com.edupulse.exception.ResourceNotFoundException;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.service.risk.RiskFormula;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Official rule-based risk engine. All arithmetic lives in {@link RiskFormula}; this class only
 * gathers inputs from the database, persists immutable snapshots and builds explanations.
 */
@Service
@RequiredArgsConstructor
public class RuleBasedRiskEngineImpl implements RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSnapshotRepository attendanceSnapshotRepository;
    private final MarkRepository markRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    /** Inputs to the formula, gathered from the student's current academic records. */
    record RiskInputs(
            List<Attendance> attendances,
            List<AttendanceSnapshot> snapshots,
            long presentClasses,
            long totalClasses,
            Double attendancePercentage,
            List<Mark> evaluatedMarks,
            Double averageMarksPercentage,
            List<AssignmentSubmission> submissions,
            long missingOrLate) {

        boolean hasNoData() {
            return attendances.isEmpty() && snapshots.isEmpty() && evaluatedMarks.isEmpty() && submissions.isEmpty();
        }
    }

    RiskInputs gatherInputs(Long studentId) {
        // Dated attendance rows are authoritative. When there are none (e.g. a student self-import), the
        // latest per-course attendance snapshot is used instead — real dated aggregates, never fabricated.
        List<Attendance> attendances = attendanceRepository.findByStudentId(studentId);
        List<AttendanceSnapshot> snapshots = List.of();
        long present;
        long total;
        Double attendancePct;
        if (!attendances.isEmpty()) {
            present = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            total = attendances.size();
            attendancePct = (double) present / total * 100.0;
        } else {
            snapshots = latestSnapshotPerCourse(studentId);
            present = snapshots.stream().mapToLong(AttendanceSnapshot::getPresent).sum();
            total = snapshots.stream().mapToLong(AttendanceSnapshot::getConducted).sum();
            attendancePct = total > 0 ? (double) present / total * 100.0 : null;
        }

        // Only completed assessments count; pending / not-conducted ones are excluded, never zeroed.
        List<Mark> evaluated = markRepository.findByStudentId(studentId).stream()
                .filter(m -> m.getAssessment() != null && m.getAssessment().getMaxMarks() > 0 && m.getMarksObtained() != null)
                .toList();
        Double avgMarks = evaluated.isEmpty() ? null : evaluated.stream()
                .mapToDouble(m -> (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0)
                .average().orElseThrow();

        List<AssignmentSubmission> submissions = submissionRepository.findByStudentId(studentId);
        long missingOrLate = submissions.stream()
                .filter(s -> s.getSubmittedAt() == null
                        || (s.getAssignment() != null && s.getAssignment().getDueDate() != null
                            && s.getSubmittedAt().isAfter(s.getAssignment().getDueDate())))
                .count();

        return new RiskInputs(attendances, snapshots, present, total, attendancePct, evaluated, avgMarks, submissions, missingOrLate);
    }

    /** Latest attendance snapshot for each course the student has imported (one per course). */
    private List<AttendanceSnapshot> latestSnapshotPerCourse(Long studentId) {
        return attendanceSnapshotRepository.findByStudentId(studentId).stream()
                .filter(s -> s.getCourse() != null)
                .collect(Collectors.groupingBy(s -> s.getCourse().getId(),
                        Collectors.maxBy(java.util.Comparator.comparing(AttendanceSnapshot::getAsOfDate))))
                .values().stream().flatMap(java.util.Optional::stream).toList();
    }

    private Student requireStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
    }

    /** Always appends a new immutable snapshot (use after data changes, e.g. imports). */
    @Override
    @Transactional
    public RiskAssessment calculateRiskForStudent(Long studentId) {
        Student student = requireStudent(studentId);
        RiskInputs in = gatherInputs(studentId);
        RiskFormula.Result result = RiskFormula.evaluate(in.attendancePercentage(), in.averageMarksPercentage(), in.missingOrLate());
        return riskAssessmentRepository.save(buildSnapshot(student, in, result));
    }

    /**
     * Returns the latest snapshot if it still matches the formula applied to current data; otherwise
     * appends a new one. Read paths use this so page views never inflate snapshot history.
     */
    @Override
    @Transactional
    public RiskAssessment ensureCurrentSnapshot(Long studentId) {
        Student student = requireStudent(studentId);
        RiskInputs in = gatherInputs(studentId);
        RiskFormula.Result result = RiskFormula.evaluate(in.attendancePercentage(), in.averageMarksPercentage(), in.missingOrLate());
        Optional<RiskAssessment> latest = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(studentId);
        if (latest.isPresent()
                && latest.get().getRiskScore() != null
                && Double.compare(latest.get().getRiskScore(), result.totalScore()) == 0
                && latest.get().getRiskLevel() == result.riskLevel()) {
            return latest.get();
        }
        return riskAssessmentRepository.save(buildSnapshot(student, in, result));
    }

    private RiskAssessment buildSnapshot(Student student, RiskInputs in, RiskFormula.Result result) {
        RiskAssessment assessment = RiskAssessment.builder()
                .student(student)
                .calculatedAt(LocalDateTime.now())
                .build();

        if (result.attendancePenalty() > 0) {
            double pct = in.attendancePercentage();
            assessment.addRiskFactor(RiskFactor.builder()
                    .factorName("Low Attendance")
                    .impactLevel(pct < 60.0 ? "HIGH" : "MEDIUM")
                    .description(String.format(Locale.US, "Attendance is %.1f%% (%d/%d sessions, threshold: 75.0%%)",
                            pct, in.presentClasses(), in.totalClasses()))
                    .build());
        }
        if (result.marksPenalty() > 0) {
            assessment.addRiskFactor(RiskFactor.builder()
                    .factorName("Low Grades")
                    .impactLevel("HIGH")
                    .description(String.format(Locale.US, "Average marks are %.1f%% across %d evaluations (threshold: 50.0%%)",
                            in.averageMarksPercentage(), in.evaluatedMarks().size()))
                    .build());
        }
        if (in.missingOrLate() > 0) {
            assessment.addRiskFactor(RiskFactor.builder()
                    .factorName("Missing Assignments")
                    .impactLevel(in.missingOrLate() >= 3 ? "HIGH" : "MEDIUM")
                    .description(in.missingOrLate() + " assignment(s) are overdue or missing submission")
                    .build());
        }
        if (in.hasNoData()) {
            assessment.addRiskFactor(RiskFactor.builder()
                    .factorName("Data Status")
                    .impactLevel("LOW")
                    .description("Insufficient data: No attendance, marks, or assignment records are available.")
                    .build());
        }

        assessment.setRiskScore(result.totalScore());
        assessment.setRiskLevel(result.riskLevel());
        return assessment;
    }

    @Override
    @Transactional
    public RiskAssessmentResponse getLatestRiskAssessment(Long studentId) {
        RiskAssessment assessment = ensureCurrentSnapshot(studentId);

        List<RiskAssessmentResponse.RiskFactorDto> factorDtos = assessment.getRiskFactors().stream()
                .map(f -> RiskAssessmentResponse.RiskFactorDto.builder()
                        .factorType(f.getFactorName())
                        .impactScore("HIGH".equals(f.getImpactLevel()) ? 80.0 : "MEDIUM".equals(f.getImpactLevel()) ? 50.0 : 20.0)
                        .description(f.getDescription())
                        .build())
                .collect(Collectors.toList());

        return RiskAssessmentResponse.builder()
                .id(assessment.getId())
                .studentId(assessment.getStudent().getId())
                .studentName(assessment.getStudent().getUser().getName())
                .riskScore(assessment.getRiskScore())
                .riskLevel(assessment.getRiskLevel())
                .calculatedAt(assessment.getCalculatedAt())
                .riskFactors(factorDtos)
                .build();
    }

    @Override
    @Transactional
    public RiskEngineExplanationDto getRiskExplanation(Long studentId) {
        Student student = requireStudent(studentId);
        // Snapshot and explanation are derived from the same inputs, so the breakdown always sums to the score.
        RiskAssessment assessment = ensureCurrentSnapshot(studentId);
        RiskInputs in = gatherInputs(studentId);
        RiskFormula.Result result = RiskFormula.evaluate(in.attendancePercentage(), in.averageMarksPercentage(), in.missingOrLate());

        Double attPct = in.attendancePercentage();
        RiskEngineExplanationDto.ComponentExplanation attComp = RiskEngineExplanationDto.ComponentExplanation.builder()
                .componentName("Attendance Risk Component")
                .actualValue(attPct == null ? null : RiskFormula.round1(attPct))
                .thresholdValue(RiskFormula.ATTENDANCE_THRESHOLD)
                .pointsContributed(result.attendancePenalty())
                .maxPossiblePoints(RiskFormula.ATTENDANCE_MAX_PENALTY)
                .calculationFormula("min((75.0 - attendancePct) * 1.5, 40.0) when attendancePct < 75.0, else 0")
                .explanation(attPct == null
                        ? "No attendance records yet. No penalty applied."
                        : attPct >= RiskFormula.ATTENDANCE_THRESHOLD
                            ? String.format(Locale.US, "Attendance is healthy at %.1f%% (above 75.0%% statutory threshold). Zero penalty applied.", attPct)
                            : String.format(Locale.US, "Attendance of %.1f%% is below the 75.0%% minimum threshold. Penalty of %.1f points applied.", attPct, result.attendancePenalty()))
                .status(attPct == null ? "NO_DATA" : attPct >= 75.0 ? "HEALTHY" : attPct >= 60.0 ? "ATTENTION" : "CRITICAL")
                .build();

        Double avg = in.averageMarksPercentage();
        RiskEngineExplanationDto.ComponentExplanation marksComp = RiskEngineExplanationDto.ComponentExplanation.builder()
                .componentName("Evaluated Marks Component")
                .actualValue(avg == null ? null : RiskFormula.round1(avg))
                .thresholdValue(RiskFormula.MARKS_THRESHOLD)
                .pointsContributed(result.marksPenalty())
                .maxPossiblePoints(RiskFormula.MARKS_MAX_PENALTY)
                .calculationFormula("min((50.0 - avgMarksPct) * 1.0, 50.0) when avgMarksPct < 50.0, else 0")
                .explanation(avg == null
                        ? "No completed assessments yet. Pending assessments are excluded, not treated as zero."
                        : avg >= RiskFormula.MARKS_THRESHOLD
                            ? String.format(Locale.US, "Evaluated marks average %.1f%% across %d graded assessments (above 50.0%% benchmark). Zero penalty applied. Pending assessments are excluded.", avg, in.evaluatedMarks().size())
                            : String.format(Locale.US, "Evaluated marks average %.1f%% is below 50.0%% benchmark. Penalty of %.1f points applied.", avg, result.marksPenalty()))
                .status(avg == null ? "NO_DATA" : avg >= 70.0 ? "HEALTHY" : avg >= 50.0 ? "ATTENTION" : "CRITICAL")
                .build();

        long missing = in.missingOrLate();
        RiskEngineExplanationDto.ComponentExplanation missingComp = RiskEngineExplanationDto.ComponentExplanation.builder()
                .componentName("Missing Deliverables Component")
                .actualValue((double) missing)
                .thresholdValue(0.0)
                .pointsContributed(result.missingDeliverablesPenalty())
                .maxPossiblePoints(null) // 5 points each; only the overall total is capped at 100
                .calculationFormula("missingOrLateCount * 5.0")
                .explanation(missing == 0
                        ? "All scheduled coursework and assignment deliverables submitted on time. Zero penalty applied."
                        : String.format(Locale.US, "%d assignment(s) are overdue or unsubmitted. Penalty of %.1f points applied (5.0 pts/assignment).", missing, result.missingDeliverablesPenalty()))
                .status(missing == 0 ? "HEALTHY" : missing <= 2 ? "ATTENTION" : "CRITICAL")
                .build();

        return RiskEngineExplanationDto.builder()
                .overallScore(assessment.getRiskScore())
                .riskLevel(assessment.getRiskLevel().name())
                .attendanceComponent(attComp)
                .marksComponent(marksComp)
                .missingAssignmentsComponent(missingComp)
                .subjectRisks(buildSubjectRisks(in))
                .formulaDescription(RiskFormula.DESCRIPTION)
                .formulaVersion(RiskFormula.VERSION)
                .recommendedIntervention(recommendationFor(assessment.getRiskLevel()))
                .dataSourceProvenance(provenanceLabel(student))
                .calculatedAt(assessment.getCalculatedAt())
                .build();
    }

    private List<RiskEngineExplanationDto.SubjectRiskDto> buildSubjectRisks(RiskInputs in) {
        Map<Course, List<Attendance>> byCourse = in.attendances().stream()
                .filter(a -> a.getCourse() != null)
                .collect(Collectors.groupingBy(Attendance::getCourse));
        List<RiskEngineExplanationDto.SubjectRiskDto> subjectRisks = new ArrayList<>();
        for (Map.Entry<Course, List<Attendance>> entry : byCourse.entrySet()) {
            Course c = entry.getKey();
            List<Attendance> cAtt = entry.getValue();
            long cTotal = cAtt.size();
            long cPres = cAtt.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            double cPct = (double) cPres / cTotal * 100.0;

            List<Mark> cMarks = in.evaluatedMarks().stream()
                    .filter(m -> m.getAssessment().getCourse() != null && m.getAssessment().getCourse().getId().equals(c.getId()))
                    .toList();
            Double cMarkPct = cMarks.isEmpty() ? null : cMarks.stream()
                    .mapToDouble(m -> (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0)
                    .average().orElseThrow();

            String status = (cPct < 75.0 || (cMarkPct != null && cMarkPct < 50.0)) ? "HIGH"
                    : (cPct < 80.0 || (cMarkPct != null && cMarkPct < 60.0)) ? "MEDIUM" : "LOW";
            String details = String.format(Locale.US, "Attendance: %.1f%% (%d/%d classes)%s",
                    cPct, cPres, cTotal,
                    cMarkPct != null ? String.format(Locale.US, " | Evaluated Marks: %.1f%%", cMarkPct) : " | Pending assessments");

            subjectRisks.add(RiskEngineExplanationDto.SubjectRiskDto.builder()
                    .courseCode(c.getCode())
                    .courseName(c.getName())
                    .attendancePercentage(RiskFormula.round1(cPct))
                    .marksPercentage(cMarkPct != null ? RiskFormula.round1(cMarkPct) : null)
                    .riskStatus(status)
                    .details(details)
                    .build());
        }
        // Snapshot-based subjects (imported attendance has no dated rows to group by course).
        if (in.attendances().isEmpty()) {
            for (AttendanceSnapshot s : in.snapshots()) {
                Course c = s.getCourse();
                double cPct = s.getConducted() > 0 ? (double) s.getPresent() / s.getConducted() * 100.0 : 100.0;
                List<Mark> cMarks = in.evaluatedMarks().stream()
                        .filter(m -> m.getAssessment().getCourse() != null && m.getAssessment().getCourse().getId().equals(c.getId()))
                        .toList();
                Double cMarkPct = cMarks.isEmpty() ? null : cMarks.stream()
                        .mapToDouble(m -> (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0)
                        .average().orElseThrow();
                String status = (cPct < 75.0 || (cMarkPct != null && cMarkPct < 50.0)) ? "HIGH"
                        : (cPct < 80.0 || (cMarkPct != null && cMarkPct < 60.0)) ? "MEDIUM" : "LOW";
                String details = String.format(Locale.US, "Attendance: %.1f%% (%d/%d classes, imported)%s",
                        cPct, s.getPresent(), s.getConducted(),
                        cMarkPct != null ? String.format(Locale.US, " | Evaluated Marks: %.1f%%", cMarkPct) : "");
                subjectRisks.add(RiskEngineExplanationDto.SubjectRiskDto.builder()
                        .courseCode(c.getCode()).courseName(c.getName())
                        .attendancePercentage(RiskFormula.round1(cPct))
                        .marksPercentage(cMarkPct != null ? RiskFormula.round1(cMarkPct) : null)
                        .riskStatus(status).details(details).build());
            }
        }
        subjectRisks.sort((a, b) -> a.getCourseCode().compareTo(b.getCourseCode()));
        return subjectRisks;
    }

    static String recommendationFor(RiskLevel level) {
        return switch (level) {
            case HIGH -> "URGENT INTERVENTION: Immediate faculty counseling recommended. Attendance and coursework recovery plan required.";
            case MEDIUM -> "ADVISORY INTERVENTION: Remedial tutoring suggested in subjects with attendance or assessment scores below threshold.";
            default -> "MAINTAIN COURSE: Academic performance and attendance remain within healthy thresholds. Continue regular progress.";
        };
    }

    /**
     * Human-readable provenance label from the stored students.data_provenance (V2). For legacy rows
     * without a stored value, DEMO showcase accounts are still recognised from account metadata.
     */
    public static String provenanceLabel(Student student) {
        DataProvenance p = student.getDataProvenance();
        if (p == null) {
            User user = student.getUser();
            if (user != null && user.getAuthProvider() == AuthProvider.DEMO) {
                p = REFERENCE_STUDENT_NUMBER.equals(student.getStudentNumber())
                        ? DataProvenance.STUDENT_SUPPLIED_REFERENCE : DataProvenance.SYNTHETIC_TEST_FIXTURE;
            } else {
                return "UNSPECIFIED SOURCE";
            }
        }
        return switch (p) {
            case STUDENT_SUPPLIED_REFERENCE -> "STUDENT-SUPPLIED REFERENCE DATA";
            case SYNTHETIC_TEST_FIXTURE -> "SYNTHETIC DEMO DATA";
            case CSV_FACULTY_IMPORT -> "IMPORTED ACADEMIC REPORT (CSV)";
            case INSTITUTIONAL_PORTAL_FIXTURE, INSTITUTIONAL_PORTAL_REPORT -> "IMPORTED ACADEMIC REPORT (PORTAL)";
            case OFFICIAL_INSTITUTIONAL_API -> "OFFICIAL INSTITUTIONAL RECORD";
        };
    }

    static final String REFERENCE_STUDENT_NUMBER = "STU-1001";
}
