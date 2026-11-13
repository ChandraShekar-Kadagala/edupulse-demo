package com.edupulse.service;

import com.edupulse.model.*;
import com.edupulse.repository.AttendanceRepository;
import com.edupulse.repository.InterventionRepository;
import com.edupulse.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Observational before/after analytics for interventions (Phase 4).
 *
 * <p>For each non-cancelled intervention the baseline is the latest official risk snapshot taken at or
 * before the intervention started, and the outcome is the latest snapshot taken after it started.
 * Only interventions with both are "evaluable". Attendance change compares dated attendance records
 * before vs. after the start date. Marks are not dated in the data model, so marks change is not
 * computed (reported explicitly rather than guessed).
 *
 * <p>These are simple before/after comparisons without a control group: they describe what happened,
 * not what the intervention caused. Groups smaller than {@link #MIN_GROUP_SIZE} are flagged.
 */
@Service
@RequiredArgsConstructor
public class InterventionEffectivenessService {

    public static final int MIN_GROUP_SIZE = 5;
    static final double UNCHANGED_TOLERANCE = 0.5; // risk points

    private final InterventionRepository interventionRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final InterventionService interventionService;

    public record InterventionOutcome(
            Long interventionId, Long studentId, String type, String priority, String status,
            LocalDate startDate, Double baselineRisk, String baselineLevel, LocalDateTime baselineAt,
            Double outcomeRisk, String outcomeLevel, LocalDateTime outcomeAt, Double riskChange,
            Double attendanceBefore, Double attendanceAfter, Double attendanceChange,
            int followUpCount, boolean evaluable, String exclusionReason) {
    }

    public record GroupSummary(String group, int interventions, int evaluable, Double meanRiskChange,
                               int improved, int unchanged, int worsened, boolean smallSample) {
    }

    public record EffectivenessReport(
            LocalDateTime generatedAt, String methodology, String causalityNote, int minGroupSize,
            int totalInterventions, int completedInterventions, Double completionRate,
            int interventionsWithFollowUp, Double followUpCompletionRate,
            GroupSummary overall, List<GroupSummary> byType, List<GroupSummary> byPriority, List<GroupSummary> byStatus,
            Map<String, Long> excluded, String marksChangeNote, String subjectBreakdownNote,
            List<InterventionOutcome> interventions) {
    }

    @Transactional(readOnly = true)
    public EffectivenessReport report() {
        List<Intervention> all = interventionRepository.findAll().stream()
                .filter(i -> i.getStatus() != InterventionStatus.CANCELLED)
                .sorted(Comparator.comparing(Intervention::getId))
                .toList();
        List<InterventionOutcome> outcomes = all.stream().map(this::evaluate).toList();

        int completed = (int) all.stream().filter(i -> i.getStatus() == InterventionStatus.COMPLETED).count();
        int withFollowUp = (int) outcomes.stream().filter(o -> o.followUpCount() > 0).count();

        Map<String, Long> excluded = outcomes.stream().filter(o -> !o.evaluable())
                .collect(Collectors.groupingBy(InterventionOutcome::exclusionReason, TreeMap::new, Collectors.counting()));

        return new EffectivenessReport(
                LocalDateTime.now(),
                "Baseline = latest official risk snapshot at or before the intervention start; outcome = latest snapshot after the start. "
                        + "Risk change = outcome - baseline (negative means lower risk). Changes within +/-" + UNCHANGED_TOLERANCE + " points count as unchanged.",
                "Observational before/after comparison without a control group. Results describe what happened after interventions; "
                        + "they do not show that the intervention caused the change.",
                MIN_GROUP_SIZE,
                all.size(), completed, rate(completed, all.size()),
                withFollowUp, rate(withFollowUp, all.size()),
                summarize("All interventions", outcomes),
                grouped(outcomes, o -> Objects.requireNonNullElse(o.type(), "Unspecified")),
                grouped(outcomes, o -> Objects.requireNonNullElse(o.priority(), "Unspecified")),
                grouped(outcomes, o -> Objects.requireNonNullElse(o.status(), "Unspecified")),
                excluded,
                "Marks change is not reported: marks are not dated, so they cannot be split into before/after the intervention.",
                "Interventions are not linked to a subject in the data model, so outcomes by subject are not available.",
                outcomes);
    }

    InterventionOutcome evaluate(Intervention i) {
        Long studentId = i.getStudent() != null ? i.getStudent().getId() : null;
        LocalDateTime start = InterventionService.startOf(i);
        int followUps = i.getFollowUps() != null ? i.getFollowUps().size() : 0;
        LocalDate startDate = start != null ? start.toLocalDate() : null;

        Optional<RiskAssessment> baseline = interventionService.baselineSnapshot(i);
        Optional<RiskAssessment> outcome = (studentId == null || start == null) ? Optional.empty()
                : riskAssessmentRepository.findByStudentId(studentId).stream()
                        .filter(r -> r.getCalculatedAt() != null && r.getCalculatedAt().isAfter(start))
                        .max(Comparator.comparing(RiskAssessment::getCalculatedAt));

        Double attBefore = null, attAfter = null;
        if (studentId != null && startDate != null) {
            List<Attendance> att = attendanceRepository.findByStudentId(studentId);
            attBefore = attendancePct(att.stream().filter(a -> a.getDate() != null && a.getDate().isBefore(startDate)).toList());
            attAfter = attendancePct(att.stream().filter(a -> a.getDate() != null && !a.getDate().isBefore(startDate)).toList());
        }

        String reason = null;
        if (studentId == null) reason = "No student linked";
        else if (start == null) reason = "No start date";
        else if (baseline.isEmpty()) reason = "No risk snapshot before the intervention started";
        else if (outcome.isEmpty()) reason = "No risk snapshot after the intervention started";

        boolean evaluable = reason == null;
        Double change = evaluable ? round1(outcome.get().getRiskScore() - baseline.get().getRiskScore()) : null;
        return new InterventionOutcome(
                i.getId(), studentId, i.getType(), i.getPriority(), i.getStatus() != null ? i.getStatus().name() : null,
                startDate,
                baseline.map(RiskAssessment::getRiskScore).orElse(null),
                baseline.map(r -> r.getRiskLevel().name()).orElse(null),
                baseline.map(RiskAssessment::getCalculatedAt).orElse(null),
                outcome.map(RiskAssessment::getRiskScore).orElse(null),
                outcome.map(r -> r.getRiskLevel().name()).orElse(null),
                outcome.map(RiskAssessment::getCalculatedAt).orElse(null),
                change,
                attBefore, attAfter,
                attBefore != null && attAfter != null ? round1(attAfter - attBefore) : null,
                followUps, evaluable, reason);
    }

    static GroupSummary summarize(String name, List<InterventionOutcome> outcomes) {
        List<Double> changes = outcomes.stream().filter(InterventionOutcome::evaluable).map(InterventionOutcome::riskChange).toList();
        int improved = (int) changes.stream().filter(c -> c < -UNCHANGED_TOLERANCE).count();
        int worsened = (int) changes.stream().filter(c -> c > UNCHANGED_TOLERANCE).count();
        Double mean = changes.isEmpty() ? null : round1(changes.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
        return new GroupSummary(name, outcomes.size(), changes.size(), mean, improved, changes.size() - improved - worsened,
                worsened, changes.size() < MIN_GROUP_SIZE);
    }

    private static List<GroupSummary> grouped(List<InterventionOutcome> outcomes,
                                              java.util.function.Function<InterventionOutcome, String> key) {
        return outcomes.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList()))
                .entrySet().stream().map(e -> summarize(e.getKey(), e.getValue())).toList();
    }

    private static Double attendancePct(List<Attendance> att) {
        if (att.isEmpty()) return null;
        return round1((double) att.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count() / att.size() * 100.0);
    }

    private static Double rate(int part, int whole) {
        return whole == 0 ? null : round1((double) part / whole * 100.0);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
