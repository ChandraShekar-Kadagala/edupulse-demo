const fs = require('fs');
const path = require('path');

const models = [
  {
    name: 'Role',
    content: `package com.edupulse.model;
public enum Role { ADMIN, FACULTY, STUDENT }`
  },
  {
    name: 'User',
    content: `package com.edupulse.model;

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
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}`
  },
  {
    name: 'Student',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {
    @Id
    private Long id; // matches User ID
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(unique = true, nullable = false)
    private String studentNumber;
    
    private String department;
    private Integer currentSemester;
    private Double currentCgpa;
}`
  },
  {
    name: 'Faculty',
    content: `package com.edupulse.model;

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
}`
  },
  {
    name: 'Course',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    private Integer credits;
    private String department;
}`
  },
  {
    name: 'Enrollment',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "enrollments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    private Integer semester;
    private String status; // ACTIVE, COMPLETED, DROPPED
}`
  },
  {
    name: 'AttendanceStatus',
    content: `package com.edupulse.model;
public enum AttendanceStatus { PRESENT, ABSENT, EXCUSED }`
  },
  {
    name: 'Attendance',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendance")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    private LocalDate date;
    
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;
}`
  },
  {
    name: 'AssessmentType',
    content: `package com.edupulse.model;
public enum AssessmentType { INTERNAL, FINAL, ASSIGNMENT }`
  },
  {
    name: 'Assessment',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assessments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Assessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    private String name;
    private Double maxMarks;
    private Double weightage;
    
    @Enumerated(EnumType.STRING)
    private AssessmentType type;
}`
  },
  {
    name: 'Mark',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marks")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Mark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;
    
    private Double marksObtained;
}`
  },
  {
    name: 'Assignment',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "assignments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Assignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    private String name;
    private LocalDate dueDate;
}`
  },
  {
    name: 'AssignmentSubmission',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "assignment_submissions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignmentSubmission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;
    
    private String status; // SUBMITTED, LATE, MISSING
    private LocalDate submittedAt;
}`
  },
  {
    name: 'RiskLevel',
    content: `package com.edupulse.model;
public enum RiskLevel { LOW, MEDIUM, HIGH }`
  },
  {
    name: 'RiskAssessment',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "risk_assessments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskAssessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    private LocalDateTime calculatedAt;
    
    private Double riskScore; // 0 - 100
    
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    
    @OneToMany(mappedBy = "riskAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RiskFactor> riskFactors = new ArrayList<>();
    
    public void addRiskFactor(RiskFactor factor) {
        riskFactors.add(factor);
        factor.setRiskAssessment(this);
    }
}`
  },
  {
    name: 'RiskFactor',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "risk_factors")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskFactor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_assessment_id", nullable = false)
    private RiskAssessment riskAssessment;
    
    private String factorName;
    private String factorValue;
    private String impactLevel; // LOW, MEDIUM, HIGH
    private String description;
}`
  },
  {
    name: 'InterventionStatus',
    content: `package com.edupulse.model;
public enum InterventionStatus { PLANNED, IN_PROGRESS, COMPLETED, CANCELLED }`
  },
  {
    name: 'Intervention',
    content: `package com.edupulse.model;

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
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}`
  },
  {
    name: 'FollowUp',
    content: `package com.edupulse.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow_ups")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FollowUp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intervention_id", nullable = false)
    private Intervention intervention;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    private String outcome; // e.g., IMPROVED, NO_SIGNIFICANT_CHANGE, DETERIORATED
    
    private Double previousRiskScore;
    private Double newRiskScore;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}`
  }
];

const targetDir = path.join(__dirname, 'src', 'main', 'java', 'com', 'edupulse', 'model');

if (!fs.existsSync(targetDir)) {
  fs.mkdirSync(targetDir, { recursive: true });
}

models.forEach(model => {
  fs.writeFileSync(path.join(targetDir, model.name + '.java'), model.content.trim());
});
console.log('Successfully generated model classes.');
