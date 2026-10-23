package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    private String name;
    private String email;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Builder.Default
    @Column(name = "token_version")
    private Long tokenVersion = 1L;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider = AuthProvider.LOCAL;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (tokenVersion == null) {
            tokenVersion = 1L;
        }
        if (authProvider == null) {
            authProvider = AuthProvider.LOCAL;
        }
    }

    public Long getTokenVersion() {
        return tokenVersion != null ? tokenVersion : 1L;
    }

    public AuthProvider getAuthProvider() {
        return authProvider != null ? authProvider : AuthProvider.LOCAL;
    }
}