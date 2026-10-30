package com.edupulse.dto;

import com.edupulse.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAcademicAnalyticsDto {
    private Long studentId;
    private String studentNumber;
    private String name;
    private String department;
    private Integer currentSemester;
    private Double currentCgpa;

    // Provenance & Freshness
    private String provenance; // CSV_FACULTY_IMPORT, SYNTHETIC_TEST_FIXTURE, OFFICIAL_INSTITUTIONAL_API, ACTUAL STUDENT-SUPPLIED REFERENCE DATA, SYNTHETIC DEMO DATA
    private String dataStatus; // COMPLETE, PARTIAL, INSUFFICIENT_DATA
    private LocalDateTime lastActivityDate;
    private boolean isStale;
    private String disclaimer;
    private String attendancePeriod;
    private String lastUpdated;

    // Attendance Summary
    private boolean hasAttendanceData;
    private long totalClasses;
    private long presentClasses;
    private long absentClasses;
    private Double overallAttendancePercentage; // null if hasAttendanceData == false
    private List<CourseAttendanceDto> courseAttendanceBreakdown;

    // Marks & Assessment Summary
    private boolean hasMarksData;
    private Double overallAverageMarksPercentage; // null if hasMarksData == false
    private int totalAssessments;
    private List<AssessmentMarkDto> assessmentBreakdown;

    // Coursework / Assignment Summary
    private boolean hasAssignmentData;
    private int totalAssignments;
    private int submittedAssignments;
    private int missingAssignments;

    // Performance Trends (genuine recorded data points)
    private boolean hasTrends;
    private List<PerformanceTrendPointDto> trendPoints;

    // Risk Assessment
    private Double riskScore;
    private RiskLevel riskLevel;
    private LocalDateTime riskCalculatedAt;
    private List<RiskAssessmentResponse.RiskFactorDto> riskFactors;

    // Actionable Recommendations
    private List<AcademicRecommendationDto> recommendations;

    // Explainable Insights
    private List<String> strengths;
    private List<String> improvementAreas;
    private List<String> upcomingConcerns;

    // Next CLA / NTM Target Planner
    private List<FutureTargetPlannerDto> targetPlanner;

    // Transparent Risk Engine Explanation
    private RiskEngineExplanationDto riskEngineExplanation;

    // Common Course Schedule & Timetable
    private List<TimetableSlotDto> timetable;
}
