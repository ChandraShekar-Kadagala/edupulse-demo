package com.edupulse.controller;

import com.edupulse.dto.JwtResponse;
import com.edupulse.dto.LoginRequest;
import com.edupulse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> request) {
        String username = request.get("username");
        if (username != null && !username.trim().isEmpty()) {
            authService.createPasswordResetTokenForUser(username.trim());
        }
        // Always return success to prevent username enumeration
        return ResponseEntity.ok(java.util.Map.of("message", "If an account exists, a reset link was generated."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        
        if (token == null || newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Invalid token or password too short"));
        }

        try {
            authService.resetPassword(token, newPassword);
            return ResponseEntity.ok(java.util.Map.of("message", "Password successfully reset"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@org.springframework.security.core.annotation.AuthenticationPrincipal com.edupulse.security.UserDetailsImpl userDetails) {
        if (userDetails != null) {
            authService.logoutUser(userDetails.getId());
        }
        return ResponseEntity.ok(java.util.Map.of("message", "User logged out successfully. Active tokens invalidated."));
    }
}
