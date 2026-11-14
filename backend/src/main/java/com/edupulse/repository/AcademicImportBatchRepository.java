package com.edupulse.repository;

import com.edupulse.model.AcademicImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicImportBatchRepository extends JpaRepository<AcademicImportBatch, Long> {
    List<AcademicImportBatch> findByStudentIdOrderByUploadedAtDesc(Long studentId);
    Optional<AcademicImportBatch> findByStudentIdAndContentSha256(Long studentId, String contentSha256);
}
