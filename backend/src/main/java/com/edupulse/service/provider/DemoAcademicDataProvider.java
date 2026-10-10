package com.edupulse.service.provider;

import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.service.RiskAssessmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DemoAcademicDataProvider implements AcademicDataProvider {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private AssessmentRepository assessmentRepository;
    @Autowired
    private MarkRepository markRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Override
    public String getProviderStatus() {
        return "AVAILABLE_FOR_TESTING";
    }

    @Override
    @Transactional
    public void syncStudentData(String studentIdStr) {
        // Find the student
        Student student = studentRepository.findByStudentNumber(studentIdStr)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        // Simulate fetching records from an external API (Fixture Data)
        Course c = courseRepository.findById(1L).orElseGet(() -> {
            Course newCourse = Course.builder().code("CS-DEMO").name("Integration Testing").credits(3).build();
            return courseRepository.save(newCourse);
        });

        // Create or find an assessment from the "external API"
        Assessment assessment = assessmentRepository.findAll().stream()
                .filter(a -> a.getName().equals("Demo Sync Exam") && a.getCourse().getId().equals(c.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Assessment newAssessment = Assessment.builder()
                            .name("Demo Sync Exam")
                            .maxMarks(100.0)
                            .course(c)
                            .type(AssessmentType.INTERNAL)
                            .build();
                    return assessmentRepository.save(newAssessment);
                });

        // Validate and prevent duplicates / handle updates
        List<Mark> existingMarks = markRepository.findByStudentId(student.getId());
        Optional<Mark> existingMarkOpt = existingMarks.stream()
                .filter(m -> m.getAssessment().getId().equals(assessment.getId()))
                .findFirst();

        if (existingMarkOpt.isPresent()) {
            // Update existing mark if different
            Mark existing = existingMarkOpt.get();
            if (existing.getMarksObtained() != 85.0) {
                existing.setMarksObtained(85.0);
                markRepository.save(existing);
            }
        } else {
            // Insert new mark
            Mark m = Mark.builder()
                    .student(student)
                    .assessment(assessment)
                    .marksObtained(85.0)
                    .build();
            markRepository.save(m);
        }
            
        // Sync attendance idempotently
        for(int i = 0; i < 9; i++) {
            LocalDate d = LocalDate.now().minusDays(i);
            if (attendanceRepository.findByStudentIdAndCourseId(student.getId(), c.getId()).stream().noneMatch(a -> a.getDate().equals(d))) {
                attendanceRepository.save(Attendance.builder().student(student).course(c).date(d).status(AttendanceStatus.PRESENT).build());
            }
        }
        LocalDate d9 = LocalDate.now().minusDays(9);
        if (attendanceRepository.findByStudentIdAndCourseId(student.getId(), c.getId()).stream().noneMatch(a -> a.getDate().equals(d9))) {
            attendanceRepository.save(Attendance.builder().student(student).course(c).date(d9).status(AttendanceStatus.ABSENT).build());
        }

        // Trigger historical risk snapshot
        riskAssessmentService.calculateRiskForStudent(student.getId());
    }

    @Override
    public boolean supportsAutoSync() {
        return true;
    }
}
