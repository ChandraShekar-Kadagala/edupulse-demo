# Intervention effectiveness analytics

Endpoint: `GET /api/analytics/interventions/effectiveness` (FACULTY, ADMIN). UI: "Intervention outcomes"
card on the faculty and admin dashboards. Implementation: `InterventionEffectivenessService`.

## Method
- **Baseline**: latest official risk snapshot at or before the intervention start
  (`start_date`, else `created_at`).
- **Outcome**: latest official risk snapshot after the start.
- **Risk change** = outcome − baseline (negative = lower risk). Within ±0.5 points counts as unchanged.
- **Attendance change**: attendance % from dated records after the start minus before the start.
- **Completion rate**: COMPLETED / non-cancelled interventions. **Follow-up rate**: interventions with at
  least one follow-up / non-cancelled interventions.
- Cancelled interventions are excluded. Interventions without a baseline or outcome snapshot are listed
  as *not evaluable* with the reason; they are never estimated.

## Honesty rules
- Results are **observational** before/after comparisons without a control group; the API and UI say that
  they do not establish causation.
- Every group reports its sample size; groups with fewer than 5 evaluable interventions are flagged.
- Marks change is **not** reported because marks are not dated in the data model.
- Outcomes by subject are **not** reported because interventions are not linked to a subject.
- The report is only available to authenticated faculty/admins; nothing is published publicly.

## Follow-up risk scores
`POST /api/interventions/{id}/followups` ignores any client-supplied scores. The server records
`previousRiskScore` = baseline snapshot (null if none existed before the start) and
`newRiskScore` = the student's current official snapshot.

## Student timeline
`GET /api/students/{id}/risk/history` (owner, faculty, admin) returns the ordered snapshot history and
intervention start dates. A single snapshot is not a trend.

## Known data limitation (demo)
The seeded showcase interventions start before the first risk snapshot was computed, so they are reported
as "No risk snapshot before the intervention started". New interventions created through the API get a
start date and use real snapshots.
