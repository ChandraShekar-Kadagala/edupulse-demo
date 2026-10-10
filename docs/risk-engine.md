# Explainable Academic Risk Engine

## 1. Philosophy
The EduPulse Risk Engine is a deterministic, rule-based decision support system. It is **not** a black-box AI model. It exists to provide early warnings to faculty, not to punish or automatically penalize students.

## 2. Thresholds
Risk scores are calculated on a scale of 0 to 100.
- **LOW RISK (0-34):** Student is performing optimally.
- **MEDIUM RISK (35-64):** Student shows early signs of academic friction. Supportive monitoring recommended.
- **HIGH RISK (65-100):** Student is experiencing significant academic difficulty. Active intervention required.

## 3. Rule Weights & Calculation
The engine evaluates the following factors:

1. **Attendance Drop:**
   - Base requirement: 75%
   - Penalty: If attendance falls below 75%, risk score increases by `(75 - ActualPercentage) * 1.5`. Maximum penalty: ~40 points.

2. **Assessment Performance:**
   - Base requirement: 50% average across all valid marks.
   - Penalty: If average marks fall below 50%, risk score increases by `(50 - ActualAverage) * 1.0`. Maximum penalty: 50 points.

3. **Missing Assignments:**
   - Penalty: 5 points for every assignment where the submission date is missing or past the due date.

*Note: The total score is strictly capped at 100.*

## 4. Historical Snapshots
To prevent data loss and allow for trend analysis (Before vs. After Intervention), risk calculations do NOT overwrite existing assessments. Every time the engine runs, a new `RiskAssessment` record is generated with a `calculatedAt` timestamp and immutable `RiskFactor` children.
