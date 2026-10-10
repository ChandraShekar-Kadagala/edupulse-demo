package com.edupulse.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FollowUpDto {
    private Long id;
    private Long interventionId;
    private Long facultyId;
    private String facultyName;
    private String notes;
    private String outcome;
    private Double previousRiskScore;
    private Double newRiskScore;
    private LocalDateTime createdAt;
}
