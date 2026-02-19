# GEMINI_INSTRUCTIONS.md

## Objective

Implement roadmap issues with production-grade quality in this monorepo.

## Product context

Open Civic Signal OS converts community signals into transparent, auditable, and executable public backlogs.

## Mandatory stack

- Backend: Java 21 + Spring Boot
- Frontend: React + TypeScript + Vite
- Contracts: OpenAPI in `packages/contracts/openapi.yaml`
- Data shape: JSON-first now, PostgreSQL-ready design

## Work execution protocol (required)

1. Pick one issue labeled `agent-ready`.
2. Read the issue and linked docs before coding:
   - `AGENTS.md`
   - `docs/agent/agent-capability-matrix.md`
   - `docs/agent/ux-quality-bar.md`
   - `docs/STACK.md`
   - `docs/MONOREPO.md`
   - `docs/ideas.md`
   - `docs/kanban.md`
3. Create/checkout branch: `feat/<issue-number>-<short-slug>`.
4. Implement in this order:
   - backend domain rules/API
   - contract update
   - frontend integration
   - docs/examples update
5. Run verification locally (or explain blockers).
6. Open PR with structured summary (template below).

## Non-negotiable engineering rules

- Prioritization logic must live in backend, never only in frontend.
- Any API response shape change requires OpenAPI update.
- Keep scoring deterministic and explainable.
- Include score component breakdown in outputs when relevant.
- Never introduce hidden weighting or opaque ranking.

## Required PR summary format

- Issue: `#<number>`
- Scope: backend | frontend | contracts | docs
- API changes: list endpoints changed/added
- Contract changes: file and summary
- Data impact: schema/fields added/changed
- Verification: commands run and outcomes
- Risks: known limitations and follow-ups

## Quality gates before merge

- Backend tests pass (`mvn -q test`)
- Frontend builds (`npm run build:web`)
- Main script still works (`npm run prioritize`)
- Docs updated for behavior change
- No dead code or placeholder TODOs in core paths
- Frontend PRs include Playwright CLI visual evidence in `output/playwright/` and PR body

## Critical anti-patterns (must avoid)

- Do not commit generated files (`dist`, `test-results`, screenshots, local artifacts).
- Do not add dependencies unless explicitly needed by the issue and justified in PR.
- Do not mix unrelated objectives in one PR.
- Do not modify docs with malformed escape sequences.
- Do not break valid JSON in config files.
- Do not skip tests or bypass required checks.
- Do not merge frontend changes without Playwright visual audit evidence.

## Security and trust constraints

- Avoid collecting unnecessary personal data.
- Prefer anonymized/public-safe fields in UI exports.
- Ensure output traceability (source + formula version + timestamp).

## When blocked

If a blocker appears, do not stop silently. Open/update issue comment with:

- blocker description
- attempted alternatives
- minimal decision needed to continue

## Container CI/CD

- Workflow: `.github/workflows/docker-images.yml`
- On PR: builds API/Web images and runs smoke tests.
- On push to `main/develop`: publishes API/Web images to GHCR.
- Tags include `sha-*`, branch tags, and `latest` on default branch.
