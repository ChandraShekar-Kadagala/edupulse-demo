package com.edupulse.dto;

import lombok.Data;

@Data
public class FollowUpRequest {
    private Long facultyId;
    private String notes;
    private String outcome;
}
