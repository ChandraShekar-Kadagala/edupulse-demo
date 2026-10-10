package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "encrypted_risk_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncryptedRiskRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String ciphertextBase64;

    @Column(nullable = false)
    private String nonceBase64;

    @Column(nullable = false)
    private String encryptionVersion;

    @Column(nullable = false)
    private String schemaVersion;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
