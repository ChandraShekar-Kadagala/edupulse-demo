package com.edupulse;

import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.edupulse.seeder.DataSeeder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Re-provisioning removes stray (non-canonical) course data from the demo students (e.g. from an old
 *  fixture sync), keeping the showcase coherent at the seven SRM AP subjects. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.seeder.enabled=true"})
@org.springframework.transaction.annotation.Transactional
class DemoDataCleanupTest {

    @Autowired private DataSeeder seeder;
    @Autowired private StudentRepository studentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AttendanceRepository attendanceRepository;

    @Test
    @DisplayName("A stray course's attendance on a demo student is removed on the next provision")
    void reseedRemovesStrayCourseData() {
        seeder.seedDemoData();
        Student a = studentRepository.findByStudentNumber("STU-1001").orElseThrow();

        // Simulate pollution: a non-canonical course with attendance for the demo student.
        Course stray = courseRepository.save(Course.builder().code("MAT 202").name("PROBABILITY AND STATISTICS").credits(3).build());
        attendanceRepository.save(Attendance.builder().student(a).course(stray)
                .date(LocalDate.now()).status(AttendanceStatus.PRESENT).build());
        long strayBefore = attendanceRepository.findByStudentId(a.getId()).stream()
                .filter(att -> att.getCourse() != null && att.getCourse().getCode().equals("MAT 202")).count();
        assertEquals(1, strayBefore);

        seeder.seedDemoData(); // re-provision

        long strayAfter = attendanceRepository.findByStudentId(a.getId()).stream()
                .filter(att -> att.getCourse() != null && att.getCourse().getCode().equals("MAT 202")).count();
        assertEquals(0, strayAfter, "stray course attendance must be cleaned off the demo student");

        // The seven canonical subjects are still present.
        var codes = attendanceRepository.findByStudentId(a.getId()).stream()
                .map(att -> att.getCourse().getCode()).distinct().sorted().toList();
        assertTrue(codes.containsAll(java.util.List.of("CSE 203", "CSE 204", "CSE 207", "CSE 213", "FIC 120", "MCE 272", "SEC 203")));
        assertFalse(codes.contains("MAT 202"));
    }

    @Test
    @DisplayName("SEC 203 keeps the real course name")
    void sec203NameIsCurrent() {
        seeder.seedDemoData();
        assertEquals("APTITUDE FOR ENGINEERS - III", courseRepository.findByCode("SEC 203").orElseThrow().getName());
    }
}
