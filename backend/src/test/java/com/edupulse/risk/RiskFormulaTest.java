package com.edupulse.risk;

import com.edupulse.model.RiskLevel;
import com.edupulse.service.risk.RiskFormula;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pure unit tests for the official rule-based formula (no Spring context). */
class RiskFormulaTest {

    @ParameterizedTest(name = "attendance {0}% -> penalty {1}")
    @CsvSource({
            "100.0, 0.0",
            "75.0, 0.0",      // threshold is inclusive-healthy
            "74.9, 0.15",
            "70.0, 7.5",
            "60.0, 22.5",
            "48.4, 39.9",
            "48.0, 40.0",     // cap reached
            "0.0, 40.0"
    })
    void attendancePenalty(double attendance, double expected) {
        assertEquals(expected, RiskFormula.attendancePenalty(attendance), 1e-9);
    }

    @ParameterizedTest(name = "marks {0}% -> penalty {1}")
    @CsvSource({
            "90.0, 0.0",
            "50.0, 0.0",
            "49.0, 1.0",
            "27.6, 22.4",
            "0.0, 50.0"
    })
    void marksPenalty(double marks, double expected) {
        assertEquals(expected, RiskFormula.marksPenalty(marks), 1e-9);
    }

    @Test
    @DisplayName("Missing data contributes zero (pending is never zero marks)")
    void nullInputsContributeZero() {
        RiskFormula.Result r = RiskFormula.evaluate(null, null, 0);
        assertEquals(0.0, r.totalScore());
        assertEquals(RiskLevel.LOW, r.riskLevel());
    }

    @Test
    @DisplayName("Missing deliverables: 5 points each, uncapped individually")
    void missingDeliverables() {
        assertEquals(0.0, RiskFormula.missingDeliverablesPenalty(0));
        assertEquals(30.0, RiskFormula.missingDeliverablesPenalty(6));
        assertEquals(0.0, RiskFormula.missingDeliverablesPenalty(-3), "negative counts are clamped");
    }

    @Test
    @DisplayName("Total is capped at 100")
    void totalCapped() {
        RiskFormula.Result r = RiskFormula.evaluate(0.0, 0.0, 10);
        assertEquals(100.0, r.totalScore());
        assertEquals(RiskLevel.HIGH, r.riskLevel());
    }

    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource({
            "0.0, LOW", "34.9, LOW", "35.0, MEDIUM", "64.9, MEDIUM", "65.0, HIGH", "100.0, HIGH"
    })
    void levelBands(double score, RiskLevel expected) {
        assertEquals(expected, RiskFormula.levelFor(score));
    }

    @Test
    @DisplayName("Showcase Student D scenario: 59.8% attendance, 27.6% marks, 6 missing -> HIGH")
    void studentDScenario() {
        RiskFormula.Result r = RiskFormula.evaluate(59.8, 27.6, 6);
        assertEquals(22.8, r.attendancePenalty());
        assertEquals(22.4, r.marksPenalty());
        assertEquals(30.0, r.missingDeliverablesPenalty());
        assertEquals(75.2, r.totalScore());
        assertEquals(RiskLevel.HIGH, r.riskLevel());
    }

    @Test
    @DisplayName("Level uses the unrounded total so rounding cannot cross a band")
    void levelUsesUnroundedTotal() {
        // 64.96 rounds to 65.0 for display but is still MEDIUM.
        RiskFormula.Result r = RiskFormula.evaluate(null, 50.0 - 64.96 + 30.0, 0); // marks penalty 34.96
        assertEquals(RiskLevel.LOW, r.riskLevel());
        RiskFormula.Result r2 = RiskFormula.evaluate(75.0 - (19.96 / 1.5), 5.0, 0); // 19.96 + 45 = 64.96
        assertEquals(65.0, r2.totalScore());
        assertEquals(RiskLevel.MEDIUM, r2.riskLevel());
    }
}
