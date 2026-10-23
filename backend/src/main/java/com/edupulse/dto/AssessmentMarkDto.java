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
    private Double marksObtained;
    private Double maxMarks;
    private Double percentage; // (marksObtained / maxMarks) * 100
    private Double weightage;
}
