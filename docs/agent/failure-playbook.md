# Failure Playbook

Playbook for fast, safe handling of incidents.

## Severity Levels

- S1: user-facing outage or trust-critical failure.
- S2: degraded behavior with workaround.
- S3: minor defect with low impact.

## Response Flow

1. Reproduce issue.
2. Classify severity.
3. Add failing test.
4. Implement minimal patch.
5. Validate via quality gates.
6. Publish incident summary and follow-up tasks.

## Mandatory Outputs

- Root cause.
- Fix scope.
- Regression guard.
- Rollback path.
- Preventive action.

## Communication Template

- What happened.
- Who was impacted.
- What was fixed.
- What remains open.
- Next review date.
