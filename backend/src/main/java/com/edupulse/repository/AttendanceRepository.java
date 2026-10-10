package com.edupulse.repository;

import com.edupulse.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    java.util.List<Attendance> findByStudentIdAndDateAfter(Long studentId, java.time.LocalDate date);
    java.util.List<Attendance> findByStudentId(Long studentId);
    java.util.List<Attendance> findByStudentIdAndCourseId(Long studentId, Long courseId);
}

