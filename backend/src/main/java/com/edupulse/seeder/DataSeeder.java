package com.edupulse.seeder;

import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.service.RiskAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Component
@Transactional
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
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

    @Value("${app.seeder.enabled:false}")
    private boolean seederEnabled;

    @Value("${app.seeder.demo-password:DemoEduPulse2026!}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (seederEnabled) {
            log.info("SEED_DEMO_DATA is enabled (app.seeder.enabled=true): Bootstrapping demo showcase cohort...");
            seedDemoData();
        } else {
            log.info("SEED_DEMO_DATA is disabled (app.seeder.enabled=false). Automatic startup seeding skipped.");
        }
    }

    /**
     * Idempotent, transaction-safe demo showcase bootstrapper.
     * Provisions the six dedicated demo accounts (4 students, 2 faculty) plus admin.
     * Never deletes, overwrites, or modifies real non-demo users or production data.
     *
     * @return true if demo accounts were seeded or refreshed; false if error
     */
    @Transactional
    public boolean seedDemoData() {
        log.info("Executing safe, idempotent demo showcase provisioning with 6 dedicated accounts...");

        String encodedPassword = passwordEncoder.encode(demoPassword);

        // 1. Create or verify Demo Admin User
        User adminUser = getOrCreateDemoUser(
                "admin",
                new String[]{"demo.admin"},
                Role.ADMIN,
                "Administrator",
                "admin@edupulse.srmap.edu.in",
                encodedPassword
        );

        // 2. Create Faculty 1: Faculty A (assigned CSE 204)
        User faculty1User = getOrCreateDemoUser(
                "demo.faculty.1",
                new String[]{"roopa", "faculty", "faculty_a"},
                Role.FACULTY,
                "Faculty A",
                "demo.faculty.1@edupulse.srmap.edu.in",
                encodedPassword
        );

        Faculty faculty1 = facultyRepository.findById(faculty1User.getId()).orElseGet(() -> {
            return facultyRepository.save(Faculty.builder()
                    .user(faculty1User)
                    .department("Computer Science & Engineering")
                    .title("Associate Professor")
                    .build());
        });
        faculty1.setTitle("Associate Professor");
        faculty1.setDepartment("Computer Science & Engineering");
        facultyRepository.save(faculty1);

        // 3. Create Faculty 2: Faculty B (assigned CSE 213)
        User faculty2User = getOrCreateDemoUser(
                "demo.faculty.2",
                new String[]{"aditya", "faculty_b"},
                Role.FACULTY,
                "Faculty B",
                "demo.faculty.2@edupulse.srmap.edu.in",
                encodedPassword
        );

        Faculty faculty2 = facultyRepository.findById(faculty2User.getId()).orElseGet(() -> {
            return facultyRepository.save(Faculty.builder()
                    .user(faculty2User)
                    .department("Computer Science & Engineering")
                    .title("Assistant Professor")
                    .build());
        });
        faculty2.setTitle("Assistant Professor");
        faculty2.setDepartment("Computer Science & Engineering");
        facultyRepository.save(faculty2);

        // Backwards compatibility: ensure legacy usernames 'roopa', 'aditya', 'faculty' authenticate if present
        ensureLegacyUser("roopa", Role.FACULTY, "Faculty A", "demo.faculty.1@edupulse.srmap.edu.in", encodedPassword);
        ensureLegacyUser("aditya", Role.FACULTY, "Faculty B", "demo.faculty.2@edupulse.srmap.edu.in", encodedPassword);
        ensureLegacyUser("faculty", Role.FACULTY, "Faculty A", "demo.faculty.1@edupulse.srmap.edu.in", encodedPassword);

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
                courseRepository.save(Course.builder().code("SEC 203").name("QUANTITATIVE APTITUDE AND LOGICAL REASONING").credits(2).build()));

        // 5. Seed Official Assessments (matching SRM AP Student Corner course syllabus structure)
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

        // 6. Seed Course Assignments
        Assignment a1 = getOrCreateAssignment(cse204, "Assignment 1: Divide and Conquer Algorithms", LocalDate.of(2026, 9, 15));
        Assignment a2 = getOrCreateAssignment(cse204, "Assignment 2: Dynamic Programming Problems", LocalDate.of(2026, 10, 5));
        Assignment a3 = getOrCreateAssignment(cse213, "Assignment 1: Multi-threading & Concurrency", LocalDate.of(2026, 9, 20));
        Assignment a4 = getOrCreateAssignment(cse213, "Assignment 2: JDBC & Spring JPA Integration", LocalDate.of(2026, 10, 10));
        Assignment a5 = getOrCreateAssignment(cse203, "Assignment 1: Recurrence Relations and Graph Theory", LocalDate.of(2026, 9, 25));
        Assignment a6 = getOrCreateAssignment(cse207, "Assignment 1: Sequential Logic Circuits", LocalDate.of(2026, 10, 1));

        // =========================================================================
        // Student A: Student A — Reference (demo.student.a / STU-1001)
        // Profile: Anonymized SRM AP academic reference data
        // Attendance: 275/276 = 99.64% (0 absent except 1 in MCE 272)
        // Converted Marks: 150.44 / 340 (Unconducted assessments left NULL/Pending)
        // Missing assignments: 0 -> Risk: 0.0 (LOW)
        // =========================================================================
        User studentAUser = getOrCreateDemoUser(
                "demo.student.a",
                new String[]{"student_a", "chandrashekar"},
                Role.STUDENT,
                "Student A — Reference",
                "demo.student.a@edupulse.srmap.edu.in",
                encodedPassword
        );
        ensureLegacyUser("student_a", Role.STUDENT, "Student A — Reference", "demo.student.a@edupulse.srmap.edu.in", encodedPassword);

        Student studentA = studentRepository.findById(studentAUser.getId())
                .orElseGet(() -> studentRepository.findByStudentNumber("STU-1001")
                        .orElseGet(() -> studentRepository.save(Student.builder()
                                .user(studentAUser)
                                .studentNumber("STU-1001")
                                .department("Computer Science & Engineering")
                                .currentSemester(5)
                                .currentCgpa(9.1)
                                .build())));
        studentA.setCurrentCgpa(9.1);
        studentRepository.save(studentA);

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
        // Student B: Student B — Top Performer (demo.student.b / STU-1002)
        // Profile: High performance (268/276 attendance, 92.4% marks)
        // Attendance: 268/276 = 97.10% (LOW Attendance Risk: 0.0)
        // Marks: High marks (~92%), 0 missing assignments -> Risk: 0.0 (LOW)
        // =========================================================================
        User studentBUser = getOrCreateDemoUser(
                "demo.student.b",
                new String[]{"aarav", "student_b"},
                Role.STUDENT,
                "Student B — Top Performer",
                "demo.student.b@edupulse.srmap.edu.in",
                encodedPassword
        );
        ensureLegacyUser("aarav", Role.STUDENT, "Student B — Top Performer", "demo.student.b@edupulse.srmap.edu.in", encodedPassword);

        Student studentB = studentRepository.findById(studentBUser.getId())
                .orElseGet(() -> studentRepository.findByStudentNumber("STU-1002")
                        .orElseGet(() -> studentRepository.save(Student.builder()
                                .user(studentBUser)
                                .studentNumber("STU-1002")
                                .department("Computer Science & Engineering")
                                .currentSemester(5)
                                .currentCgpa(9.4)
                                .build())));
        studentB.setCurrentCgpa(9.4);
        studentRepository.save(studentB);

        if (attendanceRepository.findByStudentId(studentB.getId()).isEmpty()) {
            seedCourseAttendance(studentB, cse203, 29, 1);
            seedCourseAttendance(studentB, cse204, 48, 1);
            seedCourseAttendance(studentB, cse207, 53, 1);
            seedCourseAttendance(studentB, cse213, 49, 1);
            seedCourseAttendance(studentB, fic120, 44, 1);
            seedCourseAttendance(studentB, mce272, 21, 1);
            seedCourseAttendance(studentB, sec203, 32, 2); // 268 present, 8 absent = 97.10%
        }

        if (markRepository.findByStudentId(studentB.getId()).isEmpty()) {
            markRepository.save(Mark.builder().student(studentB).assessment(cse203Mid).marksObtained(19.00).build());
            markRepository.save(Mark.builder().student(studentB).assessment(cse203Cla1).marksObtained(9.50).build());
            markRepository.save(Mark.builder().student(studentB).assessment(cse204Mid).marksObtained(24.00).build());
            markRepository.save(Mark.builder().student(studentB).assessment(cse204Cla1).marksObtained(19.00).build());
            markRepository.save(Mark.builder().student(studentB).assessment(cse207Mid).marksObtained(23.50).build());
            markRepository.save(Mark.builder().student(studentB).assessment(cse207Cla1).marksObtained(9.50).build());
            markRepository.save(Mark.builder().student(studentB).assessment(cse213Mid).marksObtained(28.50).build());
            markRepository.save(Mark.builder().student(studentB).assessment(cse213Cla1).marksObtained(9.50).build());
            markRepository.save(Mark.builder().student(studentB).assessment(fic120Mid).marksObtained(23.00).build());
            markRepository.save(Mark.builder().student(studentB).assessment(fic120Cla1).marksObtained(19.00).build());
            markRepository.save(Mark.builder().student(studentB).assessment(mce272Mid).marksObtained(28.50).build());
            markRepository.save(Mark.builder().student(studentB).assessment(mce272Cla1).marksObtained(28.50).build());
            markRepository.save(Mark.builder().student(studentB).assessment(sec203Mid).marksObtained(46.00).build());
        }

        if (submissionRepository.findByStudentId(studentB.getId()).isEmpty()) {
            submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(studentB).submittedAt(LocalDate.of(2026, 9, 10)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(studentB).submittedAt(LocalDate.of(2026, 10, 1)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(studentB).submittedAt(LocalDate.of(2026, 9, 15)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(studentB).submittedAt(LocalDate.of(2026, 10, 5)).status("SUBMITTED").build());
        }

        // =========================================================================
        // Student C: Student C — Average Performer (demo.student.c / STU-1003)
        // Profile: Average performance with 4 overdue deliverables
        // Attendance: 196/276 = 71.01% -> Attendance Penalty = (75.0 - 71.01) * 1.5 = 5.98 pts
        // Marks: ~38.8% -> Marks Penalty = (50.0 - 38.8) = 11.2 pts
        // Missing assignments: 4 overdue -> Penalty = 20.0 pts
        // Total Risk Score: ~37.0 (MEDIUM)
        // =========================================================================
        User studentCUser = getOrCreateDemoUser(
                "demo.student.c",
                new String[]{"bhavna", "student_c"},
                Role.STUDENT,
                "Student C — Average Performer",
                "demo.student.c@edupulse.srmap.edu.in",
                encodedPassword
        );
        ensureLegacyUser("bhavna", Role.STUDENT, "Student C — Average Performer", "demo.student.c@edupulse.srmap.edu.in", encodedPassword);

        Student studentC = studentRepository.findById(studentCUser.getId())
                .orElseGet(() -> studentRepository.findByStudentNumber("STU-1003")
                        .orElseGet(() -> studentRepository.save(Student.builder()
                                .user(studentCUser)
                                .studentNumber("STU-1003")
                                .department("Computer Science & Engineering")
                                .currentSemester(5)
                                .currentCgpa(7.2)
                                .build())));
        studentC.setCurrentCgpa(7.2);
        studentRepository.save(studentC);

        if (attendanceRepository.findByStudentId(studentC.getId()).isEmpty()) {
            seedCourseAttendance(studentC, cse203, 29, 8);
            seedCourseAttendance(studentC, cse204, 48, 16);
            seedCourseAttendance(studentC, cse207, 53, 17);
            seedCourseAttendance(studentC, cse213, 49, 14);
            seedCourseAttendance(studentC, fic120, 44, 12);
            seedCourseAttendance(studentC, mce272, 21, 5);
            seedCourseAttendance(studentC, sec203, 32, 8); // 196 present, 80 absent = 71.01%
        }

        if (markRepository.findByStudentId(studentC.getId()).isEmpty()) {
            markRepository.save(Mark.builder().student(studentC).assessment(cse203Mid).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(cse203Cla1).marksObtained(4.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(cse204Mid).marksObtained(10.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(cse204Cla1).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(cse207Mid).marksObtained(10.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(cse207Cla1).marksObtained(4.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(cse213Mid).marksObtained(12.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(cse213Cla1).marksObtained(4.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(fic120Mid).marksObtained(10.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(fic120Cla1).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(mce272Mid).marksObtained(12.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(mce272Cla1).marksObtained(12.00).build());
            markRepository.save(Mark.builder().student(studentC).assessment(sec203Mid).marksObtained(20.00).build());
        }

        if (submissionRepository.findByStudentId(studentC.getId()).isEmpty()) {
            submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(studentC).submittedAt(LocalDate.of(2026, 9, 14)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(studentC).submittedAt(LocalDate.of(2026, 10, 4)).status("SUBMITTED").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(studentC).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(studentC).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a5).student(studentC).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a6).student(studentC).submittedAt(null).status("MISSING").build());
        }

        // =========================================================================
        // Student D: Student D — High Risk (demo.student.d / STU-1004)
        // Profile: High academic risk with severe attendance shortfall and 6 overdue assignments
        // Attendance: 165/276 = 59.78% -> Attendance Penalty = (75.0 - 59.78) * 1.5 = 22.83 pts
        // Marks: ~27.6% -> Marks Penalty = (50.0 - 27.6) * 1.0 = 22.4 pts
        // Missing assignments: 6 missing -> Penalty = 30.0 pts
        // Total Risk Score: ~74.8 pts -> HIGH (>= 65.0)
        // =========================================================================
        User studentDUser = getOrCreateDemoUser(
                "demo.student.d",
                new String[]{"arun", "student_d"},
                Role.STUDENT,
                "Student D — High Risk",
                "demo.student.d@edupulse.srmap.edu.in",
                encodedPassword
        );
        ensureLegacyUser("arun", Role.STUDENT, "Student D — High Risk", "demo.student.d@edupulse.srmap.edu.in", encodedPassword);

        Student studentD = studentRepository.findById(studentDUser.getId())
                .orElseGet(() -> studentRepository.findByStudentNumber("STU-1004")
                        .orElseGet(() -> studentRepository.save(Student.builder()
                                .user(studentDUser)
                                .studentNumber("STU-1004")
                                .department("Computer Science & Engineering")
                                .currentSemester(5)
                                .currentCgpa(5.1)
                                .build())));
        studentD.setCurrentCgpa(5.1);
        studentRepository.save(studentD);

        if (attendanceRepository.findByStudentId(studentD.getId()).isEmpty()) {
            seedCourseAttendance(studentD, cse203, 29, 12);
            seedCourseAttendance(studentD, cse204, 48, 20);
            seedCourseAttendance(studentD, cse207, 53, 22);
            seedCourseAttendance(studentD, cse213, 49, 20);
            seedCourseAttendance(studentD, fic120, 44, 18);
            seedCourseAttendance(studentD, mce272, 21, 9);
            seedCourseAttendance(studentD, sec203, 32, 10); // 165 present, 111 absent = 59.78%
        }

        if (markRepository.findByStudentId(studentD.getId()).isEmpty()) {
            markRepository.save(Mark.builder().student(studentD).assessment(cse203Mid).marksObtained(5.50).build());
            markRepository.save(Mark.builder().student(studentD).assessment(cse203Cla1).marksObtained(2.50).build());
            markRepository.save(Mark.builder().student(studentD).assessment(cse204Mid).marksObtained(7.00).build());
            markRepository.save(Mark.builder().student(studentD).assessment(cse204Cla1).marksObtained(5.00).build());
            markRepository.save(Mark.builder().student(studentD).assessment(cse207Mid).marksObtained(7.00).build());
            markRepository.save(Mark.builder().student(studentD).assessment(cse207Cla1).marksObtained(2.50).build());
            markRepository.save(Mark.builder().student(studentD).assessment(cse213Mid).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(studentD).assessment(cse213Cla1).marksObtained(2.50).build());
            markRepository.save(Mark.builder().student(studentD).assessment(fic120Mid).marksObtained(7.00).build());
            markRepository.save(Mark.builder().student(studentD).assessment(fic120Cla1).marksObtained(5.00).build());
            markRepository.save(Mark.builder().student(studentD).assessment(mce272Mid).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(studentD).assessment(mce272Cla1).marksObtained(8.00).build());
            markRepository.save(Mark.builder().student(studentD).assessment(sec203Mid).marksObtained(14.00).build());
        }

        if (submissionRepository.findByStudentId(studentD.getId()).isEmpty()) {
            submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(studentD).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(studentD).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(studentD).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(studentD).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a5).student(studentD).submittedAt(null).status("MISSING").build());
            submissionRepository.save(AssignmentSubmission.builder().assignment(a6).student(studentD).submittedAt(null).status("MISSING").build());
        }

        // 7. Seed Interventions
        if (interventionRepository.count() == 0) {
            interventionRepository.save(Intervention.builder()
                    .student(studentD)
                    .assignedFaculty(faculty1)
                    .type("Urgent Academic Mentoring")
                    .priority("HIGH")
                    .status(InterventionStatus.PLANNED)
                    .description("Severe attendance shortfall (59.8%) and marks deficiency across core subjects. Parent-faculty consultation required.")
                    .startDate(LocalDate.now().minusDays(2))
                    .dueDate(LocalDate.now().plusDays(5))
                    .build());

            interventionRepository.save(Intervention.builder()
                    .student(studentC)
                    .assignedFaculty(faculty2)
                    .type("Remedial Tutoring & Coursework Support")
                    .priority("MEDIUM")
                    .status(InterventionStatus.IN_PROGRESS)
                    .description("Assignment catch-up support to clear 4 overdue deliverables across CSE 204 and CSE 213.")
                    .startDate(LocalDate.now().minusDays(4))
                    .dueDate(LocalDate.now().plusDays(10))
                    .build());

            interventionRepository.save(Intervention.builder()
                    .student(studentB)
                    .assignedFaculty(faculty1)
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
        riskAssessmentService.calculateRiskForStudent(studentB.getId());
        riskAssessmentService.calculateRiskForStudent(studentC.getId());
        riskAssessmentService.calculateRiskForStudent(studentD.getId());

        log.info("Demo showcase bootstrap successfully completed with 6 dedicated showcase accounts (4 students, 2 faculty) plus admin.");
        return true;
    }

    private User getOrCreateDemoUser(String primaryUsername, String[] aliases, Role role, String name, String email, String encodedPassword) {
        Optional<User> existing = userRepository.findByUsername(primaryUsername);
        if (existing.isEmpty() && aliases != null) {
            for (String alias : aliases) {
                existing = userRepository.findByUsername(alias);
                if (existing.isPresent()) break;
            }
        }

        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getAuthProvider() == AuthProvider.DEMO || user.getAuthProvider() == null) {
                user.setUsername(primaryUsername);
                user.setPasswordHash(encodedPassword);
                user.setRole(role);
                user.setName(name);
                user.setEmail(email);
                user.setAuthProvider(AuthProvider.DEMO);
                return userRepository.save(user);
            } else {
                log.warn("Account {} is a non-demo user (authProvider={}). Real application data preserved without modification.",
                        user.getUsername(), user.getAuthProvider());
                return user;
            }
        }

        return userRepository.save(User.builder()
                .username(primaryUsername)
                .passwordHash(encodedPassword)
                .role(role)
                .name(name)
                .email(email)
                .authProvider(AuthProvider.DEMO)
                .build());
    }

    private void ensureLegacyUser(String legacyUsername, Role role, String name, String email, String encodedPassword) {
        if (userRepository.findByUsername(legacyUsername).isEmpty()) {
            userRepository.save(User.builder()
                    .username(legacyUsername)
                    .passwordHash(encodedPassword)
                    .role(role)
                    .name(name)
                    .email(email)
                    .authProvider(AuthProvider.DEMO)
                    .build());
        }
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
