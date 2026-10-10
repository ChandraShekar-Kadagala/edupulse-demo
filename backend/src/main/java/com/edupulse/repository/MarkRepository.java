package com.edupulse.repository;

import com.edupulse.model.Mark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {

    java.util.List<Mark> findByStudentId(Long studentId);
}

