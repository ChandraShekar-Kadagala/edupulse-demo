package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "risk_assessments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskAssessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    private LocalDateTime calculatedAt;
    
    private Double riskScore; // 0 - 100
    
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    
    @OneToMany(mappedBy = "riskAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RiskFactor> riskFactors = new ArrayList<>();
    
    public void addRiskFactor(RiskFactor factor) {
        riskFactors.add(factor);
        factor.setRiskAssessment(this);
    }
}