package com.edupulse.seeder;

import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.service.RiskAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@Transactional
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FacultyRepository facultyRepository;

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

    @Value("${app.seeder.demo-password:DemoEduPulse2026!}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedDemoData();
    }

    /**
     * Idempotent, transaction-safe demo showcase bootstrapper.
     * Safely aborts if ANY existing application records are present.
     * Never overwrites, deletes, or modifies existing users or data.
     *
     * @return true if demo data was seeded; false if aborted due to existing records
     */
    @Transactional
    public boolean seedDemoData() {
        long userCount = userRepository.count();
        long studentCount = studentRepository.count();
        long facultyCount = facultyRepository.count();

        if (userCount > 0 || studentCount > 0 || facultyCount > 0) {
            log.warn("Demo showcase bootstrap skipped: Database already contains records (users={}, students={}, faculty={}). Existing data preserved without modification.",
                    userCount, studentCount, facultyCount);
            return false;
        }

        if (courseRepository.count() > 0 || attendanceRepository.count() > 0 || markRepository.count() > 0) {
            log.warn("Demo showcase bootstrap skipped: Academic tables already contain records. Aborting without modification.");
            return false;
        }

        log.info("Starting safe demo showcase bootstrap with dedicated demo identities...");

        String encodedPassword = passwordEncoder.encode(demoPassword);

        // 1. Create Demo Admin User
        User adminUser = User.builder()
                .username("admin")
                .passwordHash(encodedPassword)
                .role(Role.ADMIN)
                .name("System Administrator (Demo)")
                .email("demo.admin@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build();
        userRepository.save(adminUser);

        // 2. Create Demo Faculty User
        User facultyUser = User.builder()
                .username("faculty")
                .passwordHash(encodedPassword)
                .role(Role.FACULTY)
                .name("Dr. Sarah Smith (Demo Faculty)")
                .email("demo.faculty@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build();
        userRepository.save(facultyUser);

        // 3. Create Showcase Student User & Student Record: Arun Kumar
        User arunUser = User.builder()
                .username("arun")
                .passwordHash(encodedPassword)
                .role(Role.STUDENT)
                .name("Arun Kumar (Demo Student)")
                .email("demo.student@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build();
        arunUser = userRepository.save(arunUser);

        Student arun = Student.builder()
                .user(arunUser)
                .studentNumber("STU-1001")
                .department("Computer Science & Engineering")
                .currentSemester(3)
                .currentCgpa(6.5)
                .build();
        studentRepository.save(arun);

        // 4. Seed illustrative academic scenario (poor attendance, declining marks, HIGH risk)
        Course cse101 = Course.builder()
                .code("CSE101")
                .name("Intro to CS (Demo Fixture)")
                .credits(3)
                .build();
        courseRepository.save(cse101);

        // Attendance: 30% attendance (3 Present, 7 Absent)
        for (int i = 1; i <= 10; i++) {
            Attendance att = Attendance.builder()
                    .student(arun)
                    .course(cse101)
                    .date(LocalDate.now().minusDays(i))
                    .status(i <= 3 ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT)
                    .build();
            attendanceRepository.save(att);
        }

        // Assessment & Marks: Low midterm score (30/100)
        Assessment midterm = Assessment.builder()
                .course(cse101)
                .name("Midterm Exam (Demo)")
                .maxMarks(100.0)
                .type(AssessmentType.INTERNAL)
                .build();
        assessmentRepository.save(midterm);

        Mark mark = Mark.builder()
                .student(arun)
                .assessment(midterm)
                .marksObtained(30.0)
                .build();
        markRepository.save(mark);

        // Assignment & Submission: Missing homework
        Assignment a1 = Assignment.builder()
                .course(cse101)
                .name("Homework 1 (Demo)")
                .dueDate(LocalDate.now().minusDays(2))
                .build();
        assignmentRepository.save(a1);

        AssignmentSubmission s1 = AssignmentSubmission.builder()
                .assignment(a1)
                .student(arun)
                .submittedAt(null)
                .status("MISSING")
                .build();
        submissionRepository.save(s1);

        // Calculate initial risk assessment for Arun
        riskAssessmentService.calculateRiskForStudent(arun.getId());

        log.info("Demo showcase bootstrap completed successfully. Designated demo accounts initialized with AuthProvider.DEMO.");
        return true;
    }
}
