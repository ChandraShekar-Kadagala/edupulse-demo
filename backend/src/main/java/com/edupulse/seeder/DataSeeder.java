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
import java.util.Optional;

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
     * Safely aborts if ANY non-demo (real application) user records are present.
     * Never overwrites, deletes, or modifies existing real users or production data.
     *
     * @return true if demo data was seeded or already up to date; false if aborted due to real records
     */
    @Transactional
    public boolean seedDemoData() {
        // Safety guard: If real users (authProvider != DEMO) exist, abort immediately
        boolean hasRealUsers = userRepository.findAll().stream()
                .anyMatch(u -> u.getAuthProvider() != AuthProvider.DEMO);

        if (hasRealUsers) {
            log.warn("Demo showcase bootstrap skipped: Real application users detected (authProvider != DEMO). All existing user data preserved without modification.");
            return false;
        }

        log.info("Starting safe demo showcase bootstrap with anonymized Student A reference and 3 synthetic comparison profiles...");

        String encodedPassword = passwordEncoder.encode(demoPassword);

        // 1. Create Demo Admin User
        User adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            return userRepository.save(User.builder()
                    .username("admin")
                    .passwordHash(encodedPassword)
                    .role(Role.ADMIN)
                    .name("System Administrator (Demo)")
                    .email("demo.admin@edupulse.local")
                    .authProvider(AuthProvider.DEMO)
                    .build());
        });

        // 2. Create Faculty 1: Dr. Roopa Tirumalasetti (assigned CSE 204)
        User roopaUser = userRepository.findByUsername("roopa").orElseGet(() -> {
            return userRepository.save(User.builder()
                    .username("roopa")
                    .passwordHash(encodedPassword)
                    .role(Role.FACULTY)
                    .name("Dr. Roopa Tirumalasetti")
                    .email("demo.roopa@edupulse.local")
                    .authProvider(AuthProvider.DEMO)
                    .build());
        });

        Faculty facultyRoopa = facultyRepository.findById(roopaUser.getId()).orElseGet(() -> {
            return facultyRepository.save(Faculty.builder()
                    .user(roopaUser)
                    .department("Computer Science & Engineering")
                    .title("Associate Professor")
                    .build());
        });

        // 3. Create Faculty 2: Dr. Aditya Kumar Sahu (assigned CSE 213)
        User adityaUser = userRepository.findByUsername("aditya").orElseGet(() -> {
            return userRepository.save(User.builder()
                    .username("aditya")
                    .passwordHash(encodedPassword)
                    .role(Role.FACULTY)
                    .name("Dr. Aditya Kumar Sahu")
                    .email("demo.aditya@edupulse.local")
                    .authProvider(AuthProvider.DEMO)
                    .build());
        });

        Faculty facultyAditya = facultyRepository.findById(adityaUser.getId()).orElseGet(() -> {
            return facultyRepository.save(Faculty.builder()
                    .user(adityaUser)
                    .department("Computer Science & Engineering")
                    .title("Assistant Professor")
                    .build());
        });

        // Create 'faculty' user aliasing Dr. Roopa for backwards-compatible demo logins
        if (userRepository.findByUsername("faculty").isEmpty()) {
            userRepository.save(User.builder()
                    .username("faculty")
                    .passwordHash(encodedPassword)
                    .role(Role.FACULTY)
                    .name("Dr. Roopa Tirumalasetti (Demo Faculty)")
                    .email("demo.faculty@edupulse.local")
                    .authProvider(AuthProvider.DEMO)
                    .build());
        }

        // 4. Seed 7 SRM AP Courses
        Course cse203 = courseRepository.findByCode("CSE 203").orElseGet(() ->
                courseRepository.save(Course.builder().code("CSE 203").name("DISCRETE MATHEMATICS").credits(4).build()));
        Course cse204 = courseRepository.findByCode("CSE 204").orElseGet(() ->
                courseRepository.save(Course.builder().code("CSE 204").name("DESIGN AND ANALYSIS OF ALGORITHMS").credits(4).build()));
        Course cse207 = courseRepository.findByCode("CSE 207").orElseGet(() ->
                courseRepository.save(Course.builder().code("CSE 207").name("DIGITAL ELECTRONICS").credits(4).build()));
        Course cse213 = courseRepository.findByCode("CSE 213").orElseGet(() ->
                courseRepository.save(Course.builder().code("CSE 213").name("ADVANCED JAVA PROGRAMMING").credits(4).build()));
        Course fic120 = courseRepository.findByCode("FIC 120").orElseGet(() ->
                courseRepository.save(Course.builder().code("FIC 120").name("FOUNDATIONS OF ELECTRICAL AND ELECTRONICS ENGINEERING").credits(3).build()));
        Course mce272 = courseRepository.findByCode("MCE 272").orElseGet(() ->
                courseRepository.save(Course.builder().code("MCE 272").name("INTRODUCTION TO ROCKET AND JET TECHNOLOGY").credits(2).build()));
        Course sec203 = courseRepository.findByCode("SEC 203").orElseGet(() ->
                courseRepository.save(Course.builder().code("SEC 203").name("APTITUDE FOR ENGINEERS - III").credits(1).build()));

        // 5. Seed Assessments for all 7 Courses
        Assessment cse203Mid = getOrCreateAssessment(cse203, "Mid Semester Exam I", 20.0, 20.0);
        Assessment cse203Cla1 = getOrCreateAssessment(cse203, "CLA 1", 10.0, 10.0);
        getOrCreateAssessment(cse203, "CLA 2", 10.0, 10.0);
        getOrCreateAssessment(cse203, "CLA 3", 10.0, 10.0);

        Assessment cse204Mid = getOrCreateAssessment(cse204, "Mid Semester Exam I", 25.0, 14.0);
        Assessment cse204Cla1 = getOrCreateAssessment(cse204, "CLA 1", 20.0, 8.0);
        getOrCreateAssessment(cse204, "CLA 2", 20.0, 8.0);
        getOrCreateAssessment(cse204, "Weekly Evaluation", 20.0, 10.0);
        getOrCreateAssessment(cse204, "Project", 20.0, 10.0);

        Assessment cse207Mid = getOrCreateAssessment(cse207, "Mid Semester Exam I", 25.0, 15.0);
        Assessment cse207Cla1 = getOrCreateAssessment(cse207, "CLA 1", 10.0, 5.0);
        getOrCreateAssessment(cse207, "CLA 2", 10.0, 5.0);
        getOrCreateAssessment(cse207, "CLA 3", 10.0, 5.0);
        getOrCreateAssessment(cse207, "Lab Performance", 20.0, 10.0);
        getOrCreateAssessment(cse207, "Model Exam", 20.0, 5.0);
        getOrCreateAssessment(cse207, "Observation Note", 10.0, 5.0);

        Assessment cse213Mid = getOrCreateAssessment(cse213, "Mid Semester Exam I", 30.0, 25.0);
        Assessment cse213Cla1 = getOrCreateAssessment(cse213, "CLA 1", 10.0, 5.0);
        getOrCreateAssessment(cse213, "CLA 2", 10.0, 5.0);
        getOrCreateAssessment(cse213, "Lab Performance", 25.0, 15.0);

        Assessment fic120Mid = getOrCreateAssessment(fic120, "Mid Semester Exam I", 25.0, 10.0);
        Assessment fic120Cla1 = getOrCreateAssessment(fic120, "CLA 1", 20.0, 10.0);
        getOrCreateAssessment(fic120, "CLA 2", 20.0, 10.0);
        getOrCreateAssessment(fic120, "CLA 3", 20.0, 10.0);
        getOrCreateAssessment(fic120, "Lab Performance", 20.0, 5.0);
        getOrCreateAssessment(fic120, "Model Exam", 20.0, 2.5);
        getOrCreateAssessment(fic120, "Observation Note", 10.0, 2.5);

        Assessment mce272Mid = getOrCreateAssessment(mce272, "Mid Semester Exam I", 30.0, 20.0);
        Assessment mce272Cla1 = getOrCreateAssessment(mce272, "CLA 1", 30.0, 15.0);
        getOrCreateAssessment(mce272, "CLA 2", 30.0, 15.0);

        Assessment sec203Mid = getOrCreateAssessment(sec203, "Mid Semester Exam I", 50.0, 20.0);
        getOrCreateAssessment(sec203, "CLA 1", 20.0, 10.0);
        getOrCreateAssessment(sec203, "CLA 2", 20.0, 10.0);

        // 6. Seed Assignments for Coursework
        Assignment a1 = getOrCreateAssignment(cse204, "Assignment 1: Divide and Conquer Analysis", LocalDate.of(2026, 9, 15));
        Assignment a2 = getOrCreateAssignment(cse204, "Assignment 2: Dynamic Programming Solutions", LocalDate.of(2026, 10, 5));
        Assignment a3 = getOrCreateAssignment(cse213, "Assignment 1: Multithreaded Server Design", LocalDate.of(2026, 9, 20));
        Assignment a4 = getOrCreateAssignment(cse213, "Assignment 2: JDBC Repository & Connection Pool", LocalDate.of(2026, 10, 10));
        Assignment a5 = getOrCreateAssignment(cse203, "Assignment 1: Graph Theory Proofs", LocalDate.of(2026, 9, 25));
        Assignment a6 = getOrCreateAssignment(cse207, "Assignment 1: Sequential Logic Circuits", LocalDate.of(2026, 10, 1));

        // =========================================================================
        // Student A: Student A — Reference Profile (STUDENT-SUPPLIED REFERENCE DATA)
        // Privacy: Anonymized neutral identity. Original real student identity omitted.
        // Attendance: 275/276 = 99.64% (0 absent except 1 in MCE 272)
        // Converted Marks: 150.44 / 340 (Unconducted assessments left NULL/Pending)
        // Missing assignments: 0 -> Risk: 0.0 (LOW)
        // =========================================================================
        User studentAUser = userRepository.findByUsername("student_a")
                .orElseGet(() -> userRepository.findByUsername("chandrashekar")
                        .orElseGet(() -> userRepository.save(User.builder()
                                .username("student_a")
                                .passwordHash(encodedPassword)
                                .role(Role.STUDENT)
                                .name("Student A (Reference Profile)")
                                .email("student.a@edupulse.local")
                                .authProvider(AuthProvider.DEMO)
                                .build())));
        studentAUser.setName("Student A (Reference Profile)");
        userRepository.save(studentAUser);

        Student studentA = studentRepository.findById(studentAUser.getId()).orElseGet(() -> {
            return studentRepository.save(Student.builder()
                    .user(studentAUser)
                    .studentNumber("STU-1001")
                    .department("Computer Science & Engineering")
                    .currentSemester(5)
                    .currentCgpa(9.1)
                    .build());
        });

        if (attendanceRepository.findByStudentId(studentA.getId()).isEmpty()) {
            seedCourseAttendance(studentA, cse203, 29, 0);
            seedCourseAttendance(studentA, cse204, 48, 0);
            seedCourseAttendance(studentA, cse207, 53, 0);
            seedCourseAttendance(studentA, cse213, 49, 0);
            seedCourseAttendance(studentA, fic120, 44, 0);
            seedCourseAttendance(studentA, mce272, 21, 1); // 20 Present, 1 Absent = 95.24%
            seedCourseAttendance(studentA, sec203, 32, 0);
        }

        if (markRepository.findByStudentId(studentA.getId()).isEmpty()) {
            markRepository.save(Mark.builder().student(studentA).assessment(cse203Mid).marksObtained(18.25).build());
            markRepository.save(Mark.builder().student(studentA).assessment(cse203Cla1).marksObtained(10.00).build());
            markRepository.save(Mark.builder().student(studentA).assessment(cse204Mid).marksObtained(24.00).build());
            markRepository.save(Mark.builder().student(studentA).assessment(cse204Cla1).marksObtained(20.00).build());
            markRepository.save(Mark.builder().student(studentA).assessment(cse207Mid).marksObtained(20.50).build());
            markRepository.save(Mark.builder().student(studentA).assessment(cse207Cla1).marksObtained(9.00).build());
            markRepository.save(Mark.builder().student(studentA).assessment(cse213Mid).marksObtained(30.00).build());
            markRepository.save(Mark.builder().student(studentA).assessment(cse213Cla1).marksObtained(10.00).build());
            markRepository.save(Mark.builder().student(studentA).assessment(fic120Mid).marksObtained(17.00).build());
            markRepository.save(Mark.builder().student(studentA).assessment(fic120Cla1).marksObtained(15.50).build());
            markRepository.save(Mark.builder().student(studentA).assessment(mce272Mid).marksObtained(25.50).build());
            markRepository.save(Mark.builder().student(studentA).assessment(mce272Cla1).marksObtained(28.00).build());
            markRepository.save(Mark.builder().student(studentA).assessment(sec203Mid).marksObtained(21.00).build());
        }

        if (submissionRepository.findByStudentId(studentA.getId()).isEmpty()) {
            submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(studentA).submittedAt(LocalDate.of(2026, 9, 12)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(studentA).submittedAt(LocalDate.of(2026, 10, 3)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(studentA).submittedAt(LocalDate.of(2026, 9, 18)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(studentA).submittedAt(LocalDate.of(2026, 10, 8)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a5).student(studentA).submittedAt(LocalDate.of(2026, 9, 22)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a6).student(studentA).submittedAt(LocalDate.of(2026, 9, 29)).status("SUBMITTED").build());
        }

        // =========================================================================
        // Student B: Student B — Top Performer (Aarav Sharma) (SYNTHETIC TOP PERFORMER)
        // Attendance: 268/276 = 97.10% (LOW Attendance Risk: 0.0)
        // Marks: High marks (~92%), 0 missing assignments -> Risk: 0.0 (LOW)
        // =========================================================================
        User aaravUser = userRepository.findByUsername("aarav").orElseGet(() -> {
            return userRepository.save(User.builder()
                    .username("aarav")
                    .passwordHash(encodedPassword)
                    .role(Role.STUDENT)
                    .name("Student B (Top Performer)")
                    .email("student.b@edupulse.local")
                    .authProvider(AuthProvider.DEMO)
                    .build());
        });

        Student aarav = studentRepository.findById(aaravUser.getId()).orElseGet(() -> {
            return studentRepository.save(Student.builder()
                    .user(aaravUser)
                    .studentNumber("STU-1002")
                    .department("Computer Science & Engineering")
                    .currentSemester(5)
                    .currentCgpa(9.4)
                    .build());
        });

        if (attendanceRepository.findByStudentId(aarav.getId()).isEmpty()) {
            seedCourseAttendance(aarav, cse203, 29, 1);
            seedCourseAttendance(aarav, cse204, 48, 1);
            seedCourseAttendance(aarav, cse207, 53, 1);
            seedCourseAttendance(aarav, cse213, 49, 1);
            seedCourseAttendance(aarav, fic120, 44, 1);
            seedCourseAttendance(aarav, mce272, 21, 1);
            seedCourseAttendance(aarav, sec203, 32, 2); // 268 present, 8 absent = 97.10%
        }

        if (markRepository.findByStudentId(aarav.getId()).isEmpty()) {
            markRepository.save(Mark.builder().student(aarav).assessment(cse203Mid).marksObtained(19.00).build());
            markRepository.save(Mark.builder().student(aarav).assessment(cse203Cla1).marksObtained(9.50).build());
            markRepository.save(Mark.builder().student(aarav).assessment(cse204Mid).marksObtained(24.00).build());
            markRepository.save(Mark.builder().student(aarav).assessment(cse204Cla1).marksObtained(19.00).build());
            markRepository.save(Mark.builder().student(aarav).assessment(cse207Mid).marksObtained(23.50).build());
            markRepository.save(Mark.builder().student(aarav).assessment(cse207Cla1).marksObtained(9.50).build());
            markRepository.save(Mark.builder().student(aarav).assessment(cse213Mid).marksObtained(28.50).build());
            markRepository.save(Mark.builder().student(aarav).assessment(cse213Cla1).marksObtained(9.50).build());
            markRepository.save(Mark.builder().student(aarav).assessment(fic120Mid).marksObtained(23.00).build());
            markRepository.save(Mark.builder().student(aarav).assessment(fic120Cla1).marksObtained(19.00).build());
            markRepository.save(Mark.builder().student(aarav).assessment(mce272Mid).marksObtained(28.50).build());
            markRepository.save(Mark.builder().student(aarav).assessment(mce272Cla1).marksObtained(28.50).build());
            markRepository.save(Mark.builder().student(aarav).assessment(sec203Mid).marksObtained(46.00).build());
        }

        if (submissionRepository.findByStudentId(aarav.getId()).isEmpty()) {
            submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(aarav).submittedAt(LocalDate.of(2026, 9, 10)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(aarav).submittedAt(LocalDate.of(2026, 10, 1)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(aarav).submittedAt(LocalDate.of(2026, 9, 15)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(aarav).submittedAt(LocalDate.of(2026, 10, 5)).status("SUBMITTED").build());
        }

        // =========================================================================
        // Student C: Student C — Average Performer (Bhavna Patel) (SYNTHETIC AVERAGE)
        // Attendance: 196/276 = 71.01% -> Attendance Penalty = (75.0 - 71.01) * 1.5 = 5.98 pts
        // Marks: ~39% -> Marks Penalty = (50.0 - 39.0) = 11.0 pts
        // Missing assignments: 4 overdue -> Penalty = 20.0 pts
        // Total Risk Score: ~37.0 (MEDIUM)
        // =========================================================================
        User bhavnaUser = userRepository.findByUsername("bhavna").orElseGet(() -> {
            return userRepository.save(User.builder()
                    .username("bhavna")
                    .passwordHash(encodedPassword)
                    .role(Role.STUDENT)
                    .name("Student C (Average Performer)")
                    .email("student.c@edupulse.local")
                    .authProvider(AuthProvider.DEMO)
                    .build());
        });

        Student bhavna = studentRepository.findById(bhavnaUser.getId()).orElseGet(() -> {
            return studentRepository.save(Student.builder()
                    .user(bhavnaUser)
                    .studentNumber("STU-1003")
                    .department("Computer Science & Engineering")
                    .currentSemester(5)
                    .currentCgpa(7.2)
                    .build());
        });

        if (attendanceRepository.findByStudentId(bhavna.getId()).isEmpty()) {
            seedCourseAttendance(bhavna, cse203, 29, 8);
            seedCourseAttendance(bhavna, cse204, 48, 16);
            seedCourseAttendance(bhavna, cse207, 53, 17);
            seedCourseAttendance(bhavna, cse213, 49, 14);
            seedCourseAttendance(bhavna, fic120, 44, 12);
            seedCourseAttendance(bhavna, mce272, 21, 5);
            seedCourseAttendance(bhavna, sec203, 32, 8); // 196 present, 80 absent = 71.01%
        }

        if (markRepository.findByStudentId(bhavna.getId()).isEmpty()) {
            markRepository.save(Mark.builder().student(bhavna).assessment(cse203Mid).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(cse203Cla1).marksObtained(4.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(cse204Mid).marksObtained(10.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(cse204Cla1).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(cse207Mid).marksObtained(10.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(cse207Cla1).marksObtained(4.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(cse213Mid).marksObtained(12.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(cse213Cla1).marksObtained(4.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(fic120Mid).marksObtained(10.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(fic120Cla1).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(mce272Mid).marksObtained(12.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(mce272Cla1).marksObtained(12.00).build());
            markRepository.save(Mark.builder().student(bhavna).assessment(sec203Mid).marksObtained(20.00).build());
        }

        if (submissionRepository.findByStudentId(bhavna.getId()).isEmpty()) {
            submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(bhavna).submittedAt(LocalDate.of(2026, 9, 14)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(bhavna).submittedAt(LocalDate.of(2026, 10, 4)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(bhavna).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(bhavna).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a5).student(bhavna).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a6).student(bhavna).submittedAt(null).status("MISSING").build());
        }

        // =========================================================================
        // Student D: Student D — High-Risk Performer (Arun Kumar) (SYNTHETIC HIGH-RISK)
        // Attendance: 165/276 = 59.78% -> Attendance Penalty = (75.0 - 59.78) * 1.5 = 22.83 pts
        // Marks: ~28% -> Marks Penalty = (50.0 - 28.0) * 1.0 = 22.0 pts
        // Missing assignments: 6 missing -> Penalty = 30.0 pts
        // Total Risk Score: 22.83 + 22.0 + 30.0 = 74.83 pts -> HIGH (>= 65.0)
        // =========================================================================
        User arunUser = userRepository.findByUsername("arun").orElseGet(() -> {
            return userRepository.save(User.builder()
                    .username("arun")
                    .passwordHash(encodedPassword)
                    .role(Role.STUDENT)
                    .name("Student D (High-Risk Performer)")
                    .email("student.d@edupulse.local")
                    .authProvider(AuthProvider.DEMO)
                    .build());
        });

        Student arun = studentRepository.findById(arunUser.getId()).orElseGet(() -> {
            return studentRepository.save(Student.builder()
                    .user(arunUser)
                    .studentNumber("STU-1004")
                    .department("Computer Science & Engineering")
                    .currentSemester(5)
                    .currentCgpa(5.1)
                    .build());
        });

        if (attendanceRepository.findByStudentId(arun.getId()).isEmpty()) {
            seedCourseAttendance(arun, cse203, 29, 12);
            seedCourseAttendance(arun, cse204, 48, 20);
            seedCourseAttendance(arun, cse207, 53, 22);
            seedCourseAttendance(arun, cse213, 49, 20);
            seedCourseAttendance(arun, fic120, 44, 18);
            seedCourseAttendance(arun, mce272, 21, 9);
            seedCourseAttendance(arun, sec203, 32, 10); // 165 present, 111 absent = 59.78%
        }

        if (markRepository.findByStudentId(arun.getId()).isEmpty()) {
            markRepository.save(Mark.builder().student(arun).assessment(cse203Mid).marksObtained(5.50).build());
            markRepository.save(Mark.builder().student(arun).assessment(cse203Cla1).marksObtained(2.50).build());
            markRepository.save(Mark.builder().student(arun).assessment(cse204Mid).marksObtained(7.00).build());
            markRepository.save(Mark.builder().student(arun).assessment(cse204Cla1).marksObtained(5.00).build());
            markRepository.save(Mark.builder().student(arun).assessment(cse207Mid).marksObtained(7.00).build());
            markRepository.save(Mark.builder().student(arun).assessment(cse207Cla1).marksObtained(2.50).build());
            markRepository.save(Mark.builder().student(arun).assessment(cse213Mid).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(arun).assessment(cse213Cla1).marksObtained(2.50).build());
            markRepository.save(Mark.builder().student(arun).assessment(fic120Mid).marksObtained(7.00).build());
            markRepository.save(Mark.builder().student(arun).assessment(fic120Cla1).marksObtained(5.00).build());
            markRepository.save(Mark.builder().student(arun).assessment(mce272Mid).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(arun).assessment(mce272Cla1).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(arun).assessment(sec203Mid).marksObtained(14.00).build());
        }

        if (submissionRepository.findByStudentId(arun.getId()).isEmpty()) {
            submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(arun).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(arun).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(arun).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(arun).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a5).student(arun).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a6).student(arun).submittedAt(null).status("MISSING").build());
        }

        // 7. Seed Interventions
        if (interventionRepository.count() == 0) {
            interventionRepository.save(Intervention.builder()
                    .student(arun)
                    .assignedFaculty(facultyRoopa)
                    .type("Urgent Academic Mentoring")
                    .priority("HIGH")
                    .status(InterventionStatus.PLANNED)
                    .description("Severe attendance shortfall (59.8%) and marks deficiency across core subjects. Parent-faculty consultation required.")
                    .startDate(LocalDate.now().minusDays(2))
                    .dueDate(LocalDate.now().plusDays(5))
                    .build());

            interventionRepository.save(Intervention.builder()
                    .student(bhavna)
                    .assignedFaculty(facultyAditya)
                    .type("Remedial Tutoring & Coursework Support")
                    .priority("MEDIUM")
                    .status(InterventionStatus.IN_PROGRESS)
                    .description("Assignment catch-up support to clear 4 overdue deliverables across CSE 204 and CSE 213.")
                    .startDate(LocalDate.now().minusDays(4))
                    .dueDate(LocalDate.now().plusDays(10))
                    .build());

            interventionRepository.save(Intervention.builder()
                    .student(aarav)
                    .assignedFaculty(facultyRoopa)
                    .type("Honors Track Guidance")
                    .priority("LOW")
                    .status(InterventionStatus.COMPLETED)
                    .description("Outstanding academic standing (9.4 CGPA). Recommended for peer tutoring and undergraduate research fellowship.")
                    .startDate(LocalDate.now().minusDays(10))
                    .dueDate(LocalDate.now().minusDays(1))
                    .build());
        }

        // 8. Calculate Risk Assessments for all 4 students
        riskAssessmentService.calculateRiskForStudent(studentA.getId());
        riskAssessmentService.calculateRiskForStudent(aarav.getId());
        riskAssessmentService.calculateRiskForStudent(bhavna.getId());
        riskAssessmentService.calculateRiskForStudent(arun.getId());

        log.info("Demo showcase bootstrap successfully completed with 4 students (Student A Reference + 3 synthetic profiles) across 7 subjects and 2 faculty profiles.");
        return true;
    }

    private Assessment getOrCreateAssessment(Course course, String name, double maxMarks, double weightage) {
        return assessmentRepository.findAll().stream()
                .filter(a -> a.getCourse() != null && a.getCourse().getId().equals(course.getId()) && a.getName().equals(name))
                .findFirst()
                .orElseGet(() -> assessmentRepository.save(Assessment.builder()
                        .course(course)
                        .name(name)
                        .maxMarks(maxMarks)
                        .weightage(weightage)
                        .type(AssessmentType.INTERNAL)
                        .build()));
    }

    private Assignment getOrCreateAssignment(Course course, String name, LocalDate dueDate) {
        return assignmentRepository.findAll().stream()
                .filter(a -> a.getCourse() != null && a.getCourse().getId().equals(course.getId()) && a.getName().equals(name))
                .findFirst()
                .orElseGet(() -> assignmentRepository.save(Assignment.builder()
                        .course(course)
                        .name(name)
                        .dueDate(dueDate)
                        .build()));
    }

    private void seedCourseAttendance(Student student, Course course, int totalSessions, int absentCount) {
        LocalDate startDate = LocalDate.of(2026, 8, 3);
        for (int i = 0; i < totalSessions; i++) {
            LocalDate date = startDate.plusDays((i * 85L) / Math.max(1, totalSessions));
            AttendanceStatus status = (i < absentCount) ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT;
            attendanceRepository.save(Attendance.builder()
                    .student(student)
                    .course(course)
                    .date(date)
                    .status(status)
                    .build());
        }
    }
}
