# Agent Operations Runbook

This runbook standardizes agent execution quality for Open Civic Signal OS.

## 1. Intake

- Select one issue with clear scope and priority label.
- Map issue to one Story ID from `docs/agent/user-stories.md`.
- Confirm impacted layers: backend, contracts, frontend, docs.

## 2. Pre-Implementation Gate

- Read `AGENTS.md` and `docs/agent/execution-checklists.md`.
- Define acceptance criteria and rollback plan before coding.
- Confirm deterministic behavior expectations for domain logic.

## 3. Implementation Pattern

- Implement smallest vertical slice first.
- Keep public contract changes explicit and documented.
- Add tests and docs in the same PR.

## 4. Verification

- Run: `npm run prioritize`
- Run: `npm run build:web`
- Run Java build if backend changed.
- Attach outputs to PR body.

## 5. PR Quality

PR must include:

- Issue reference (`#123`)
- Story ID (`story:*`)
- Scope checkboxes
- Verification checklist
- Risk notes and rollback plan

## 6. Blocker Protocol

If blocked, open `.github/ISSUE_TEMPLATE/agent-blocker.yml` immediately.
Do not continue by guessing hidden product decisions.

## 7. Weekly Metrics Review

- Read `docs/kanban.md` section `Weekly Delivery Metrics`.
- Track:
  - throughput (closed issues),
  - open blockers,
  - reopened issues (rework signal).
- If blockers or rework rise for 2 consecutive weeks, reduce WIP and split story scope.
