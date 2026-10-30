package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyDashboardDto {
    private FacultyDto faculty;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private String classroom;
    private String slotDetails;
    private long totalConductedSessions;
    private Double courseAverageAttendance;
    private Double courseAverageMarks;
    private int enrolledStudentsCount;
    private int highRiskCount;
    private int mediumRiskCount;
    private int lowRiskCount;
    private List<StudentCoursePerformanceDto> students;
    private List<AssessmentOverviewDto> assessments;
    private List<InterventionDto> activeInterventions;
    private RiskEngineSummaryDto riskEngineSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentCoursePerformanceDto {
        private Long studentId;
        private String studentName;
        private String studentNumber;
        private Double cgpa;
        private String provenance;
        // Course-specific attendance
        private long courseTotalClasses;
        private long coursePresentClasses;
        private long courseAbsentClasses;
        private Double courseAttendancePercentage;
        private String courseAttendanceStatus; // NORMAL, BELOW_THRESHOLD
        // Overall attendance
        private Double overallAttendancePercentage;
        // Course-specific marks
        private Double courseAverageMarksPercentage;
        private List<AssessmentMarkDto> marks;
        // Course-specific missing assignments
        private int missingAssignmentsCount;
        // Overall risk
        private String riskLevel;
        private Double riskScore;
        private String riskReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentOverviewDto {
        private Long id;
        private String name;
        private String type;
        private Double maxMarks;
        private Double weightage;
        private String status; // EVALUATED, PENDING
        private Double classAverage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskEngineSummaryDto {
        private String formula;
        private String attendanceThresholdRule;
        private String marksThresholdRule;
        private String missingAssignmentRule;
        private String pendingAssessmentRule;
    }
}
