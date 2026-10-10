package com.edupulse.repository;

import com.edupulse.model.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    java.util.Optional<RiskAssessment> findFirstByStudentIdOrderByCalculatedAtDesc(Long studentId);
    java.util.List<RiskAssessment> findByStudentId(Long studentId);
}

