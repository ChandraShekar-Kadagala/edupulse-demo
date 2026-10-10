package com.edupulse.repository;

import com.edupulse.model.Intervention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterventionRepository extends JpaRepository<Intervention, Long> {

    java.util.List<Intervention> findByStudentId(Long studentId);
    java.util.List<Intervention> findByAssignedFacultyId(Long facultyId);
}

