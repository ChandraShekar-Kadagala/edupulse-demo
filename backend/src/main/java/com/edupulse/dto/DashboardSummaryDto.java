package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    private long totalStudents;
    private long highRisk;
    private long mediumRisk;
    private long lowRisk;
    private long insufficientData;
    private long activeInterventions;
    private long completedInterventions;
    private Double cohortAverageAttendance; // null if no students with attendance
    private Double cohortAverageMarks; // null if no students with marks
    private List<RecentHighRiskStudentDto> recentHighRisk;
    private List<String> cohortStrengths;
    private List<String> cohortImprovementAreas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentHighRiskStudentDto {
        private Long id;
        private String name;
        private String factor;
        private Double riskScore;
        private String intervention;
    }
}
