package com.edupulse.repository;

import com.edupulse.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    java.util.Optional<Student> findByStudentNumber(String studentNumber);
}
