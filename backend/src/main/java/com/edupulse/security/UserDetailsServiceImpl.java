package com.edupulse.security;

import com.edupulse.model.Student;
import com.edupulse.model.User;
import com.edupulse.repository.StudentRepository;
import com.edupulse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StudentRepository studentRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username or registration number cannot be blank");
        }

        String identifier = username.trim();

        // 1. Exact lookup by username
        User user = userRepository.findByUsername(identifier).orElse(null);

        // 2. Exact lookup by student registration number
        if (user == null) {
            user = studentRepository.findByStudentNumber(identifier)
                    .map(Student::getUser)
                    .orElse(null);
        }

        // 3. Fallback alias mapping for demo accounts (case-insensitive)
        if (user == null) {
            String lower = identifier.toLowerCase();
            if (lower.equals("demo.student.a") || lower.equals("student_a") || lower.equals("stu_a") || lower.equals("stu-1001") || lower.equals("chandrashekar")) {
                user = userRepository.findByUsername("demo.student.a")
                        .orElseGet(() -> studentRepository.findByStudentNumber("STU-1001").map(Student::getUser)
                                .orElseGet(() -> userRepository.findByUsername("student_a")
                                        .orElseGet(() -> userRepository.findByUsername("chandrashekar").orElse(null))));
            } else if (lower.equals("demo.student.b") || lower.equals("student_b") || lower.equals("stu_b") || lower.equals("aarav") || lower.equals("stu-1002")) {
                user = userRepository.findByUsername("demo.student.b")
                        .orElseGet(() -> studentRepository.findByStudentNumber("STU-1002").map(Student::getUser)
                                .orElseGet(() -> userRepository.findByUsername("aarav")
                                        .orElseGet(() -> userRepository.findByUsername("student_b").orElse(null))));
            } else if (lower.equals("demo.student.c") || lower.equals("student_c") || lower.equals("stu_c") || lower.equals("bhavna") || lower.equals("stu-1003")) {
                user = userRepository.findByUsername("demo.student.c")
                        .orElseGet(() -> studentRepository.findByStudentNumber("STU-1003").map(Student::getUser)
                                .orElseGet(() -> userRepository.findByUsername("bhavna")
                                        .orElseGet(() -> userRepository.findByUsername("student_c").orElse(null))));
            } else if (lower.equals("demo.student.d") || lower.equals("student_d") || lower.equals("stu_d") || lower.equals("arun") || lower.equals("stu-1004")) {
                user = userRepository.findByUsername("demo.student.d")
                        .orElseGet(() -> studentRepository.findByStudentNumber("STU-1004").map(Student::getUser)
                                .orElseGet(() -> userRepository.findByUsername("arun")
                                        .orElseGet(() -> userRepository.findByUsername("student_d").orElse(null))));
            } else if (lower.equals("demo.faculty.1") || lower.equals("faculty_a") || lower.equals("roopa") || lower.equals("faculty")) {
                user = userRepository.findByUsername("demo.faculty.1")
                        .orElseGet(() -> userRepository.findByUsername("roopa")
                                .orElseGet(() -> userRepository.findByUsername("faculty").orElse(null)));
            } else if (lower.equals("demo.faculty.2") || lower.equals("faculty_b") || lower.equals("aditya")) {
                user = userRepository.findByUsername("demo.faculty.2")
                        .orElseGet(() -> userRepository.findByUsername("aditya").orElse(null));
            } else if (lower.equals("admin")) {
                user = userRepository.findByUsername("admin").orElse(null);
            }
        }

        if (user == null) {
            throw new UsernameNotFoundException("User Not Found with username or registration number: " + username);
        }
        return UserDetailsImpl.build(user);
    }
}
