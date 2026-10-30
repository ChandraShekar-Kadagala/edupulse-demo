package com.edupulse.service;

import com.edupulse.dto.RiskAssessmentResponse;
import com.edupulse.dto.RiskEngineExplanationDto;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RuleBasedRiskEngineImpl implements RiskAssessmentService {

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RiskAssessment calculateRiskForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        RiskAssessment assessment = RiskAssessment.builder()
                .student(student)
                .calculatedAt(LocalDateTime.now())
                .build();

        double totalRiskScore = 0.0;

        // 1. Attendance Risk Component
        // Formula: If aggregate attendance < 75.0%, penalty = min((75.0 - attendance) * 1.5, 40.0)
        List<Attendance> attendances = attendanceRepository.findByStudentId(studentId);
        long totalClasses = attendances.size();
        if (totalClasses > 0) {
            long presentClasses = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            double attendancePercentage = (double) presentClasses / totalClasses * 100.0;
            if (attendancePercentage < 75.0) {
                double risk = Math.min((75.0 - attendancePercentage) * 1.5, 40.0);
                totalRiskScore += risk;
                assessment.addRiskFactor(RiskFactor.builder()
                        .factorName("Low Attendance")
                        .impactLevel(attendancePercentage < 60.0 ? "HIGH" : "MEDIUM")
                        .description(String.format(Locale.US, "Attendance is %.1f%% (%d/%d sessions, threshold: 75.0%%)",
                                attendancePercentage, presentClasses, totalClasses))
                        .build());
            }
        }

        // 2. Marks Risk Component (Evaluated strictly on completed assessments; pending/not conducted are excluded)
        // Formula: If evaluated avg marks < 50.0%, penalty = min((50.0 - avgMarks) * 1.0, 50.0)
        List<Mark> marks = markRepository.findByStudentId(studentId);
        List<Mark> evaluatedMarks = marks.stream()
                .filter(m -> m.getAssessment() != null && m.getAssessment().getMaxMarks() > 0 && m.getMarksObtained() != null)
                .toList();

        if (!evaluatedMarks.isEmpty()) {
            double avgMarks = evaluatedMarks.stream()
                    .mapToDouble(m -> (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0)
                    .average().orElse(100.0);
            if (avgMarks < 50.0) {
                double risk = Math.min((50.0 - avgMarks) * 1.0, 50.0);
                totalRiskScore += risk;
                assessment.addRiskFactor(RiskFactor.builder()
                        .factorName("Low Grades")
                        .impactLevel("HIGH")
                        .description(String.format(Locale.US, "Average marks are %.1f%% across %d evaluations (threshold: 50.0%%)",
                                avgMarks, evaluatedMarks.size()))
                        .build());
            }
        }

        // 3. Missing Assignments Risk Component
        // Formula: 5.0 points per missing or overdue deliverable
        List<AssignmentSubmission> submissions = submissionRepository.findByStudentId(studentId);
        long lateOrMissing = submissions.stream()
                .filter(s -> s.getSubmittedAt() == null || (s.getAssignment() != null && s.getSubmittedAt().isAfter(s.getAssignment().getDueDate())))
                .count();
        if (lateOrMissing > 0) {
            double risk = lateOrMissing * 5.0;
            totalRiskScore += risk;
            assessment.addRiskFactor(RiskFactor.builder()
                    .factorName("Missing Assignments")
                    .impactLevel(lateOrMissing >= 3 ? "HIGH" : "MEDIUM")
                    .description(lateOrMissing + " assignment(s) are overdue or missing submission")
                    .build());
        }

        // 4. Missing data flag if completely empty
        if (totalClasses == 0 && evaluatedMarks.isEmpty() && submissions.isEmpty()) {
            assessment.addRiskFactor(RiskFactor.builder()
                    .factorName("Data Status")
                    .impactLevel("LOW")
                    .description("Insufficient data: No attendance, marks, or assignment records are available.")
                    .build());
        }

        totalRiskScore = Math.min(totalRiskScore, 100.0);
        assessment.setRiskScore(Math.round(totalRiskScore * 10.0) / 10.0);

        if (totalRiskScore >= 65.0) {
            assessment.setRiskLevel(RiskLevel.HIGH);
        } else if (totalRiskScore >= 35.0) {
            assessment.setRiskLevel(RiskLevel.MEDIUM);
        } else {
            assessment.setRiskLevel(RiskLevel.LOW);
        }

        return riskAssessmentRepository.save(assessment);
    }

    @Override
    public RiskAssessmentResponse getLatestRiskAssessment(Long studentId) {
        RiskAssessment assessment = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(studentId)
                .orElseGet(() -> calculateRiskForStudent(studentId));

        List<RiskAssessmentResponse.RiskFactorDto> factorDtos = assessment.getRiskFactors().stream()
                .map(f -> RiskAssessmentResponse.RiskFactorDto.builder()
                        .factorType(f.getFactorName())
                        .impactScore(f.getImpactLevel().equals("HIGH") ? 80.0 : f.getImpactLevel().equals("MEDIUM") ? 50.0 : 20.0)
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
    public RiskEngineExplanationDto getRiskExplanation(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        RiskAssessment assessment = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(studentId)
                .orElseGet(() -> calculateRiskForStudent(studentId));

        // 1. Attendance Component
        List<Attendance> attendances = attendanceRepository.findByStudentId(studentId);
        long totalClasses = attendances.size();
        long presentClasses = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        double attendancePct = totalClasses > 0 ? (double) presentClasses / totalClasses * 100.0 : 100.0;
        double attendancePenalty = attendancePct < 75.0 ? Math.min((75.0 - attendancePct) * 1.5, 40.0) : 0.0;

        RiskEngineExplanationDto.ComponentExplanation attComp = RiskEngineExplanationDto.ComponentExplanation.builder()
                .componentName("Attendance Risk Component")
                .actualValue(Math.round(attendancePct * 10.0) / 10.0)
                .thresholdValue(75.0)
                .pointsContributed(Math.round(attendancePenalty * 10.0) / 10.0)
                .maxPossiblePoints(40.0)
                .calculationFormula("Math.min((75.0 - attendancePct) * 1.5, 40.0)")
                .explanation(attendancePct >= 75.0
                        ? String.format(Locale.US, "Attendance is healthy at %.1f%% (above 75.0%% statutory threshold). Zero penalty applied.", attendancePct)
                        : String.format(Locale.US, "Attendance of %.1f%% is below the 75.0%% minimum threshold. Penalty of %.1f points applied.", attendancePct, attendancePenalty))
                .status(attendancePct >= 75.0 ? "HEALTHY" : attendancePct >= 60.0 ? "ATTENTION" : "CRITICAL")
                .build();

        // 2. Marks Component
        List<Mark> marks = markRepository.findByStudentId(studentId);
        List<Mark> evaluatedMarks = marks.stream()
                .filter(m -> m.getAssessment() != null && m.getAssessment().getMaxMarks() > 0 && m.getMarksObtained() != null)
                .toList();

        double avgMarksPct = evaluatedMarks.isEmpty() ? 100.0 : evaluatedMarks.stream()
                .mapToDouble(m -> (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0)
                .average().orElse(100.0);
        double marksPenalty = avgMarksPct < 50.0 ? Math.min((50.0 - avgMarksPct) * 1.0, 50.0) : 0.0;

        RiskEngineExplanationDto.ComponentExplanation marksComp = RiskEngineExplanationDto.ComponentExplanation.builder()
                .componentName("Evaluated Marks Component")
                .actualValue(Math.round(avgMarksPct * 10.0) / 10.0)
                .thresholdValue(50.0)
                .pointsContributed(Math.round(marksPenalty * 10.0) / 10.0)
                .maxPossiblePoints(50.0)
                .calculationFormula("Math.min((50.0 - avgMarksPct) * 1.0, 50.0)")
                .explanation(avgMarksPct >= 50.0
                        ? String.format(Locale.US, "Evaluated marks average %.1f%% across %d graded assessments (above 50.0%% benchmark). Zero penalty applied. Pending assessments are excluded.", avgMarksPct, evaluatedMarks.size())
                        : String.format(Locale.US, "Evaluated marks average %.1f%% is below 50.0%% benchmark. Penalty of %.1f points applied.", avgMarksPct, marksPenalty))
                .status(avgMarksPct >= 70.0 ? "HEALTHY" : avgMarksPct >= 50.0 ? "ATTENTION" : "CRITICAL")
                .build();

        // 3. Missing Deliverables Component
        List<AssignmentSubmission> submissions = submissionRepository.findByStudentId(studentId);
        long missingCount = submissions.stream()
                .filter(s -> s.getSubmittedAt() == null || (s.getAssignment() != null && s.getSubmittedAt().isAfter(s.getAssignment().getDueDate())))
                .count();
        double missingPenalty = missingCount * 5.0;

        RiskEngineExplanationDto.ComponentExplanation missingComp = RiskEngineExplanationDto.ComponentExplanation.builder()
                .componentName("Missing Deliverables Component")
                .actualValue((double) missingCount)
                .thresholdValue(0.0)
                .pointsContributed(missingPenalty)
                .maxPossiblePoints(30.0)
                .calculationFormula("missingCount * 5.0")
                .explanation(missingCount == 0
                        ? "All scheduled coursework and assignment deliverables submitted on time. Zero penalty applied."
                        : String.format(Locale.US, "%d assignment(s) are overdue or unsubmitted. Penalty of %.1f points applied (5.0 pts/assignment).", missingCount, missingPenalty))
                .status(missingCount == 0 ? "HEALTHY" : missingCount <= 2 ? "ATTENTION" : "CRITICAL")
                .build();

        // Subject-Specific Risks
        Map<Course, List<Attendance>> byCourseAtt = attendances.stream().collect(Collectors.groupingBy(Attendance::getCourse));
        List<RiskEngineExplanationDto.SubjectRiskDto> subjectRisks = new ArrayList<>();

        for (Map.Entry<Course, List<Attendance>> entry : byCourseAtt.entrySet()) {
            Course c = entry.getKey();
            List<Attendance> cAtt = entry.getValue();
            long cTotal = cAtt.size();
            long cPres = cAtt.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            double cPct = cTotal > 0 ? (double) cPres / cTotal * 100.0 : 100.0;

            List<Mark> cMarks = evaluatedMarks.stream()
                    .filter(m -> m.getAssessment() != null && m.getAssessment().getCourse() != null && m.getAssessment().getCourse().getId().equals(c.getId()))
                    .toList();
            Double cMarkPct = cMarks.isEmpty() ? null : cMarks.stream()
                    .mapToDouble(m -> (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0)
                    .average().orElse(100.0);

            String status = (cPct < 75.0 || (cMarkPct != null && cMarkPct < 50.0)) ? "HIGH" : (cPct < 80.0 || (cMarkPct != null && cMarkPct < 60.0)) ? "MEDIUM" : "LOW";
            String details = String.format(Locale.US, "Attendance: %.1f%% (%d/%d classes)%s",
                    cPct, cPres, cTotal,
                    cMarkPct != null ? String.format(Locale.US, " | Evaluated Marks: %.1f%%", cMarkPct) : " | Pending assessments");

            subjectRisks.add(RiskEngineExplanationDto.SubjectRiskDto.builder()
                    .courseCode(c.getCode())
                    .courseName(c.getName())
                    .attendancePercentage(Math.round(cPct * 10.0) / 10.0)
                    .marksPercentage(cMarkPct != null ? Math.round(cMarkPct * 10.0) / 10.0 : null)
                    .riskStatus(status)
                    .details(details)
                    .build());
        }

        String interventionRec;
        if (assessment.getRiskScore() >= 65.0) {
            interventionRec = "URGENT INTERVENTION: Immediate mandatory faculty counseling scheduled. Attendance and coursework recovery plan required.";
        } else if (assessment.getRiskScore() >= 35.0) {
            interventionRec = "ADVISORY INTERVENTION: Remedial tutoring suggested in subjects with attendance or assessment scores below threshold.";
        } else {
            interventionRec = "MAINTAIN COURSE: Academic performance and attendance remain within healthy thresholds. Continue regular progress.";
        }

        String provenance = (student.getStudentNumber().equals("STU-1001")
                || student.getUser().getUsername().equalsIgnoreCase("demo.student.a")
                || student.getUser().getUsername().equalsIgnoreCase("student_a")
                || student.getUser().getName().contains("Student A"))
                ? "STUDENT-SUPPLIED REFERENCE DATA"
                : "SYNTHETIC DEMO DATA";

        return RiskEngineExplanationDto.builder()
                .overallScore(assessment.getRiskScore())
                .riskLevel(assessment.getRiskLevel().name())
                .attendanceComponent(attComp)
                .marksComponent(marksComp)
                .missingAssignmentsComponent(missingComp)
                .subjectRisks(subjectRisks)
                .formulaDescription("Total Risk Score = Min(Attendance Penalty [max 40] + Marks Penalty [max 50] + Missing Deliverables [5 pts each], 100.0). Risk Categories: LOW (<35.0), MEDIUM (35.0-64.9), HIGH (>=65.0). Pending assessments are never treated as zero.")
                .recommendedIntervention(interventionRec)
                .dataSourceProvenance(provenance)
                .calculatedAt(assessment.getCalculatedAt())
                .build();
    }
}
