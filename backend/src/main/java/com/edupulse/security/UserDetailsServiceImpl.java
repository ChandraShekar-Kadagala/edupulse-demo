package com.edupulse.security;

import com.edupulse.model.User;
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
    private com.edupulse.repository.StudentRepository studentRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> studentRepository.findByStudentNumber(username)
                        .map(com.edupulse.model.Student::getUser)
                        .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username or registration number: " + username)));
        return UserDetailsImpl.build(user);
    }
}
