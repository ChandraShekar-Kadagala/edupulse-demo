package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "interventions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Intervention {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_faculty_id", nullable = false)
    private Faculty assignedFaculty;
    
    private String type; // e.g. "Mentor Meeting", "Tutoring"
    private String description;
    
    @Enumerated(EnumType.STRING)
    private InterventionStatus status;
    
    private String priority; // LOW, MEDIUM, HIGH
    
    private LocalDate startDate;
    private LocalDate dueDate;
    
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "intervention", cascade = CascadeType.ALL)
    private java.util.List<FollowUp> followUps;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}