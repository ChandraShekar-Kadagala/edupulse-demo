package com.edupulse.service;

import com.edupulse.exception.ResourceNotFoundException;
import com.edupulse.model.*;
import com.edupulse.repository.FollowUpRepository;
import com.edupulse.repository.InterventionRepository;
import com.edupulse.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InterventionService {

    private final InterventionRepository interventionRepository;
    private final FollowUpRepository followUpRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskAssessmentService riskAssessmentService;

    public List<Intervention> getInterventionsForStudent(Long studentId) {
        return interventionRepository.findByStudentId(studentId);
    }

    public List<Intervention> getAllInterventions() {
        return interventionRepository.findAll();
    }

    public Intervention createIntervention(Intervention intervention) {
        return interventionRepository.save(intervention);
    }

    public Intervention requireIntervention(Long id) {
        return interventionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Intervention", id));
    }

    @Transactional
    public Intervention updateInterventionStatus(Long id, InterventionStatus status) {
        Intervention i = requireIntervention(id);
        i.setStatus(status);
        return interventionRepository.save(i);
    }

    /** When the intervention began: its start date, else its creation time. */
    public static LocalDateTime startOf(Intervention i) {
        if (i.getStartDate() != null) {
            return i.getStartDate().atStartOfDay();
        }
        return i.getCreatedAt();
    }

    /** Latest official snapshot taken at or before the intervention started (the baseline), if any. */
    public Optional<RiskAssessment> baselineSnapshot(Intervention i) {
        LocalDateTime start = startOf(i);
        if (start == null || i.getStudent() == null) {
            return Optional.empty();
        }
        return riskAssessmentRepository.findByStudentId(i.getStudent().getId()).stream()
                .filter(r -> r.getCalculatedAt() != null && !r.getCalculatedAt().isAfter(start))
                .max(Comparator.comparing(RiskAssessment::getCalculatedAt));
    }

    /**
     * Records a follow-up. Risk scores are taken from official snapshots on the server, never from the
     * client: previous = baseline snapshot before the intervention started (null if none existed),
     * new = the student's current snapshot.
     */
    @Transactional
    public FollowUp addFollowUp(Long interventionId, FollowUp followUp) {
        Intervention i = requireIntervention(interventionId);
        followUp.setIntervention(i);
        followUp.setCreatedAt(LocalDateTime.now());
        followUp.setPreviousRiskScore(baselineSnapshot(i).map(RiskAssessment::getRiskScore).orElse(null));
        followUp.setNewRiskScore(riskAssessmentService.ensureCurrentSnapshot(i.getStudent().getId()).getRiskScore());
        return followUpRepository.save(followUp);
    }
}
