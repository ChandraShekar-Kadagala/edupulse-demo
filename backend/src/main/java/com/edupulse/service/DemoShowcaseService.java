package com.edupulse.service;

import com.edupulse.model.*;
import com.edupulse.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Public, read-only summary of the six DEMO showcase accounts for the login page.
 * Every figure is read from the database (latest risk snapshot, recorded attendance and marks,
 * course assignments). Only accounts whose auth provider is DEMO are ever included, and the
 * endpoint never creates snapshots or other data.
 */
@Service
@RequiredArgsConstructor
public class DemoShowcaseService {

    public static final List<String> STUDENT_USERNAMES =
            List.of("demo.student.a", "demo.student.b", "demo.student.c", "demo.student.d");
    public static final List<String> FACULTY_USERNAMES = List.of("demo.faculty.1", "demo.faculty.2");

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;

    private Optional<User> demoUser(String username) {
        return userRepository.findByUsername(username).filter(u -> u.getAuthProvider() == AuthProvider.DEMO);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> students() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String username : STUDENT_USERNAMES) {
            demoUser(username).flatMap(u -> studentRepository.findById(u.getId())).ifPresent(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("username", username);
                m.put("regNo", s.getStudentNumber());
                m.put("name", s.getUser().getName());
                m.put("role", "STUDENT");
                m.put("cgpa", s.getCurrentCgpa());

                Optional<RiskAssessment> risk = riskAssessmentRepository.findFirstByStudentIdOrderByCalculatedAtDesc(s.getId());
                m.put("risk", risk.map(r -> r.getRiskLevel().name()).orElse(null));
                m.put("riskScore", risk.map(RiskAssessment::getRiskScore).orElse(null));
                m.put("riskCalculatedAt", risk.map(RiskAssessment::getCalculatedAt).orElse(null));

                List<Attendance> att = attendanceRepository.findByStudentId(s.getId());
                long present = att.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
                m.put("attendance", att.isEmpty() ? null
                        : String.format(Locale.US, "%.1f%% (%d/%d)", (double) present / att.size() * 100.0, present, att.size()));

                List<Mark> graded = markRepository.findByStudentId(s.getId()).stream()
                        .filter(mk -> mk.getMarksObtained() != null && mk.getAssessment() != null
                                && mk.getAssessment().getMaxMarks() != null && mk.getAssessment().getMaxMarks() > 0)
                        .toList();
                m.put("evaluatedAssessments", graded.size());
                m.put("averageMarksPercentage", graded.isEmpty() ? null : Math.round(graded.stream()
                        .mapToDouble(mk -> mk.getMarksObtained() / mk.getAssessment().getMaxMarks() * 100.0)
                        .average().orElseThrow() * 10.0) / 10.0);
                m.put("subjects", enrollmentRepository.findByStudentId(s.getId()).size());
                m.put("provenance", RuleBasedRiskEngineImpl.provenanceLabel(s));
                out.add(m);
            });
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> faculty() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String username : FACULTY_USERNAMES) {
            demoUser(username).flatMap(u -> facultyRepository.findById(u.getId())).ifPresent(f -> {
                List<CourseAssignment> assignments = courseAssignmentRepository.findByFacultyIdWithCourse(f.getId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("username", username);
                m.put("name", f.getUser().getName());
                m.put("role", "FACULTY");
                m.put("title", f.getTitle());
                m.put("courses", assignments.stream()
                        .map(a -> { Map<String, Object> c = new LinkedHashMap<>(); c.put("code", a.getCourse().getCode()); c.put("name", a.getCourse().getName()); return c; })
                        .toList());
                out.add(m);
            });
        }
        return out;
    }
}
