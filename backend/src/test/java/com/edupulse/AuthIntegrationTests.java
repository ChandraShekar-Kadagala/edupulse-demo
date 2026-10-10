package com.edupulse;

import com.edupulse.dto.LoginRequest;
import com.edupulse.model.*;
import com.edupulse.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
public class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        tokenRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .username("arun")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .name("Arun Kumar")
                .email("arun@example.com")
                .build();
        user = userRepository.save(user);

        Student student = Student.builder()
                .user(user)
                .studentNumber("AP25110070436")
                .department("CS")
                .build();
        studentRepository.save(student);
        
        User admin = User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("adminpass"))
                .role(Role.ADMIN)
                .name("Admin")
                .email("admin@example.com")
                .build();
        userRepository.save(admin);
    }

    @Test
    public void testValidLoginWithUsername() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("arun");
        req.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("arun"));
    }

    @Test
    public void testValidLoginWithRegistrationNumber() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("AP25110070436");
        req.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("arun")); // mapped to actual username
    }

    @Test
    public void testInvalidLogin() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("arun");
        req.setPassword("wrongpass");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    public void testUnknownAccountPasswordReset() throws Exception {
        // Even if account doesn't exist, it should return 200 OK to prevent enumeration
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"NONEXISTENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
                
        assertTrue(tokenRepository.findAll().isEmpty());
    }

    @Test
    public void testSuccessfulPasswordReset() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);
        
        User user = userRepository.findByUsername("arun").get();
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(hashedToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        tokenRepository.save(prt);

        // Perform Reset
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", rawToken, "newPassword", "newSecurePassword123"))))
                .andExpect(status().isOk());

        // Verify it was marked as used
        PasswordResetToken updatedToken = tokenRepository.findByToken(hashedToken).get();
        assertTrue(updatedToken.isUsed());

        // Verify login works with new password
        LoginRequest req = new LoginRequest();
        req.setUsername("arun");
        req.setPassword("newSecurePassword123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    public void testExpiredToken() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);
        
        User user = userRepository.findByUsername("arun").get();
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(hashedToken)
                .user(user)
                .expiryDate(LocalDateTime.now().minusHours(1)) // expired
                .used(false)
                .build();
        tokenRepository.save(prt);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", rawToken, "newPassword", "newSecurePassword123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token has expired or already been used"));
    }
    
    @Test
    public void testReusedToken() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);
        
        User user = userRepository.findByUsername("arun").get();
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(hashedToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(true) // already used
                .build();
        tokenRepository.save(prt);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", rawToken, "newPassword", "newSecurePassword123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token has expired or already been used"));
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
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
}
