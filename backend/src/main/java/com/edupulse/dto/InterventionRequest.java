package com.edupulse.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class InterventionRequest {
    private Long studentId;
    private Long assignedFacultyId;
    private String type;
    private String description;
    private String priority;
    private LocalDate dueDate;
    /** Optional; defaults to today. Used as the baseline point for effectiveness analytics. */
    private LocalDate startDate;
}
