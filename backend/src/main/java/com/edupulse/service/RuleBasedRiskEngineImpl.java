package com.edupulse.service;

import com.edupulse.dto.RiskAssessmentResponse;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
            double attendancePercentage = (double) presentClasses / totalClasses * 100;
            if (attendancePercentage < 75.0) {
                double risk = (75.0 - attendancePercentage) * 1.5; // Max ~40
                totalRiskScore += risk;
                assessment.addRiskFactor(RiskFactor.builder()
                        .factorName("Low Attendance")
                        .impactLevel("MEDIUM")
                        .description("Attendance is " + String.format("%.1f", attendancePercentage) + "%")
                        .build());
            }
        }

        // 2. Marks Risk
        List<Mark> marks = markRepository.findByStudentId(studentId);
        if (!marks.isEmpty()) {
            double avgMarks = marks.stream().mapToDouble(m -> (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100).average().orElse(100.0);
            if (avgMarks < 50.0) {
                double risk = (50.0 - avgMarks) * 1.0; // Max 50
                totalRiskScore += risk;
                assessment.addRiskFactor(RiskFactor.builder()
                        .factorName("Low Grades")
                        .impactLevel("HIGH")
                        .description("Average marks are " + String.format("%.1f", avgMarks) + "%")
                        .build());
            }
        }

        // 3. Missing Assignments Risk
        List<AssignmentSubmission> submissions = submissionRepository.findByStudentId(studentId);
        long lateOrMissing = submissions.stream().filter(s -> s.getSubmittedAt() == null || s.getSubmittedAt().isAfter(s.getAssignment().getDueDate())).count();
        if (lateOrMissing > 0) {
            double risk = lateOrMissing * 5.0; // 5 pts per missing
            totalRiskScore += risk;
            assessment.addRiskFactor(RiskFactor.builder()
                    .factorName("Missing Assignments")
                    .impactLevel("MEDIUM")
                    .description(lateOrMissing + " assignments missing or late")
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
