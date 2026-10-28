package com.edupulse.controller;

import com.edupulse.model.Intervention;
import com.edupulse.service.InterventionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping("/api/interventions")
public class InterventionController {
    
    @Autowired
    private InterventionService interventionService;

    @Autowired
    private com.edupulse.repository.StudentRepository studentRepository;
    
    @Autowired
    private com.edupulse.repository.FacultyRepository facultyRepository;

    @GetMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<List<com.edupulse.dto.InterventionDto>> getAllInterventions() {
        List<com.edupulse.dto.InterventionDto> dtos = interventionService.getAllInterventions().stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or #studentId == principal.id")
    public ResponseEntity<List<com.edupulse.dto.InterventionDto>> getInterventionsForStudent(@PathVariable Long studentId) {
        List<com.edupulse.dto.InterventionDto> dtos = interventionService.getInterventionsForStudent(studentId).stream().map(this::mapToDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<com.edupulse.dto.InterventionDto> createIntervention(@RequestBody com.edupulse.dto.InterventionRequest request) {
        Intervention intervention = new Intervention();
        intervention.setStudent(studentRepository.findById(request.getStudentId()).orElseThrow());
        intervention.setAssignedFaculty(facultyRepository.findById(request.getAssignedFacultyId()).orElseThrow());
        intervention.setType(request.getType());
        intervention.setDescription(request.getDescription());
        intervention.setPriority(request.getPriority());
        intervention.setDueDate(request.getDueDate());
        intervention.setStatus(com.edupulse.model.InterventionStatus.PLANNED);
        intervention.setCreatedAt(java.time.LocalDateTime.now());
        
        Intervention saved = interventionService.createIntervention(intervention);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<com.edupulse.dto.InterventionDto> updateStatus(@PathVariable Long id, @RequestParam com.edupulse.model.InterventionStatus status) {
        return ResponseEntity.ok(mapToDto(interventionService.updateInterventionStatus(id, status)));
    }

    @PostMapping("/{id}/followups")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<com.edupulse.dto.FollowUpDto> addFollowUp(@PathVariable Long id, @RequestBody com.edupulse.dto.FollowUpRequest request) {
        com.edupulse.model.FollowUp f = new com.edupulse.model.FollowUp();
        f.setNotes(request.getNotes());
        f.setOutcome(request.getOutcome());
        f.setFaculty(facultyRepository.findById(request.getFacultyId()).orElseThrow());
        com.edupulse.model.FollowUp saved = interventionService.addFollowUp(id, f);
        return ResponseEntity.ok(mapFollowUpToDto(saved));
    }

    private com.edupulse.dto.InterventionDto mapToDto(Intervention i) {
        return com.edupulse.dto.InterventionDto.builder()
                .id(i.getId())
                .studentId(i.getStudent() != null ? i.getStudent().getUser().getId() : null)
                .studentName(i.getStudent() != null ? i.getStudent().getUser().getName() : null)
                .assignedFacultyId(i.getAssignedFaculty() != null ? i.getAssignedFaculty().getUser().getId() : null)
                .assignedFacultyName(i.getAssignedFaculty() != null ? i.getAssignedFaculty().getUser().getName() : null)
                .type(i.getType())
                .description(i.getDescription())
                .status(i.getStatus())
                .priority(i.getPriority())
                .startDate(i.getStartDate())
                .dueDate(i.getDueDate())
                .createdAt(i.getCreatedAt())
                .followUps(i.getFollowUps() != null ? i.getFollowUps().stream().map(this::mapFollowUpToDto).toList() : null)
                .build();
    }
    
    private com.edupulse.dto.FollowUpDto mapFollowUpToDto(com.edupulse.model.FollowUp f) {
        boolean isStudent = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        return com.edupulse.dto.FollowUpDto.builder()
                .id(f.getId())
                .interventionId(f.getIntervention() != null ? f.getIntervention().getId() : null)
                .facultyId(f.getFaculty() != null ? f.getFaculty().getUser().getId() : null)
                .facultyName(f.getFaculty() != null ? f.getFaculty().getUser().getName() : null)
                .notes(isStudent ? "Faculty notes hidden" : f.getNotes())
                .outcome(f.getOutcome())
                .previousRiskScore(f.getPreviousRiskScore())
                .newRiskScore(f.getNewRiskScore())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
