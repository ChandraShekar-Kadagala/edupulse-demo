package com.edupulse.service;

import com.edupulse.dto.RiskAssessmentResponse;
import com.edupulse.dto.RiskEngineExplanationDto;
import com.edupulse.model.RiskAssessment;

public interface RiskAssessmentService {
    /** Always appends a new snapshot. Use after academic data changes. */
    RiskAssessment calculateRiskForStudent(Long studentId);
    /** Returns the latest snapshot, appending a new one only if current data yields a different result. */
    RiskAssessment ensureCurrentSnapshot(Long studentId);
    RiskAssessmentResponse getLatestRiskAssessment(Long studentId);
    RiskEngineExplanationDto getRiskExplanation(Long studentId);
}
