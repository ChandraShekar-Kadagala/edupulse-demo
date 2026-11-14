package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Versioned, purpose-specific, revocable consent for importing a student's academic data (V3). */
@Entity
@Table(name = "student_data_consents")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentDataConsent {
    public static final String PURPOSE_RISK_ANALYTICS = "ACADEMIC_RISK_ANALYTICS";
    public static final String CURRENT_POLICY_VERSION = "v1";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 64)
    private String purpose;

    @Column(name = "policy_version", nullable = false, length = 16)
    private String policyVersion;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }

    public boolean isActive() {
        return revokedAt == null;
    }
}
