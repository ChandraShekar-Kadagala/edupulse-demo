package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEngineExplanationDto {
    private Double overallScore;
    private String riskLevel;
    private ComponentExplanation attendanceComponent;
    private ComponentExplanation marksComponent;
    private ComponentExplanation missingAssignmentsComponent;
    private List<SubjectRiskDto> subjectRisks;
    private String formulaDescription;
    private String recommendedIntervention;
    private String dataSourceProvenance;
    private LocalDateTime calculatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentExplanation {
        private String componentName;
        private Double actualValue;
        private Double thresholdValue;
        private Double pointsContributed;
        private Double maxPossiblePoints;
        private String calculationFormula;
        private String explanation;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectRiskDto {
        private String courseCode;
        private String courseName;
        private Double attendancePercentage;
        private Double marksPercentage;
        private String riskStatus;
        private String details;
    }
}
