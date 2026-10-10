package com.edupulse.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentDto {
    private Long id;
    private String name;
    private String email;
    private String studentNumber;
    private String department;
    private Integer currentSemester;
    private Double currentCgpa;
}
