package com.edupulse.service.importing;

import com.edupulse.model.AcademicImportBatch;
import com.edupulse.repository.*;
import com.edupulse.service.RiskAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Deletes imported academic data (one batch or all of a student's) and recomputes risk. */
@Service
@RequiredArgsConstructor
public class ImportedDataDeletionService {

    private final AcademicImportBatchRepository batchRepository;
    private final AttendanceSnapshotRepository snapshotRepository;
    private final ExamResultRepository examResultRepository;
    private final MarkRepository markRepository;
    private final RiskAssessmentService riskAssessmentService;

    @Transactional
    public void deleteBatch(Long studentId, Long batchId) {
        AcademicImportBatch batch = batchRepository.findById(batchId).orElse(null);
        if (batch == null || batch.getStudent() == null || !batch.getStudent().getId().equals(studentId)) {
            return; // not the caller's batch; nothing to do
        }
        removeRowsForBatch(batchId);
        batchRepository.delete(batch);
        riskAssessmentService.calculateRiskForStudent(studentId);
    }

    @Transactional
    public void deleteAllImportedData(Long studentId) {
        List<AcademicImportBatch> batches = batchRepository.findByStudentIdOrderByUploadedAtDesc(studentId);
        for (AcademicImportBatch b : batches) {
            removeRowsForBatch(b.getId());
        }
        batchRepository.deleteAll(batches);
        if (!batches.isEmpty()) {
            riskAssessmentService.calculateRiskForStudent(studentId);
        }
    }

    private void removeRowsForBatch(Long batchId) {
        snapshotRepository.deleteByBatchId(batchId);
        examResultRepository.deleteByBatchId(batchId);
        markRepository.findAll().stream()
                .filter(m -> batchId.equals(m.getImportBatchId()))
                .forEach(markRepository::delete);
    }
}
