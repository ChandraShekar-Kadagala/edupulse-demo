package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentMarkDto {
    private Long markId;
    private Long assessmentId;
    private String courseCode;
    private String courseName;
    private String assessmentName;
    private String assessmentType;
    private Double marksObtained; // conducted raw mark
    private Double maxMarks; // conducted max mark
    private Double convertedMarks; // institutional converted mark secured
    private Double maxConvertedMarks; // institutional converted max mark
    private Double percentage; // (marksObtained / maxMarks) * 100
    private Double weightage;
    private String status; // COMPLETED, PENDING, NOT_CONDUCTED
    private String facultyName;
    private String classroom;
}
