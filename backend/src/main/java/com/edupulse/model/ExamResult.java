package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

/** One exam-ledger / semester-result row (V3). */
@Entity
@Table(name = "exam_results")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(nullable = false, length = 32)
    private String semester;

    @Column(name = "month_year", length = 16)
    private String monthYear;

    private Double credit;

    @Column(length = 4)
    private String grade;

    @Column(name = "grade_points")
    private Double gradePoints;

    @Column(length = 16)
    private String result;

    private int attempt;
}
