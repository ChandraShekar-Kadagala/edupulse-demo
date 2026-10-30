package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyDto {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String department;
    private String title;
    private String employeeCode;
    private String assignedCourseCode;
    private String assignedCourseName;
    private String classroom;
    private List<CourseSummaryDto> assignedCourses;
    private int enrolledStudentsCount;
    private int atRiskStudentsCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseSummaryDto {
        private String courseCode;
        private String courseName;
        private Integer credits;
        private String classroom;
        private String slotDetails;
    }
}
