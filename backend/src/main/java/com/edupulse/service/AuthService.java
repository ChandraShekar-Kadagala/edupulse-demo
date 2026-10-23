package com.edupulse.service;

import com.edupulse.dto.JwtResponse;
import com.edupulse.dto.LoginRequest;
import com.edupulse.security.JwtUtils;
import com.edupulse.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();    
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        return JwtResponse.builder()
                .token(jwt)
                .type("Bearer")
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .role(role)
                .roles(java.util.List.of(role))
                .authProvider(userDetails.getAuthProvider() != null ? userDetails.getAuthProvider() : "LOCAL")
                .build();
    }

    @Autowired
    private com.edupulse.repository.UserRepository userRepository;
    
    @Autowired
    private com.edupulse.repository.StudentRepository studentRepository;
    
    @Autowired
    private com.edupulse.repository.PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
    @Autowired
    private org.springframework.core.env.Environment env;
    
    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (int i = 0; i < encodedhash.length; i++) {
                String hex = Integer.toHexString(0xff & encodedhash[i]);
                if(hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    public void createPasswordResetTokenForUser(String username) {
        com.edupulse.model.User user = userRepository.findByUsername(username)
                .orElseGet(() -> studentRepository.findByStudentNumber(username)
                        .map(com.edupulse.model.Student::getUser)
                        .orElse(null));

        if (user != null) {
            String rawToken = java.util.UUID.randomUUID().toString();
            String hashedToken = hashToken(rawToken);
            
            com.edupulse.model.PasswordResetToken myToken = com.edupulse.model.PasswordResetToken.builder()
                .token(hashedToken)
                .user(user)
                .expiryDate(java.time.LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
            tokenRepository.save(myToken);
            
            if (env.acceptsProfiles(org.springframework.core.env.Profiles.of("dev"))) {
                System.out.println("DEV ONLY: PASSWORD RESET TOKEN FOR " + username + ": " + rawToken);
            }
        }
    }

    public void resetPassword(String rawToken, String newPassword) {
        String hashedToken = hashToken(rawToken);
        com.edupulse.model.PasswordResetToken resetToken = tokenRepository.findByToken(hashedToken)
            .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Token has expired or already been used");
        }

        com.edupulse.model.User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    public void logoutUser(Long userId) {
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                user.setTokenVersion(user.getTokenVersion() + 1);
                userRepository.save(user);
            });
        }
    }
}
