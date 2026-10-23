package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAttendanceDto {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private long totalClasses;
    private long presentClasses;
    private long absentClasses;
    private Long totalHours;
    private Long attendedHours;
    private Double attendancePercentage; // null if totalClasses == 0
    private String status; // NORMAL, BELOW_THRESHOLD, NO_DATA

    public long getTotalHours() {
        return totalHours != null ? totalHours : totalClasses;
    }

    public long getAttendedHours() {
        return attendedHours != null ? attendedHours : presentClasses;
    }
}
