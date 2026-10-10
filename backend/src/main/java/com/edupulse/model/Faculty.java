package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faculty")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Faculty {
    @Id
    private Long id; // matches User ID
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    private String department;
    private String title;
}