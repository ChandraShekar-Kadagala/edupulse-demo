package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceTrendPointDto {
    private String name; // e.g. "2026-01", "Month 1", "Midterm"
    private Double attendance; // percentage, nullable if no attendance in period
    private Double marks; // percentage, nullable if no marks in period
    private int classesCount;
    private int assessmentsCount;
}
