package com.edupulse.repository;

import com.edupulse.model.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByStudentId(Long studentId);
    Optional<ExamResult> findByStudentIdAndCourseIdAndSemesterAndAttempt(Long studentId, Long courseId, String semester, int attempt);
    void deleteByBatchId(Long batchId);
}
