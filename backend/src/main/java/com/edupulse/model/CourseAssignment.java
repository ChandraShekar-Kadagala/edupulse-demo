package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Explicit faculty-to-course teaching assignment (V2). Dashboards are driven only by these rows. */
@Entity
@Table(name = "course_assignments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(length = 64)
    private String section;

    @Column(length = 32)
    private String term;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
