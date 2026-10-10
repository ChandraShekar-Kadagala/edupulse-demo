package com.edupulse.dto;

import com.edupulse.model.InterventionStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InterventionDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long assignedFacultyId;
    private String assignedFacultyName;
    private String type;
    private String description;
    private InterventionStatus status;
    private String priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private java.util.List<FollowUpDto> followUps;
}
