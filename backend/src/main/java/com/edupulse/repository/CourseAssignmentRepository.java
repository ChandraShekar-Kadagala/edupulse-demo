package com.edupulse.repository;

import com.edupulse.model.CourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {

    @Query("select ca from CourseAssignment ca join fetch ca.course where ca.faculty.id = :facultyId order by ca.course.code")
    List<CourseAssignment> findByFacultyIdWithCourse(@Param("facultyId") Long facultyId);

    boolean existsByFacultyIdAndCourseId(Long facultyId, Long courseId);
}
