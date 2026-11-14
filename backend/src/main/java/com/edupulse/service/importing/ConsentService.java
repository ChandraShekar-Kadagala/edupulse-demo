package com.edupulse.service.importing;

import com.edupulse.exception.ResourceNotFoundException;
import com.edupulse.model.Student;
import com.edupulse.model.StudentDataConsent;
import com.edupulse.repository.StudentDataConsentRepository;
import com.edupulse.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** Grants, checks and revokes a student's consent to import academic data. */
@Service
@RequiredArgsConstructor
public class ConsentService {

    private final StudentDataConsentRepository consentRepository;
    private final StudentRepository studentRepository;
    private final ImportedDataDeletionService deletionService;

    public boolean hasActiveConsent(Long studentId) {
        return activeConsent(studentId).isPresent();
    }

    public Optional<StudentDataConsent> activeConsent(Long studentId) {
        return consentRepository.findFirstByStudentIdAndPurposeAndRevokedAtIsNullOrderByGrantedAtDesc(
                studentId, StudentDataConsent.PURPOSE_RISK_ANALYTICS);
    }

    @Transactional
    public StudentDataConsent grant(Long studentId) {
        return activeConsent(studentId).orElseGet(() -> {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
            return consentRepository.save(StudentDataConsent.builder()
                    .student(student)
                    .purpose(StudentDataConsent.PURPOSE_RISK_ANALYTICS)
                    .policyVersion(StudentDataConsent.CURRENT_POLICY_VERSION)
                    .build());
        });
    }

    /** Revoking consent deletes all of the student's imported data and recomputes their risk. */
    @Transactional
    public void revoke(Long studentId) {
        activeConsent(studentId).ifPresent(c -> {
            c.setRevokedAt(LocalDateTime.now());
            consentRepository.save(c);
        });
        deletionService.deleteAllImportedData(studentId);
    }
}
