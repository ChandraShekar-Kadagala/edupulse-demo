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
    private long totalClasses; // classes conducted
    private long presentClasses;
    private long absentClasses;
    private long odMlClasses; // On Duty / Medical Leave approved
    private Long totalHours;
    private Long attendedHours;
    private Double attendancePercentage; // null if totalClasses == 0
    private String status; // NORMAL, BELOW_THRESHOLD, NO_DATA
    private String period; // "03-Aug-2026 to 29-Oct-2026"
    private String facultyName;
    private String classroom;

    public long getTotalHours() {
        return totalHours != null ? totalHours : totalClasses;
    }

    public long getAttendedHours() {
        return attendedHours != null ? attendedHours : presentClasses;
    }
}
