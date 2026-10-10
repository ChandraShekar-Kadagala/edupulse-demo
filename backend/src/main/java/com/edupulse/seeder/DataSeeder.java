package com.edupulse.seeder;

import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.service.RiskAssessmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@org.springframework.context.annotation.Profile("!prod")
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository submissionRepository;
    
    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return;
        }

        // Create Admin
        User adminUser = User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .name("Admin User")
                .email("admin@edupulse.edu")
                .build();
        userRepository.save(adminUser);

        // Create Faculty
        User facultyUser = User.builder()
                .username("faculty")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.FACULTY)
                .name("Dr. Smith")
                .email("smith@edupulse.edu")
                .build();
        userRepository.save(facultyUser);

        // Create Showcase Student: Arun Kumar
        User arunUser = User.builder()
                .username("arun")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .name("Arun Kumar")
                .email("arun@edupulse.edu")
                .build();
        arunUser = userRepository.save(arunUser);

        Student arun = Student.builder()
                .user(arunUser)
                .studentNumber("STU-1001")
                .department("Computer Science")
                .currentSemester(3)
                .currentCgpa(6.5)
                .build();
        studentRepository.save(arun);

        // Seed some data for Arun to trigger high risk
        Course cse101 = Course.builder().code("CSE101").name("Intro to CS").credits(3).build();
        courseRepository.save(cse101);

        // Low Attendance
        for (int i = 1; i <= 10; i++) {
            Attendance att = Attendance.builder()
                    .student(arun)
                    .course(cse101)
                    .date(LocalDate.now().minusDays(i))
                    .status(i <= 3 ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT) // 30% attendance
                    .build();
            attendanceRepository.save(att);
        }

        // Low Marks
        Assessment midterm = Assessment.builder().course(cse101).name("Midterm").maxMarks(100.0).type(AssessmentType.INTERNAL).build();
        assessmentRepository.save(midterm);
        Mark m = Mark.builder().student(arun).assessment(midterm).marksObtained(30.0).build();
        markRepository.save(m);
        // I will just save the Mark, wait I need AssessmentRepository. Let me check if it's there. 
        // Yes, AssessmentRepository exists.
        
        // Let's create Assignment for missing submission
        Assignment a1 = Assignment.builder().course(cse101).name("Homework 1").dueDate(LocalDate.now().minusDays(2)).build();
        assignmentRepository.save(a1);

        AssignmentSubmission s1 = AssignmentSubmission.builder().assignment(a1).student(arun).submittedAt(null).status("MISSING").build();
        submissionRepository.save(s1);

        // Calculate risk
        riskAssessmentService.calculateRiskForStudent(arun.getId());
    }
}
