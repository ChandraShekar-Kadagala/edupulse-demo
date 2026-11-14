package com.edupulse.repository;

import com.edupulse.model.AttendanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceSnapshotRepository extends JpaRepository<AttendanceSnapshot, Long> {
    List<AttendanceSnapshot> findByStudentId(Long studentId);
    Optional<AttendanceSnapshot> findByStudentIdAndCourseIdAndAsOfDate(Long studentId, Long courseId, java.time.LocalDate asOfDate);
    List<AttendanceSnapshot> findByBatchId(Long batchId);
    void deleteByBatchId(Long batchId);
}
