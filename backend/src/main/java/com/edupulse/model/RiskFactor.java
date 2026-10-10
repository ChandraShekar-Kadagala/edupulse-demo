package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "risk_factors")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskFactor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_assessment_id", nullable = false)
    private RiskAssessment riskAssessment;
    
    private String factorName;
    private String factorValue;
    private String impactLevel; // LOW, MEDIUM, HIGH
    private String description;
}