# Agent Evaluation Pack

This pack standardizes PR evaluation quality across autonomous agents.

## Components

- `docs/agent/story-acceptance-checklist.md`
- `docs/agent/review-comment-catalog.md`
- `scripts/agent-evaluate-pr.mjs`
- `.github/workflows/agent-evaluation.yml`

## Objective

- Prevent low-signal PRs.
- Enforce evidence-based verification.
- Improve review consistency and reduce rework.

## Scoring Model

Each criterion scores:

- 0: missing
- 1: partially present
- 2: clearly present

Dimensions:

1. Issue linkage
2. Story ID clarity
3. Scope declaration
4. Verification evidence
5. Risk + rollback
6. Contract/docs alignment

## Gate

Default pass threshold: `8/12`.
Override with env `MIN_AGENT_SCORE` in CI if needed.
