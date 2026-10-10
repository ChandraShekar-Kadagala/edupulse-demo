package com.edupulse.repository;

import com.edupulse.model.EncryptedRiskRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EncryptedRiskRecordRepository extends JpaRepository<EncryptedRiskRecord, Long> {
    List<EncryptedRiskRecord> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
