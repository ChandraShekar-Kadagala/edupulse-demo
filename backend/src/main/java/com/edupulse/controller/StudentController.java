package com.edupulse.controller;

import com.edupulse.dto.RiskAssessmentResponse;
import com.edupulse.model.Student;
import com.edupulse.service.RiskAssessmentService;
import com.edupulse.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping("/api/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @GetMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<List<com.edupulse.dto.StudentDto>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents().stream().map(this::mapToDto).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<com.edupulse.dto.StudentDto> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(mapToDto(studentService.getStudentById(id)));
    }
    
    private com.edupulse.dto.StudentDto mapToDto(Student student) {
        return com.edupulse.dto.StudentDto.builder()
                .id(student.getUser().getId())
                .name(student.getUser().getName())
                .email(student.getUser().getEmail())
                .studentNumber(student.getStudentNumber())
                .department(student.getDepartment())
                .currentSemester(student.getCurrentSemester())
                .currentCgpa(student.getCurrentCgpa())
                .build();
    }
    
    @GetMapping("/{id}/risk")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<RiskAssessmentResponse> getStudentRisk(@PathVariable Long id) {
        return ResponseEntity.ok(riskAssessmentService.getLatestRiskAssessment(id));
    }

    @PostMapping("/{id}/risk/recalculate")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<?> recalculateStudentRisk(@PathVariable Long id) {
        riskAssessmentService.calculateRiskForStudent(id);
        return ResponseEntity.ok(riskAssessmentService.getLatestRiskAssessment(id));
    }
}
