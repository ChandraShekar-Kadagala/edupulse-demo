package com.edupulse.repository;

import com.edupulse.model.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    java.util.List<AssignmentSubmission> findByStudentId(Long studentId);
}
