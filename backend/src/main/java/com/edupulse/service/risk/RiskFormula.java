package com.edupulse.service.risk;

import com.edupulse.model.RiskLevel;

/**
 * The official EduPulse rule-based risk formula — the single source of truth.
 *
 * <p>Pure and side-effect free so it can be unit-tested exhaustively and reused by the
 * persisted risk engine, the explanation endpoint and the what-if simulator. Changing any
 * constant here changes official risk scores; update {@code docs/risk-engine.md} and the
 * formula tests in the same change.
 *
 * <pre>
 * attendancePenalty = attendance &lt; 75 ? min((75 - attendance) * 1.5, 40) : 0
 * marksPenalty      = avgMarks   &lt; 50 ? min((50 - avgMarks)   * 1.0, 50) : 0
 * missingPenalty    = missingOrLate * 5
 * total             = min(attendancePenalty + marksPenalty + missingPenalty, 100)
 * level             = total &gt;= 65 ? HIGH : total &gt;= 35 ? MEDIUM : LOW
 * </pre>
 *
 * A {@code null} attendance or marks input means "no data", which contributes zero penalty —
 * pending assessments are never treated as zero marks.
 */
public final class RiskFormula {

    public static final String VERSION = "rules-v1";

    public static final double ATTENDANCE_THRESHOLD = 75.0;
    public static final double ATTENDANCE_WEIGHT = 1.5;
    public static final double ATTENDANCE_MAX_PENALTY = 40.0;

    public static final double MARKS_THRESHOLD = 50.0;
    public static final double MARKS_WEIGHT = 1.0;
    public static final double MARKS_MAX_PENALTY = 50.0;

    public static final double MISSING_DELIVERABLE_POINTS = 5.0;

    public static final double SCORE_CAP = 100.0;
    public static final double MEDIUM_THRESHOLD = 35.0;
    public static final double HIGH_THRESHOLD = 65.0;

    public static final String DESCRIPTION =
            "Total Risk Score = Min(Attendance Penalty [max 40] + Marks Penalty [max 50] + Missing Deliverables [5 pts each], 100.0). "
            + "Risk Categories: LOW (<35.0), MEDIUM (35.0-64.9), HIGH (>=65.0). Pending assessments are never treated as zero.";

    private RiskFormula() {
    }

    /** Result of one formula evaluation. All values are rounded to one decimal place. */
    public record Result(
            double attendancePenalty,
            double marksPenalty,
            double missingDeliverablesPenalty,
            double totalScore,
            RiskLevel riskLevel) {
    }

    public static double attendancePenalty(Double attendancePercentage) {
        if (attendancePercentage == null || attendancePercentage >= ATTENDANCE_THRESHOLD) {
            return 0.0;
        }
        return Math.min((ATTENDANCE_THRESHOLD - attendancePercentage) * ATTENDANCE_WEIGHT, ATTENDANCE_MAX_PENALTY);
    }

    public static double marksPenalty(Double averageMarksPercentage) {
        if (averageMarksPercentage == null || averageMarksPercentage >= MARKS_THRESHOLD) {
            return 0.0;
        }
        return Math.min((MARKS_THRESHOLD - averageMarksPercentage) * MARKS_WEIGHT, MARKS_MAX_PENALTY);
    }

    public static double missingDeliverablesPenalty(long missingOrLateCount) {
        return Math.max(0, missingOrLateCount) * MISSING_DELIVERABLE_POINTS;
    }

    public static RiskLevel levelFor(double score) {
        if (score >= HIGH_THRESHOLD) {
            return RiskLevel.HIGH;
        }
        if (score >= MEDIUM_THRESHOLD) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    public static Result evaluate(Double attendancePercentage, Double averageMarksPercentage, long missingOrLateCount) {
        double att = attendancePenalty(attendancePercentage);
        double marks = marksPenalty(averageMarksPercentage);
        double missing = missingDeliverablesPenalty(missingOrLateCount);
        double total = Math.min(att + marks + missing, SCORE_CAP);
        double roundedTotal = round1(total);
        // Level is derived from the unrounded total so rounding can never move a student across a band.
        return new Result(round1(att), round1(marks), round1(missing), roundedTotal, levelFor(total));
    }

    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
