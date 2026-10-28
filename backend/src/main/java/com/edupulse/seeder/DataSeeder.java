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
    private InterventionRepository interventionRepository;

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

        if (courseRepository.count() > 0 || attendanceRepository.count() > 0 || markRepository.count() > 0 || interventionRepository.count() > 0) {
            log.warn("Demo showcase bootstrap skipped: Academic or intervention tables already contain records. Aborting without modification.");
            return false;
        }

        log.info("Starting safe demo showcase bootstrap with 4-student cohort and dedicated demo identities...");

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

        // 2. Create Demo Faculty User & Profile
        User facultyUser = User.builder()
                .username("faculty")
                .passwordHash(encodedPassword)
                .role(Role.FACULTY)
                .name("Dr. Sarah Smith (Demo Faculty)")
                .email("demo.faculty@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build();
        facultyUser = userRepository.save(facultyUser);

        Faculty faculty = Faculty.builder()
                .user(facultyUser)
                .department("Computer Science & Engineering")
                .title("Associate Professor")
                .build();
        faculty = facultyRepository.save(faculty);

        // 3. Shared Demo Course
        Course cse101 = Course.builder()
                .code("CSE101")
                .name("Data Structures & Algorithms (Demo Fixture)")
                .credits(4)
                .build();
        cse101 = courseRepository.save(cse101);

        // Assessments for CSE101
        Assessment midterm = Assessment.builder()
                .course(cse101)
                .name("Midterm Exam (Demo)")
                .maxMarks(100.0)
                .type(AssessmentType.INTERNAL)
                .build();
        midterm = assessmentRepository.save(midterm);

        Assessment quiz = Assessment.builder()
                .course(cse101)
                .name("Quiz 1 (Demo)")
                .maxMarks(50.0)
                .type(AssessmentType.INTERNAL)
                .build();
        quiz = assessmentRepository.save(quiz);

        // Assignments for CSE101
        Assignment a1 = Assignment.builder()
                .course(cse101)
                .name("Assignment 1: Linked Lists (Demo)")
                .dueDate(LocalDate.now().minusDays(10))
                .build();
        a1 = assignmentRepository.save(a1);

        Assignment a2 = Assignment.builder()
                .course(cse101)
                .name("Assignment 2: Binary Search Trees (Demo)")
                .dueDate(LocalDate.now().minusDays(5))
                .build();
        a2 = assignmentRepository.save(a2);

        Assignment a3 = Assignment.builder()
                .course(cse101)
                .name("Assignment 3: Graph Traversal (Demo)")
                .dueDate(LocalDate.now().minusDays(3))
                .build();
        a3 = assignmentRepository.save(a3);

        Assignment a4 = Assignment.builder()
                .course(cse101)
                .name("Assignment 4: Hash Maps (Demo)")
                .dueDate(LocalDate.now().minusDays(1))
                .build();
        a4 = assignmentRepository.save(a4);

        // -------------------------------------------------------------
        // Student A: Aarav Sharma (Top Performer / LOW Risk)
        // 95% attendance (19/20), 92% marks, 0 missing -> Risk Score 0.0
        // -------------------------------------------------------------
        User aaravUser = userRepository.save(User.builder()
                .username("aarav")
                .passwordHash(encodedPassword)
                .role(Role.STUDENT)
                .name("Aarav Sharma (Demo Student)")
                .email("aarav.sharma@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        Student aarav = studentRepository.save(Student.builder()
                .user(aaravUser)
                .studentNumber("STU-1001")
                .department("Computer Science & Engineering")
                .currentSemester(5)
                .currentCgpa(9.4)
                .build());

        for (int i = 1; i <= 20; i++) {
            attendanceRepository.save(Attendance.builder()
                    .student(aarav)
                    .course(cse101)
                    .date(LocalDate.now().minusDays(i))
                    .status(i == 7 ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT) // 19 Present, 1 Absent = 95%
                    .build());
        }

        markRepository.save(Mark.builder().student(aarav).assessment(midterm).marksObtained(92.0).build());
        markRepository.save(Mark.builder().student(aarav).assessment(quiz).marksObtained(46.0).build());

        submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(aarav).submittedAt(LocalDate.now().minusDays(12)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(aarav).submittedAt(LocalDate.now().minusDays(6)).status("SUBMITTED").build());

        // -------------------------------------------------------------
        // Student B: Bhavna Patel (Above-Average / LOW Risk)
        // 80% attendance (16/20), 79% marks, 1 late submission -> Risk Score 5.0
        // -------------------------------------------------------------
        User bhavnaUser = userRepository.save(User.builder()
                .username("bhavna")
                .passwordHash(encodedPassword)
                .role(Role.STUDENT)
                .name("Bhavna Patel (Demo Student)")
                .email("bhavna.patel@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        Student bhavna = studentRepository.save(Student.builder()
                .user(bhavnaUser)
                .studentNumber("STU-1002")
                .department("Computer Science & Engineering")
                .currentSemester(4)
                .currentCgpa(7.9)
                .build());

        for (int i = 1; i <= 20; i++) {
            attendanceRepository.save(Attendance.builder()
                    .student(bhavna)
                    .course(cse101)
                    .date(LocalDate.now().minusDays(i))
                    .status(i <= 4 ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT) // 16 Present, 4 Absent = 80%
                    .build());
        }

        markRepository.save(Mark.builder().student(bhavna).assessment(midterm).marksObtained(78.0).build());
        markRepository.save(Mark.builder().student(bhavna).assessment(quiz).marksObtained(40.0).build());

        submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(bhavna).submittedAt(LocalDate.now().minusDays(11)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(bhavna).submittedAt(LocalDate.now().minusDays(4)).status("LATE").build());

        // -------------------------------------------------------------
        // Student C: Chirag Verma (Average / MEDIUM Risk)
        // 65% attendance (13/20 -> 15.0 pts), 44.5% marks (5.5 pts), 4 missing assignments (20 pts) -> Risk Score 40.5
        // -------------------------------------------------------------
        User chiragUser = userRepository.save(User.builder()
                .username("chirag")
                .passwordHash(encodedPassword)
                .role(Role.STUDENT)
                .name("Chirag Verma (Demo Student)")
                .email("chirag.verma@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        Student chirag = studentRepository.save(Student.builder()
                .user(chiragUser)
                .studentNumber("STU-1003")
                .department("Computer Science & Engineering")
                .currentSemester(3)
                .currentCgpa(6.2)
                .build());

        for (int i = 1; i <= 20; i++) {
            attendanceRepository.save(Attendance.builder()
                    .student(chirag)
                    .course(cse101)
                    .date(LocalDate.now().minusDays(i))
                    .status(i <= 7 ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT) // 13 Present, 7 Absent = 65%
                    .build());
        }

        markRepository.save(Mark.builder().student(chirag).assessment(midterm).marksObtained(45.0).build());
        markRepository.save(Mark.builder().student(chirag).assessment(quiz).marksObtained(22.0).build());

        submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(chirag).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(chirag).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(chirag).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(chirag).submittedAt(null).status("MISSING").build());

        // -------------------------------------------------------------
        // Student D: Arun Kumar (Struggling / HIGH Risk)
        // 30% attendance (6/20 -> capped 40.0 pts), 24.5% marks (25.5 pts), 2 missing assignments (10 pts) -> Risk Score 75.5
        // -------------------------------------------------------------
        User arunUser = userRepository.save(User.builder()
                .username("arun")
                .passwordHash(encodedPassword)
                .role(Role.STUDENT)
                .name("Arun Kumar (Demo Student)")
                .email("demo.student@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        Student arun = studentRepository.save(Student.builder()
                .user(arunUser)
                .studentNumber("STU-1004")
                .department("Computer Science & Engineering")
                .currentSemester(3)
                .currentCgpa(5.1)
                .build());

        for (int i = 1; i <= 20; i++) {
            attendanceRepository.save(Attendance.builder()
                    .student(arun)
                    .course(cse101)
                    .date(LocalDate.now().minusDays(i))
                    .status(i <= 6 ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT) // 6 Present, 14 Absent = 30%
                    .build());
        }

        markRepository.save(Mark.builder().student(arun).assessment(midterm).marksObtained(25.0).build());
        markRepository.save(Mark.builder().student(arun).assessment(quiz).marksObtained(12.0).build());

        submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(arun).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(arun).submittedAt(null).status("MISSING").build());

        // -------------------------------------------------------------
        // Seed Interventions for Showcase
        // -------------------------------------------------------------
        interventionRepository.save(Intervention.builder()
                .student(arun)
                .assignedFaculty(faculty)
                .type("Urgent Academic Mentoring")
                .priority("HIGH")
                .status(InterventionStatus.PLANNED)
                .description("Severe attendance shortfall (30%) and declining assessment scores. Immediate parent-faculty counseling scheduled.")
                .startDate(LocalDate.now().minusDays(1))
                .dueDate(LocalDate.now().plusDays(5))
                .build());

        interventionRepository.save(Intervention.builder()
                .student(chirag)
                .assignedFaculty(faculty)
                .type("Remedial Tutoring & Coursework Support")
                .priority("MEDIUM")
                .status(InterventionStatus.IN_PROGRESS)
                .description("Assignment completion assistance for CSE101 to recover 4 overdue deliverables.")
                .startDate(LocalDate.now().minusDays(3))
                .dueDate(LocalDate.now().plusDays(10))
                .build());

        interventionRepository.save(Intervention.builder()
                .student(bhavna)
                .assignedFaculty(faculty)
                .type("Assignment Submission Check-in")
                .priority("LOW")
                .status(InterventionStatus.COMPLETED)
                .description("Resolved submission delay for Binary Search Trees coursework. Student back on track.")
                .startDate(LocalDate.now().minusDays(7))
                .dueDate(LocalDate.now().minusDays(1))
                .build());

        // -------------------------------------------------------------
        // Calculate initial risk assessments for all 4 students
        // -------------------------------------------------------------
        riskAssessmentService.calculateRiskForStudent(aarav.getId());
        riskAssessmentService.calculateRiskForStudent(bhavna.getId());
        riskAssessmentService.calculateRiskForStudent(chirag.getId());
        riskAssessmentService.calculateRiskForStudent(arun.getId());

        log.info("Demo showcase bootstrap completed successfully. 4 synthetic showcase students initialized with distinct risk profiles.");
        return true;
    }
}
