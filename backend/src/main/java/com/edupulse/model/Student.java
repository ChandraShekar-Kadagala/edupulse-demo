package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {
    @Id
    private Long id; // matches User ID
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(unique = true, nullable = false)
    private String studentNumber;
    
    private String department;
    private Integer currentSemester;
    private Double currentCgpa;

    /** Primary source of this student's academic records (null = unspecified / legacy record). */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_provenance", length = 64)
    private DataProvenance dataProvenance;
}