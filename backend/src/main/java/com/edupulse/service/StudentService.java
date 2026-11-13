package com.edupulse.service;

import com.edupulse.model.Student;
import com.edupulse.repository.StudentRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@lombok.RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }
    
    public Student getStudentById(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new com.edupulse.exception.ResourceNotFoundException("Student", id));
    }
}
