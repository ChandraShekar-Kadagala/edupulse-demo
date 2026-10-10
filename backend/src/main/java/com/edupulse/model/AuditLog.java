package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;
    
    private Long userId; // the user who performed the action
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    private LocalDateTime timestamp;
}
