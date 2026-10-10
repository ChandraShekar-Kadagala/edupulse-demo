package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow_ups")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FollowUp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intervention_id", nullable = false)
    private Intervention intervention;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    private String outcome; // e.g., IMPROVED, NO_SIGNIFICANT_CHANGE, DETERIORATED
    
    private Double previousRiskScore;
    private Double newRiskScore;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}