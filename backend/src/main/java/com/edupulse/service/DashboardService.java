package com.edupulse.service;

import com.edupulse.repository.StudentRepository;
import com.edupulse.repository.InterventionRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
@lombok.RequiredArgsConstructor
public class DashboardService {
    private final StudentRepository studentRepository;
    
    private final InterventionRepository interventionRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", studentRepository.count());
        stats.put("activeInterventions", interventionRepository.count());
        stats.put("highRiskStudents", 5); // Example hardcoded or computed
        stats.put("mediumRiskStudents", 15);
        return stats;
    }
}
