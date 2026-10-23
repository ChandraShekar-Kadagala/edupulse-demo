package com.edupulse.controller;

import com.edupulse.dto.EncryptedRiskRecordDto;
import com.edupulse.model.EncryptedRiskRecord;
import com.edupulse.model.Student;
import com.edupulse.repository.EncryptedRiskRecordRepository;
import com.edupulse.repository.StudentRepository;
import com.edupulse.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}", maxAge = 3600)
@RestController
@RequestMapping({"/api/privacy", "/api/v1/privacy"})
public class PrivacyApiController {

    @Autowired
    private EncryptedRiskRecordRepository encryptedRiskRecordRepository;

    @Autowired
    private StudentRepository studentRepository;

    @PostMapping("/records")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> saveEncryptedRecord(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody EncryptedRiskRecordDto dto) {
        
        Student student = studentRepository.findById(userDetails.getId()).orElse(null);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only students can save private records.");
        }

        if (dto.getCiphertextBase64() == null || dto.getNonceBase64() == null || dto.getCiphertextBase64().length() > 50000) {
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        EncryptedRiskRecord record = EncryptedRiskRecord.builder()
                .student(student)
                .ciphertextBase64(dto.getCiphertextBase64())
                .nonceBase64(dto.getNonceBase64())
                .encryptionVersion(dto.getEncryptionVersion() != null ? dto.getEncryptionVersion() : "1.0")
                .schemaVersion(dto.getSchemaVersion() != null ? dto.getSchemaVersion() : "1.0")
                .createdAt(LocalDateTime.now())
                .build();
        
        EncryptedRiskRecord saved = encryptedRiskRecordRepository.save(record);
        dto.setId(saved.getId());
        dto.setCreatedAt(saved.getCreatedAt());
        
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/records")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getEncryptedRecords(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<EncryptedRiskRecord> records = encryptedRiskRecordRepository.findByStudentIdOrderByCreatedAtDesc(userDetails.getId());
        List<EncryptedRiskRecordDto> dtos = records.stream().map(r -> EncryptedRiskRecordDto.builder()
                .id(r.getId())
                .ciphertextBase64(r.getCiphertextBase64())
                .nonceBase64(r.getNonceBase64())
                .encryptionVersion(r.getEncryptionVersion())
                .schemaVersion(r.getSchemaVersion())
                .createdAt(r.getCreatedAt())
                .build()).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> deleteRecord(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long id) {
        EncryptedRiskRecord record = encryptedRiskRecordRepository.findById(id).orElse(null);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        if (!record.getStudent().getId().equals(userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        encryptedRiskRecordRepository.delete(record);
        return ResponseEntity.ok().build();
    }
}
