package com.edupulse.controller;

import com.edupulse.dto.FacultyDto;
import com.edupulse.service.FacultyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    @Autowired
    private FacultyService facultyService;

    @GetMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<List<FacultyDto>> getAllFaculty() {
        return ResponseEntity.ok(facultyService.getAllFacultyProfiles());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<FacultyDto> getFacultyById(@PathVariable Long id) {
        return ResponseEntity.ok(facultyService.getFacultyById(id));
    }
}
