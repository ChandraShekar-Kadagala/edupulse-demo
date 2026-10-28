package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDto {
    private Long id;
    private String name;
    private String email;
    private String studentNumber;
    private String department;
    private Integer currentSemester;
    private Double currentCgpa;
    private UserDto user;
    private String riskLevel;
    private Double riskScore;
    private Double attendancePercentage;
    private Double averageMarksPercentage;
    private String riskReason;
    private Long totalClasses;
    private Long presentClasses;
}
