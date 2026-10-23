package com.edupulse.repository;

import com.edupulse.model.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Optional<Assessment> findByCourseIdAndName(Long courseId, String name);
    List<Assessment> findByCourseId(Long courseId);
}
