package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marks")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Mark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;
    
    private Double marksObtained;
}