# Quality Rubric

Score each PR 0-2 on each dimension.

- Correctness: behavior matches acceptance criteria.
- Safety: rollback path and risk notes are explicit.
- Test Depth: includes happy path + edge/regression.
- Contract Integrity: API/schema/docs aligned.
- Operational Readiness: run/deploy instructions validated.
- Clarity: PR is understandable and scoped.

## Grade Bands

- 10-12: merge-ready.
- 7-9: needs focused revisions.
- <=6: rework before review.

## Hard Fails

- Missing Story ID.
- No verification evidence.
- No rollback plan.
- Undocumented behavior change.
