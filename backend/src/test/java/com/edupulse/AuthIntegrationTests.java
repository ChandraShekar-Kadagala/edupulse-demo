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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
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

    @Test
    public void testTokenInvalidationAfterPasswordReset() throws Exception {
        // 1. Login to obtain an active token
        LoginRequest req = new LoginRequest();
        req.setUsername("arun");
        req.setPassword("password");

        String responseJson = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> respMap = objectMapper.readValue(responseJson, Map.class);
        String initialToken = (String) respMap.get("token");

        // 2. Verify token grants access to authenticated endpoint
        mockMvc.perform(get("/api/v1/integration/status")
                .header("Authorization", "Bearer " + initialToken))
                .andExpect(status().isOk());

        // 3. Perform Password Reset
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

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", rawToken, "newPassword", "brandNewPassword999"))))
                .andExpect(status().isOk());

        // 4. Verify initial token is now rejected with 401 Unauthorized
        mockMvc.perform(get("/api/v1/integration/status")
                .header("Authorization", "Bearer " + initialToken))
                .andExpect(status().isUnauthorized());

        // 5. Verify login works with new password and produces a working token
        req.setPassword("brandNewPassword999");
        String newLoginJson = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String newToken = (String) objectMapper.readValue(newLoginJson, Map.class).get("token");
        mockMvc.perform(get("/api/v1/integration/status")
                .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testLogoutInvalidatesToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("arun");
        req.setPassword("password");

        String responseJson = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = (String) objectMapper.readValue(responseJson, Map.class).get("token");

        // Verify token works
        mockMvc.perform(get("/api/v1/integration/status")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Call logout
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Token should now be invalid
        mockMvc.perform(get("/api/v1/integration/status")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRateLimitingOnLogin() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("arun");
        req.setPassword("wrongpass");

        String uniqueTestIp = "203.0.113.42";

        // First 10 requests should be processed (and return 401 Unauthorized due to wrong password)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .with(request -> {
                        request.setRemoteAddr(uniqueTestIp);
                        return request;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        // 11th request from the same IP must be throttled with HTTP 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(uniqueTestIp);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(429))
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }

    @Test
    public void testCorsPreflightOptions() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    public void testCrossStudentAccessForbidden() throws Exception {
        // Create second student
        User student2User = User.builder()
                .username("otherstudent")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.STUDENT)
                .name("Other Student")
                .email("other@example.com")
                .build();
        student2User = userRepository.save(student2User);

        Student student2 = Student.builder()
                .user(student2User)
                .studentNumber("AP99999999999")
                .department("CS")
                .build();
        studentRepository.save(student2);

        // Login as arun
        LoginRequest req = new LoginRequest();
        req.setUsername("arun");
        req.setPassword("password");

        String responseJson = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String arunToken = (String) objectMapper.readValue(responseJson, Map.class).get("token");

        // Arun attempts to view student 2's risk assessment
        mockMvc.perform(get("/api/students/" + student2User.getId() + "/risk")
                .header("Authorization", "Bearer " + arunToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testRoleRestrictionOnFacultyEndpoint() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("arun");
        req.setPassword("password");

        String responseJson = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String arunToken = (String) objectMapper.readValue(responseJson, Map.class).get("token");

        // Student tries to access faculty-only student directory
        mockMvc.perform(get("/api/students")
                .header("Authorization", "Bearer " + arunToken))
                .andExpect(status().isForbidden());

        // Student tries to access faculty-only dashboard stats
        mockMvc.perform(get("/api/dashboard/stats")
                .header("Authorization", "Bearer " + arunToken))
                .andExpect(status().isForbidden());
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
