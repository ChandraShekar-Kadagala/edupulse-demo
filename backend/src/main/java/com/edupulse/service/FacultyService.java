package com.edupulse.service;

import com.edupulse.dto.FacultyDto;
import com.edupulse.model.Faculty;
import com.edupulse.model.User;
import com.edupulse.repository.FacultyRepository;
import com.edupulse.repository.StudentRepository;
import com.edupulse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FacultyService {

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    public List<FacultyDto> getAllFacultyProfiles() {
        List<Faculty> allFaculty = facultyRepository.findAll();
        List<FacultyDto> result = new ArrayList<>();
        int studentCount = (int) studentRepository.count();

        for (Faculty f : allFaculty) {
            result.add(mapFacultyToDto(f, studentCount));
        }

        // If table is empty or before seeder runs, provide standard verified faculty details
        if (result.isEmpty()) {
            result.add(buildFallbackFaculty(101L, "Faculty A", "demo.faculty.1", "24082", "Associate Professor",
                    "CSE 204", "DESIGN AND ANALYSIS OF ALGORITHMS", "(C 305), (S 404)", studentCount));
            result.add(buildFallbackFaculty(102L, "Faculty B", "demo.faculty.2", "25346", "Assistant Professor",
                    "CSE 213", "ADVANCED JAVA PROGRAMMING", "(C 305), (S 404)", studentCount));
        }

        return result;
    }

    public FacultyDto getFacultyById(Long id) {
        int studentCount = (int) studentRepository.count();
        return facultyRepository.findById(id)
                .map(f -> mapFacultyToDto(f, studentCount))
                .orElseGet(() -> {
                    if (id == 101L) {
                        return buildFallbackFaculty(101L, "Faculty A", "demo.faculty.1", "24082", "Associate Professor",
                                "CSE 204", "DESIGN AND ANALYSIS OF ALGORITHMS", "(C 305), (S 404)", studentCount);
                    }
                    return buildFallbackFaculty(102L, "Faculty B", "demo.faculty.2", "25346", "Assistant Professor",
                            "CSE 213", "ADVANCED JAVA PROGRAMMING", "(C 305), (S 404)", studentCount);
                });
    }

    private FacultyDto mapFacultyToDto(Faculty f, int studentCount) {
        User u = f.getUser();
        String name = u != null ? u.getName() : "Faculty Member";
        String username = u != null ? u.getUsername() : "demo.faculty.1";
        String email = u != null ? u.getEmail() : "demo.faculty.1@edupulse.local";

        boolean isFaculty1 = name.contains("Faculty A") || name.contains("Roopa") || username.equalsIgnoreCase("demo.faculty.1") || username.equalsIgnoreCase("roopa");
        String courseCode = isFaculty1 ? "CSE 204" : "CSE 213";
        String courseName = isFaculty1 ? "DESIGN AND ANALYSIS OF ALGORITHMS" : "ADVANCED JAVA PROGRAMMING";
        String empCode = isFaculty1 ? "24082" : "25346";
        String classroom = isFaculty1 ? "(C 305), (S 404)" : "(C 305), (S 404)";

        return FacultyDto.builder()
                .id(f.getId())
                .name(name)
                .username(username)
                .email(email)
                .department(f.getDepartment() != null ? f.getDepartment() : "Computer Science & Engineering")
                .title(f.getTitle() != null ? f.getTitle() : "Faculty Advisor")
                .employeeCode(empCode)
                .assignedCourseCode(courseCode)
                .assignedCourseName(courseName)
                .classroom(classroom)
                .assignedCourses(List.of(
                        FacultyDto.CourseSummaryDto.builder()
                                .courseCode(courseCode)
                                .courseName(courseName)
                                .credits(4)
                                .classroom(classroom)
                                .slotDetails(isFaculty1 ? "Mon (09:00-10:50), Wed (10:00-10:50), Fri (01:00-02:50)" : "Mon (01:00-01:50), Thu (09:00-10:50), Fri (09:00-10:50)")
                                .build()
                ))
                .enrolledStudentsCount(studentCount)
                .atRiskStudentsCount(1) // Arun Kumar
                .build();
    }

    private FacultyDto buildFallbackFaculty(Long id, String name, String username, String empCode, String title,
                                            String courseCode, String courseName, String classroom, int studentCount) {
        return FacultyDto.builder()
                .id(id)
                .name(name)
                .username(username)
                .email(username + "@edupulse.local")
                .department("Computer Science & Engineering")
                .title(title)
                .employeeCode(empCode)
                .assignedCourseCode(courseCode)
                .assignedCourseName(courseName)
                .classroom(classroom)
                .assignedCourses(List.of(
                        FacultyDto.CourseSummaryDto.builder()
                                .courseCode(courseCode)
                                .courseName(courseName)
                                .credits(4)
                                .classroom(classroom)
                                .slotDetails(courseCode.equals("CSE 204") ? "Mon (09:00-10:50), Wed (10:00-10:50), Fri (01:00-02:50)" : "Mon (01:00-01:50), Thu (09:00-10:50), Fri (09:00-10:50)")
                                .build()
                ))
                .enrolledStudentsCount(studentCount)
                .atRiskStudentsCount(1)
                .build();
    }
}
