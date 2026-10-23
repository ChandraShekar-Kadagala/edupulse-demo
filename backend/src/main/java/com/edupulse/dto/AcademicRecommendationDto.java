package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicRecommendationDto {
    private String category; // ATTENDANCE, ACADEMIC_PERFORMANCE, COURSEWORK, ENRICHMENT, DATA_COLLECTION
    private String title;
    private String rationale;
    private String nextStep;
    private String priority; // HIGH, MEDIUM, LOW
}
