package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FutureTargetPlannerDto {
    private String courseCode;
    private String courseName;
    private Double currentObtainedMarks;
    private Double currentEvaluatedMaxMarks;
    private Double currentConvertedMarks;
    private Double totalSubjectMaxMarks;
    private String pendingAssessmentName;
    private Double pendingAssessmentMaxMarks;
    private Integer remainingAssessmentsCount;
    private String remainingAssessmentsList;
    private TargetScenario minimumTarget;
    private TargetScenario recommendedTarget;
    private TargetScenario stretchTarget;
    private String riskImpactDescription;
    private boolean verifiedWeightage;
    private String projectionDisclaimer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetScenario {
        private String scenarioName;
        private Double targetScore;
        private Double expectedConvertedTotal;
        private Double projectedPercentage;
        private String projectedRiskLevel;
        private String rationale;
    }
}
