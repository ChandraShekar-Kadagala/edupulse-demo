package com.edupulse.service;

import com.edupulse.dto.RiskAssessmentResponse;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
    @Transactional
    public RiskAssessment calculateRiskForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        RiskAssessment assessment = RiskAssessment.builder()
                .student(student)
                .calculatedAt(LocalDateTime.now())
                .build();

        double totalRiskScore = 0.0;

        // 1. Attendance Risk
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

        // 2. Marks Risk
        List<Mark> marks = markRepository.findByStudentId(studentId);
        if (!marks.isEmpty()) {
            double avgMarks = marks.stream()
                    .filter(m -> m.getAssessment() != null && m.getAssessment().getMaxMarks() > 0)
                    .mapToDouble(m -> (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0)
                    .average().orElse(100.0);
            if (avgMarks < 50.0) {
                double risk = Math.min((50.0 - avgMarks) * 1.0, 50.0);
                totalRiskScore += risk;
                assessment.addRiskFactor(RiskFactor.builder()
                        .factorName("Low Grades")
                        .impactLevel("HIGH")
                        .description(String.format(Locale.US, "Average marks are %.1f%% across %d evaluations (threshold: 50.0%%)",
                                avgMarks, marks.size()))
                        .build());
            }
        }

        // 3. Missing Assignments Risk
        List<AssignmentSubmission> submissions = submissionRepository.findByStudentId(studentId);
        long lateOrMissing = submissions.stream()
                .filter(s -> s.getSubmittedAt() == null || (s.getAssignment() != null && s.getSubmittedAt().isAfter(s.getAssignment().getDueDate())))
                .count();
        if (lateOrMissing > 0) {
            double risk = lateOrMissing * 5.0; // 5 pts per missing
            totalRiskScore += risk;
            assessment.addRiskFactor(RiskFactor.builder()
                    .factorName("Missing Assignments")
                    .impactLevel(lateOrMissing >= 3 ? "HIGH" : "MEDIUM")
                    .description(lateOrMissing + " assignment(s) are overdue or missing submission")
                    .build());
        }

        // 4. Missing data flag if completely empty
        if (totalClasses == 0 && marks.isEmpty() && submissions.isEmpty()) {
            assessment.addRiskFactor(RiskFactor.builder()
                    .factorName("Data Status")
                    .impactLevel("LOW")
                    .description("Insufficient data: No attendance, marks, or assignment records are available.")
                    .build());
        }

        totalRiskScore = Math.min(totalRiskScore, 100.0);
        assessment.setRiskScore(totalRiskScore);

        if (totalRiskScore >= 65) {
            assessment.setRiskLevel(RiskLevel.HIGH);
        } else if (totalRiskScore >= 35) {
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
}
