package com.edupulse.service;

import com.edupulse.model.Intervention;
import com.edupulse.repository.InterventionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InterventionService {
    @Autowired
    private InterventionRepository interventionRepository;

    public List<Intervention> getInterventionsForStudent(Long studentId) {
        return interventionRepository.findByStudentId(studentId);
    }
    
    public Intervention createIntervention(Intervention intervention) {
        return interventionRepository.save(intervention);
    }

    public Intervention updateInterventionStatus(Long id, com.edupulse.model.InterventionStatus status) {
        Intervention i = interventionRepository.findById(id).orElseThrow();
        i.setStatus(status);
        return interventionRepository.save(i);
    }

    @Autowired
    private com.edupulse.repository.FollowUpRepository followUpRepository;

    public com.edupulse.model.FollowUp addFollowUp(Long interventionId, com.edupulse.model.FollowUp followUp) {
        Intervention i = interventionRepository.findById(interventionId).orElseThrow();
        followUp.setIntervention(i);
        followUp.setCreatedAt(java.time.LocalDateTime.now());
        return followUpRepository.save(followUp);
    }
}
