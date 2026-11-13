package com.edupulse.repository;

import com.edupulse.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    java.util.List<Enrollment> findByCourseId(Long courseId);
    java.util.List<Enrollment> findByStudentId(Long studentId);
}
