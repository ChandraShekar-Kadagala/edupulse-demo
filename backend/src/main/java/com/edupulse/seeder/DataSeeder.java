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
import java.util.List;

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

        log.info("Starting safe demo showcase bootstrap with 7-subject academic reconstruction, 4-student showcase and 2 faculty profiles...");

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

        // 2. Create Faculty 1: Dr. Roopa Tirumalasetti (assigned CSE 204)
        User roopaUser = userRepository.save(User.builder()
                .username("roopa")
                .passwordHash(encodedPassword)
                .role(Role.FACULTY)
                .name("Dr. Roopa Tirumalasetti")
                .email("demo.roopa@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        Faculty facultyRoopa = facultyRepository.save(Faculty.builder()
                .user(roopaUser)
                .department("Computer Science & Engineering")
                .title("Associate Professor")
                .build());

        // 3. Create Faculty 2: Dr. Aditya Kumar Sahu (assigned CSE 213)
        User adityaUser = userRepository.save(User.builder()
                .username("aditya")
                .passwordHash(encodedPassword)
                .role(Role.FACULTY)
                .name("Dr. Aditya Kumar Sahu")
                .email("demo.aditya@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        Faculty facultyAditya = facultyRepository.save(Faculty.builder()
                .user(adityaUser)
                .department("Computer Science & Engineering")
                .title("Assistant Professor")
                .build());

        // Also create 'faculty' user aliasing Dr. Roopa for backwards-compatible demo logins
        userRepository.save(User.builder()
                .username("faculty")
                .passwordHash(encodedPassword)
                .role(Role.FACULTY)
                .name("Dr. Roopa Tirumalasetti (Demo Faculty)")
                .email("demo.faculty@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        // 4. Seed 7 SRM AP Courses
        Course cse203 = courseRepository.save(Course.builder().code("CSE 203").name("DISCRETE MATHEMATICS").credits(4).build());
        Course cse204 = courseRepository.save(Course.builder().code("CSE 204").name("DESIGN AND ANALYSIS OF ALGORITHMS").credits(4).build());
        Course cse207 = courseRepository.save(Course.builder().code("CSE 207").name("DIGITAL ELECTRONICS").credits(4).build());
        Course cse213 = courseRepository.save(Course.builder().code("CSE 213").name("ADVANCED JAVA PROGRAMMING").credits(4).build());
        Course fic120 = courseRepository.save(Course.builder().code("FIC 120").name("FOUNDATIONS OF ELECTRICAL AND ELECTRONICS ENGINEERING").credits(3).build());
        Course mce272 = courseRepository.save(Course.builder().code("MCE 272").name("INTRODUCTION TO ROCKET AND JET TECHNOLOGY").credits(2).build());
        Course sec203 = courseRepository.save(Course.builder().code("SEC 203").name("APTITUDE FOR ENGINEERS - III").credits(1).build());

        // 5. Seed Assessments for all 7 Courses
        // CSE 203: Mid 1 (20/20), CLA 1 (10/10), CLA 2 (10/10), CLA 3 (10/10) -> Total 50
        Assessment cse203Mid = assessmentRepository.save(Assessment.builder().course(cse203).name("Mid Semester Exam I").maxMarks(20.0).weightage(20.0).type(AssessmentType.INTERNAL).build());
        Assessment cse203Cla1 = assessmentRepository.save(Assessment.builder().course(cse203).name("CLA 1").maxMarks(10.0).weightage(10.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse203).name("CLA 2").maxMarks(10.0).weightage(10.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse203).name("CLA 3").maxMarks(10.0).weightage(10.0).type(AssessmentType.INTERNAL).build());

        // CSE 204: Mid 1 (25, w14), CLA 1 (20, w8), CLA 2 (20, w8), Weekly Evaluation (20, w10), Project (20, w10) -> Total 50
        Assessment cse204Mid = assessmentRepository.save(Assessment.builder().course(cse204).name("Mid Semester Exam I").maxMarks(25.0).weightage(14.0).type(AssessmentType.INTERNAL).build());
        Assessment cse204Cla1 = assessmentRepository.save(Assessment.builder().course(cse204).name("CLA 1").maxMarks(20.0).weightage(8.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse204).name("CLA 2").maxMarks(20.0).weightage(8.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse204).name("Weekly Evaluation").maxMarks(20.0).weightage(10.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse204).name("Project").maxMarks(20.0).weightage(10.0).type(AssessmentType.INTERNAL).build());

        // CSE 207: Mid 1 (25, w15), CLA 1 (10, w5), CLA 2 (10, w5), CLA 3 (10, w5), Lab (20, w10), Model (20, w5), Obs (10, w5) -> Total 50
        Assessment cse207Mid = assessmentRepository.save(Assessment.builder().course(cse207).name("Mid Semester Exam I").maxMarks(25.0).weightage(15.0).type(AssessmentType.INTERNAL).build());
        Assessment cse207Cla1 = assessmentRepository.save(Assessment.builder().course(cse207).name("CLA 1").maxMarks(10.0).weightage(5.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse207).name("CLA 2").maxMarks(10.0).weightage(5.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse207).name("CLA 3").maxMarks(10.0).weightage(5.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse207).name("Lab Performance").maxMarks(20.0).weightage(10.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse207).name("Model Exam").maxMarks(20.0).weightage(5.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse207).name("Observation Note").maxMarks(10.0).weightage(5.0).type(AssessmentType.INTERNAL).build());

        // CSE 213: Mid 1 (30, w25), CLA 1 (10, w5), CLA 2 (10, w5), Lab (25, w15) -> Total 50
        Assessment cse213Mid = assessmentRepository.save(Assessment.builder().course(cse213).name("Mid Semester Exam I").maxMarks(30.0).weightage(25.0).type(AssessmentType.INTERNAL).build());
        Assessment cse213Cla1 = assessmentRepository.save(Assessment.builder().course(cse213).name("CLA 1").maxMarks(10.0).weightage(5.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse213).name("CLA 2").maxMarks(10.0).weightage(5.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(cse213).name("Lab Performance").maxMarks(25.0).weightage(15.0).type(AssessmentType.INTERNAL).build());

        // FIC 120: Mid 1 (25, w10), CLA 1 (20, w10), CLA 2 (20, w10), CLA 3 (20, w10), Lab (20, w5), Model (20, w2.5), Obs (10, w2.5) -> Total 50
        Assessment fic120Mid = assessmentRepository.save(Assessment.builder().course(fic120).name("Mid Semester Exam I").maxMarks(25.0).weightage(10.0).type(AssessmentType.INTERNAL).build());
        Assessment fic120Cla1 = assessmentRepository.save(Assessment.builder().course(fic120).name("CLA 1").maxMarks(20.0).weightage(10.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(fic120).name("CLA 2").maxMarks(20.0).weightage(10.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(fic120).name("CLA 3").maxMarks(20.0).weightage(10.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(fic120).name("Lab Performance").maxMarks(20.0).weightage(5.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(fic120).name("Model Exam").maxMarks(20.0).weightage(2.5).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(fic120).name("Observation Note").maxMarks(10.0).weightage(2.5).type(AssessmentType.INTERNAL).build());

        // MCE 272: Mid 1 (30, w20), CLA 1 (30, w15), CLA 2 (30, w15) -> Total 50
        Assessment mce272Mid = assessmentRepository.save(Assessment.builder().course(mce272).name("Mid Semester Exam I").maxMarks(30.0).weightage(20.0).type(AssessmentType.INTERNAL).build());
        Assessment mce272Cla1 = assessmentRepository.save(Assessment.builder().course(mce272).name("CLA 1").maxMarks(30.0).weightage(15.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(mce272).name("CLA 2").maxMarks(30.0).weightage(15.0).type(AssessmentType.INTERNAL).build());

        // SEC 203: Mid 1 (50, w20), CLA 1 (20, w10), CLA 2 (20, w10) -> Total 40
        Assessment sec203Mid = assessmentRepository.save(Assessment.builder().course(sec203).name("Mid Semester Exam I").maxMarks(50.0).weightage(20.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(sec203).name("CLA 1").maxMarks(20.0).weightage(10.0).type(AssessmentType.INTERNAL).build());
        assessmentRepository.save(Assessment.builder().course(sec203).name("CLA 2").maxMarks(20.0).weightage(10.0).type(AssessmentType.INTERNAL).build());

        // 6. Seed Assignments for Coursework
        Assignment a1 = assignmentRepository.save(Assignment.builder().course(cse204).name("Assignment 1: Divide and Conquer Analysis").dueDate(LocalDate.of(2026, 9, 15)).build());
        Assignment a2 = assignmentRepository.save(Assignment.builder().course(cse204).name("Assignment 2: Dynamic Programming Solutions").dueDate(LocalDate.of(2026, 10, 5)).build());
        Assignment a3 = assignmentRepository.save(Assignment.builder().course(cse213).name("Assignment 1: Multithreaded Server Design").dueDate(LocalDate.of(2026, 9, 20)).build());
        Assignment a4 = assignmentRepository.save(Assignment.builder().course(cse213).name("Assignment 2: JDBC Repository & Connection Pool").dueDate(LocalDate.of(2026, 10, 10)).build());
        Assignment a5 = assignmentRepository.save(Assignment.builder().course(cse203).name("Assignment 1: Graph Theory Proofs").dueDate(LocalDate.of(2026, 9, 25)).build());
        Assignment a6 = assignmentRepository.save(Assignment.builder().course(cse207).name("Assignment 1: Sequential Logic Circuits").dueDate(LocalDate.of(2026, 10, 1)).build());

        // =========================================================================
        // Student 1: KADAGALA CHANDRA SHEKAR (ACTUAL STUDENT-SUPPLIED REFERENCE DATA)
        // Attendance: 275/276 = 99.64% (0 absent except 1 in MCE 272)
        // Converted Marks: 150.44 / 340 (Unconducted assessments left NULL/Pending)
        // Missing assignments: 0 -> Risk: 0.0 (LOW)
        // =========================================================================
        User chandraUser = userRepository.save(User.builder()
                .username("chandrashekar")
                .passwordHash(encodedPassword)
                .role(Role.STUDENT)
                .name("KADAGALA CHANDRA SHEKAR")
                .email("chandrashekar.k@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        Student chandra = studentRepository.save(Student.builder()
                .user(chandraUser)
                .studentNumber("STU-2026-CHANDRA")
                .department("Computer Science & Engineering")
                .currentSemester(5)
                .currentCgpa(9.1)
                .build());

        // Exact Attendance for Chandra Shekar (275 present, 1 absent in MCE 272)
        seedCourseAttendance(chandra, cse203, 29, 0);
        seedCourseAttendance(chandra, cse204, 48, 0);
        seedCourseAttendance(chandra, cse207, 53, 0);
        seedCourseAttendance(chandra, cse213, 49, 0);
        seedCourseAttendance(chandra, fic120, 44, 0);
        seedCourseAttendance(chandra, mce272, 21, 1); // 20 Present, 1 Absent = 95.24%
        seedCourseAttendance(chandra, sec203, 32, 0);

        // Exact Marks for Chandra Shekar (Raw & Converted -> Sum = 150.44)
        markRepository.save(Mark.builder().student(chandra).assessment(cse203Mid).marksObtained(18.25).build()); // conv 18.25
        markRepository.save(Mark.builder().student(chandra).assessment(cse203Cla1).marksObtained(10.00).build()); // conv 10.00 -> 28.25
        markRepository.save(Mark.builder().student(chandra).assessment(cse204Mid).marksObtained(24.00).build()); // conv 13.44
        markRepository.save(Mark.builder().student(chandra).assessment(cse204Cla1).marksObtained(20.00).build()); // conv 8.00 -> 21.44
        markRepository.save(Mark.builder().student(chandra).assessment(cse207Mid).marksObtained(20.50).build()); // conv 12.30
        markRepository.save(Mark.builder().student(chandra).assessment(cse207Cla1).marksObtained(9.00).build()); // conv 4.50 -> 16.80
        markRepository.save(Mark.builder().student(chandra).assessment(cse213Mid).marksObtained(30.00).build()); // conv 25.00
        markRepository.save(Mark.builder().student(chandra).assessment(cse213Cla1).marksObtained(10.00).build()); // conv 5.00 -> 30.00
        markRepository.save(Mark.builder().student(chandra).assessment(fic120Mid).marksObtained(17.00).build()); // conv 6.80
        markRepository.save(Mark.builder().student(chandra).assessment(fic120Cla1).marksObtained(15.50).build()); // conv 7.75 -> 14.55
        markRepository.save(Mark.builder().student(chandra).assessment(mce272Mid).marksObtained(25.50).build()); // conv 17.00
        markRepository.save(Mark.builder().student(chandra).assessment(mce272Cla1).marksObtained(28.00).build()); // conv 14.00 -> 31.00
        markRepository.save(Mark.builder().student(chandra).assessment(sec203Mid).marksObtained(21.00).build()); // conv 8.40 -> 8.40
        // Total Converted Obtained: 28.25 + 21.44 + 16.80 + 30.00 + 14.55 + 31.00 + 8.40 = 150.44

        // Submissions for Chandra Shekar (all submitted on time)
        submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(chandra).submittedAt(LocalDate.of(2026, 9, 12)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(chandra).submittedAt(LocalDate.of(2026, 10, 3)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(chandra).submittedAt(LocalDate.of(2026, 9, 18)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(chandra).submittedAt(LocalDate.of(2026, 10, 8)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a5).student(chandra).submittedAt(LocalDate.of(2026, 9, 22)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a6).student(chandra).submittedAt(LocalDate.of(2026, 9, 29)).status("SUBMITTED").build());

        // =========================================================================
        // Student 2: Aarav Sharma (SYNTHETIC TOP PERFORMER)
        // Attendance: 268/276 = 97.10% (LOW Attendance Risk: 0.0)
        // Marks: High marks (~92%), 0 missing assignments -> Risk: 0.0 (LOW)
        // =========================================================================
        User aaravUser = userRepository.save(User.builder()
                .username("aarav")
                .passwordHash(encodedPassword)
                .role(Role.STUDENT)
                .name("Aarav Sharma (Demo Top Performer)")
                .email("aarav.sharma@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        Student aarav = studentRepository.save(Student.builder()
                .user(aaravUser)
                .studentNumber("STU-1002")
                .department("Computer Science & Engineering")
                .currentSemester(5)
                .currentCgpa(9.4)
                .build());

        seedCourseAttendance(aarav, cse203, 29, 1);
        seedCourseAttendance(aarav, cse204, 48, 1);
        seedCourseAttendance(aarav, cse207, 53, 1);
        seedCourseAttendance(aarav, cse213, 49, 1);
        seedCourseAttendance(aarav, fic120, 44, 1);
        seedCourseAttendance(aarav, mce272, 21, 1);
        seedCourseAttendance(aarav, sec203, 32, 2); // Total: 268 present, 8 absent = 97.10%

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

        submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(aarav).submittedAt(LocalDate.of(2026, 9, 10)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(aarav).submittedAt(LocalDate.of(2026, 10, 1)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(aarav).submittedAt(LocalDate.of(2026, 9, 15)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(aarav).submittedAt(LocalDate.of(2026, 10, 5)).status("SUBMITTED").build());

        // =========================================================================
        // Student 3: Bhavna Patel (SYNTHETIC AVERAGE PERFORMER)
        // Attendance: 196/276 = 71.01% -> Attendance Penalty = (75.0 - 71.01) * 1.5 = ~6.0 pts
        // Marks: ~39% -> Marks Penalty = (50.0 - 39.0) = 11.0 pts
        // Missing assignments: 4 overdue -> Penalty = 20.0 pts
        // Total Risk Score: ~37.0 (MEDIUM)
        // =========================================================================
        User bhavnaUser = userRepository.save(User.builder()
                .username("bhavna")
                .passwordHash(encodedPassword)
                .role(Role.STUDENT)
                .name("Bhavna Patel (Demo Average Performer)")
                .email("bhavna.patel@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        Student bhavna = studentRepository.save(Student.builder()
                .user(bhavnaUser)
                .studentNumber("STU-1003")
                .department("Computer Science & Engineering")
                .currentSemester(5)
                .currentCgpa(7.2)
                .build());

        seedCourseAttendance(bhavna, cse203, 29, 8);
        seedCourseAttendance(bhavna, cse204, 48, 16);
        seedCourseAttendance(bhavna, cse207, 53, 17);
        seedCourseAttendance(bhavna, cse213, 49, 14);
        seedCourseAttendance(bhavna, fic120, 44, 12);
        seedCourseAttendance(bhavna, mce272, 21, 5);
        seedCourseAttendance(bhavna, sec203, 32, 8); // Total: 196 present, 80 absent = 71.01%

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

        // 2 submitted, 4 missing
        submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(bhavna).submittedAt(LocalDate.of(2026, 9, 14)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(bhavna).submittedAt(LocalDate.of(2026, 10, 4)).status("SUBMITTED").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(bhavna).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(bhavna).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a5).student(bhavna).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a6).student(bhavna).submittedAt(null).status("MISSING").build());

        // =========================================================================
        // Student 4: Arun Kumar (SYNTHETIC HIGH-RISK PERFORMER)
        // Attendance: 165/276 = 59.78% -> Attendance Penalty = (75.0 - 59.78) * 1.5 = 22.83 pts
        // Marks: ~28% -> Marks Penalty = (50.0 - 28.0) * 1.0 = 22.0 pts
        // Missing assignments: 6 missing -> Penalty = 30.0 pts
        // Total Risk Score: 22.83 + 22.0 + 30.0 = 74.83 pts -> HIGH (>= 65.0)
        // =========================================================================
        User arunUser = userRepository.save(User.builder()
                .username("arun")
                .passwordHash(encodedPassword)
                .role(Role.STUDENT)
                .name("Arun Kumar (Demo High-Risk)")
                .email("arun.kumar@edupulse.local")
                .authProvider(AuthProvider.DEMO)
                .build());

        Student arun = studentRepository.save(Student.builder()
                .user(arunUser)
                .studentNumber("STU-1004")
                .department("Computer Science & Engineering")
                .currentSemester(5)
                .currentCgpa(5.1)
                .build());

        seedCourseAttendance(arun, cse203, 29, 12);
        seedCourseAttendance(arun, cse204, 48, 20);
        seedCourseAttendance(arun, cse207, 53, 22);
        seedCourseAttendance(arun, cse213, 49, 20);
        seedCourseAttendance(arun, fic120, 44, 18);
        seedCourseAttendance(arun, mce272, 21, 9);
        seedCourseAttendance(arun, sec203, 32, 10); // Total: 165 present, 111 absent = 59.78%

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

        // 6 missing assignments
        submissionRepository.save(AssignmentSubmission.builder().assignment(a1).student(arun).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a2).student(arun).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a3).student(arun).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a4).student(arun).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a5).student(arun).submittedAt(null).status("MISSING").build());
        submissionRepository.save(AssignmentSubmission.builder().assignment(a6).student(arun).submittedAt(null).status("MISSING").build());

        // 7. Seed Interventions
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

        // 8. Calculate Risk Assessments for all 4 students
        riskAssessmentService.calculateRiskForStudent(chandra.getId());
        riskAssessmentService.calculateRiskForStudent(aarav.getId());
        riskAssessmentService.calculateRiskForStudent(bhavna.getId());
        riskAssessmentService.calculateRiskForStudent(arun.getId());

        log.info("Demo showcase bootstrap successfully completed with 4 students (1 actual student reference + 3 synthetic profiles) across 7 subjects and 2 faculty profiles.");
        return true;
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
