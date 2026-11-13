package com.edupulse.controller;

import com.edupulse.dto.InterventionDto;
import com.edupulse.dto.RiskAssessmentResponse;
import com.edupulse.dto.StudentAcademicAnalyticsDto;
import com.edupulse.dto.StudentDto;
import com.edupulse.dto.UserDto;
import com.edupulse.model.Attendance;
import com.edupulse.model.AttendanceStatus;
import com.edupulse.model.Intervention;
import com.edupulse.model.Mark;
import com.edupulse.model.Student;
import com.edupulse.repository.AttendanceRepository;
import com.edupulse.repository.MarkRepository;
import com.edupulse.service.AcademicAnalyticsService;
import com.edupulse.service.InterventionService;
import com.edupulse.service.RiskAssessmentService;
import com.edupulse.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping("/api/students")
@lombok.RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    private final RiskAssessmentService riskAssessmentService;

    private final AcademicAnalyticsService academicAnalyticsService;

    private final InterventionService interventionService;

    private final AttendanceRepository attendanceRepository;

    private final MarkRepository markRepository;

    private final com.edupulse.repository.RiskAssessmentRepository riskAssessmentRepository;

    @GetMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<List<StudentDto>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents().stream().map(this::mapToDto).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<StudentDto> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(mapToDto(studentService.getStudentById(id)));
    }

    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<StudentAcademicAnalyticsDto> getStudentAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(academicAnalyticsService.getStudentAnalytics(id));
    }

    @GetMapping("/{id}/risk")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<RiskAssessmentResponse> getStudentRisk(@PathVariable Long id) {
        return ResponseEntity.ok(riskAssessmentService.getLatestRiskAssessment(id));
    }

    @PostMapping({"/{id}/risk/recalculate", "/{id}/risk/assess"})
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<?> recalculateStudentRisk(@PathVariable Long id) {
        riskAssessmentService.calculateRiskForStudent(id);
        return ResponseEntity.ok(riskAssessmentService.getLatestRiskAssessment(id));
    }

    /** Official risk snapshots over time plus intervention start dates (timeline for trends and outcomes). */
    @GetMapping("/{id}/risk/history")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<java.util.Map<String, Object>> getRiskHistory(@PathVariable Long id) {
        studentService.getStudentById(id); // 404 if unknown
        var snapshots = riskAssessmentRepository.findByStudentId(id).stream()
                .filter(r -> r.getCalculatedAt() != null)
                .sorted(java.util.Comparator.comparing(com.edupulse.model.RiskAssessment::getCalculatedAt))
                .map(r -> java.util.Map.<String, Object>of(
                        "calculatedAt", r.getCalculatedAt(),
                        "riskScore", r.getRiskScore(),
                        "riskLevel", r.getRiskLevel().name()))
                .toList();
        var interventions = interventionService.getInterventionsForStudent(id).stream()
                .sorted(java.util.Comparator.comparing(Intervention::getId))
                .map(i -> { var m = new java.util.LinkedHashMap<String, Object>();
                    m.put("id", i.getId()); m.put("type", i.getType());
                    m.put("status", i.getStatus() != null ? i.getStatus().name() : null);
                    m.put("startedAt", InterventionService.startOf(i)); return m; })
                .toList();
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("studentId", id);
        body.put("snapshots", snapshots);
        body.put("interventions", interventions);
        body.put("note", "Snapshots are appended when an official recalculation changes the result; a single snapshot is not a trend.");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}/interventions")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<List<InterventionDto>> getStudentInterventions(@PathVariable Long id) {
        List<InterventionDto> dtos = interventionService.getInterventionsForStudent(id).stream()
                .map(this::mapInterventionToDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    private StudentDto mapToDto(Student student) {
        UserDto userDto = student.getUser() != null ? UserDto.builder()
                .id(student.getUser().getId())
                .username(student.getUser().getUsername())
                .name(student.getUser().getName())
                .email(student.getUser().getEmail())
                .role(student.getUser().getRole())
                .authProvider(student.getUser().getAuthProvider())
                .build() : null;

        Long studentId = student.getUser() != null ? student.getUser().getId() : student.getId();

        List<Attendance> attendances = attendanceRepository.findByStudentId(studentId);
        Double attendancePercentage = null;
        Long totalClasses = null;
        Long presentClasses = null;
        if (attendances != null && !attendances.isEmpty()) {
            totalClasses = (long) attendances.size();
            presentClasses = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            attendancePercentage = Math.round(((double) presentClasses / totalClasses * 100.0) * 10.0) / 10.0;
        }

        List<Mark> marks = markRepository.findByStudentId(studentId);
        Double avgMarks = null;
        if (marks != null && !marks.isEmpty()) {
            double avg = marks.stream()
                    .filter(m -> m.getAssessment() != null && m.getAssessment().getMaxMarks() > 0)
                    .mapToDouble(m -> (m.getMarksObtained() / m.getAssessment().getMaxMarks()) * 100.0)
                    .average().orElse(0.0);
            avgMarks = Math.round(avg * 10.0) / 10.0;
        }

        RiskAssessmentResponse risk = null;
        try {
            risk = riskAssessmentService.getLatestRiskAssessment(studentId);
        } catch (Exception ignored) {
        }

        String riskLevel = risk != null && risk.getRiskLevel() != null ? risk.getRiskLevel().name() : null;
        Double riskScore = risk != null ? risk.getRiskScore() : null;
        String riskReason = (risk != null && risk.getRiskFactors() != null && !risk.getRiskFactors().isEmpty())
                ? risk.getRiskFactors().get(0).getDescription()
                : null;

        return StudentDto.builder()
                .id(studentId)
                .name(student.getUser() != null ? student.getUser().getName() : null)
                .email(student.getUser() != null ? student.getUser().getEmail() : null)
                .studentNumber(student.getStudentNumber())
                .department(student.getDepartment())
                .currentSemester(student.getCurrentSemester())
                .currentCgpa(student.getCurrentCgpa())
                .user(userDto)
                .riskLevel(riskLevel)
                .riskScore(riskScore)
                .riskReason(riskReason)
                .attendancePercentage(attendancePercentage)
                .averageMarksPercentage(avgMarks)
                .totalClasses(totalClasses)
                .presentClasses(presentClasses)
                .build();
    }

    private InterventionDto mapInterventionToDto(Intervention i) {
        return InterventionDto.builder()
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
                .build();
    }
}
