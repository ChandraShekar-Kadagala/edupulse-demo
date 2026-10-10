package com.edupulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedRiskRecordDto {
    private Long id;
    private String ciphertextBase64;
    private String nonceBase64;
    private String encryptionVersion;
    private String schemaVersion;
    private LocalDateTime createdAt;
}
