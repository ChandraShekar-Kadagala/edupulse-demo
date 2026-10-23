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
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public String getProviderName() {
        return "Synthetic Demo Fixture Provider";
    }

    @Override
    public String getProviderStatus() {
        return "AVAILABLE_FOR_TESTING";
    }

    @Override
    public DataProvenance getProvenance() {
        return DataProvenance.SYNTHETIC_TEST_FIXTURE;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    @Transactional
    public void syncStudentData(String studentIdStr) {
        // Find the student
        Student student = studentRepository.findByStudentNumber(studentIdStr)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        // Simulate fetching records from an external API (Fixture Data)
        Course c = courseRepository.findByCode("CS-DEMO").orElseGet(() -> {
            Course newCourse = Course.builder().code("CS-DEMO").name("Integration Testing (Synthetic Demo)").credits(3).build();
            return courseRepository.save(newCourse);
        });

        // Create or find an assessment from the "external API"
        Assessment assessment = assessmentRepository.findByCourseIdAndName(c.getId(), "Demo Sync Exam (Synthetic)")
                .orElseGet(() -> {
                    Assessment newAssessment = Assessment.builder()
                            .name("Demo Sync Exam (Synthetic)")
                            .maxMarks(100.0)
                            .course(c)
                            .type(AssessmentType.INTERNAL)
                            .build();
                    return assessmentRepository.save(newAssessment);
                });

        // Validate and prevent duplicates / handle updates
        Optional<Mark> existingMarkOpt = markRepository.findByStudentIdAndAssessmentId(student.getId(), assessment.getId());

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
        for (int i = 0; i < 9; i++) {
            LocalDate d = LocalDate.now().minusDays(i);
            Optional<Attendance> existing = attendanceRepository.findByStudentIdAndCourseIdAndDate(student.getId(), c.getId(), d);
            if (existing.isEmpty()) {
                attendanceRepository.save(Attendance.builder().student(student).course(c).date(d).status(AttendanceStatus.PRESENT).build());
            }
        }
        LocalDate d9 = LocalDate.now().minusDays(9);
        Optional<Attendance> existing9 = attendanceRepository.findByStudentIdAndCourseIdAndDate(student.getId(), c.getId(), d9);
        if (existing9.isEmpty()) {
            attendanceRepository.save(Attendance.builder().student(student).course(c).date(d9).status(AttendanceStatus.ABSENT).build());
        }

        // Trigger historical risk snapshot
        riskAssessmentService.calculateRiskForStudent(student.getId());

        // Log synthetic fixture sync in audit log
        auditLogRepository.save(AuditLog.builder()
                .action("SYNTHETIC_DEMO_SYNC")
                .userId(student.getId())
                .details("Synthetic test fixture synced for student: " + student.getStudentNumber() + ", provenance=" + DataProvenance.SYNTHETIC_TEST_FIXTURE)
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }

    @Override
    public boolean supportsAutoSync() {
        return true;
    }
}
