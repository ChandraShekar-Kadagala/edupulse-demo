package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assessments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Assessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    private String name;
    private Double maxMarks;
    private Double weightage;
    
    @Enumerated(EnumType.STRING)
    private AssessmentType type;
}