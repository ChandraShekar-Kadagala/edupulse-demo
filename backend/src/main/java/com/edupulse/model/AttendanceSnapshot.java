package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A dated aggregate of one course's attendance (V3). Replaces fabricating one fake row per day from a
 * report total: the report only gives present/conducted as of a date, so that is exactly what we store.
 */
@Entity
@Table(name = "attendance_snapshots")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceSnapshot {
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

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    private int conducted;
    private int present;
    private int absent;

    @Column(name = "od_ml")
    private int odMl;
}
