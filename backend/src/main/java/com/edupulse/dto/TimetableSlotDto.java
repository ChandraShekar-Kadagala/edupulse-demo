package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableSlotDto {
    private String day;
    private int slotNumber;
    private String timeRange;
    private String courseCode;
    private String courseName;
    private String classroom;
    private String facultyName;
    private String ltp;
}
