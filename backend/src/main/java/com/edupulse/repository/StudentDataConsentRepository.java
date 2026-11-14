package com.edupulse.repository;

import com.edupulse.model.StudentDataConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentDataConsentRepository extends JpaRepository<StudentDataConsent, Long> {
    Optional<StudentDataConsent> findFirstByStudentIdAndPurposeAndRevokedAtIsNullOrderByGrantedAtDesc(Long studentId, String purpose);
}
