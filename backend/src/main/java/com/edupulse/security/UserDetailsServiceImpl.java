package com.edupulse.security;

import com.edupulse.model.Student;
import com.edupulse.model.User;
import com.edupulse.repository.StudentRepository;
import com.edupulse.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@lombok.RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    private final StudentRepository studentRepository;

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


        if (user == null) {
            throw new UsernameNotFoundException("User Not Found with username or registration number: " + username);
        }
        return UserDetailsImpl.build(user);
    }
}
