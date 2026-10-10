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
public class RiskAssessmentResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private Double riskScore;
    private RiskLevel riskLevel;
    private LocalDateTime calculatedAt;
    private List<RiskFactorDto> riskFactors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskFactorDto {
        private String factorType;
        private Double impactScore;
        private String description;
    }
}
