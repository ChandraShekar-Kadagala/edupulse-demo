package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One student-controlled import (V3). Rows link back here so an import can be deleted as a unit. */
@Entity
@Table(name = "academic_import_batches")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AcademicImportBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "report_type", length = 32)
    private String reportType;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "content_sha256", length = 64)
    private String contentSha256;

    @Column(nullable = false, length = 16)
    private String status; // PREVIEW | CONFIRMED | DISCARDED

    @Column(name = "accepted_rows")
    private int acceptedRows;

    @Column(name = "rejected_rows")
    private int rejectedRows;

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
