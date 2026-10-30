package com.edupulse.service;

import com.edupulse.dto.RiskAssessmentResponse;
import com.edupulse.dto.RiskEngineExplanationDto;
import com.edupulse.model.RiskAssessment;

public interface RiskAssessmentService {
    RiskAssessment calculateRiskForStudent(Long studentId);
    RiskAssessmentResponse getLatestRiskAssessment(Long studentId);
    RiskEngineExplanationDto getRiskExplanation(Long studentId);
}
