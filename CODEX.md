# CODEX.md

## Codex Execution Contract (Open Civic Signal OS)

This file defines mandatory behavior for Codex-based implementation agents.

## Mission Alignment

- Keep civic prioritization transparent, deterministic, and auditable.
- Preserve backend ownership of ranking logic.
- Prefer small, reviewable changes with explicit rollback.

## Non-Negotiable Rules

- One objective per PR.
- No generated artifacts in commits (`dist`, `test-results`, screenshots, temp output files).
- No dependency additions unless explicitly required by the issue.
- No hidden or undocumented API shape changes.
- No CI weakening (never skip tests in backend validation).
- No malformed docs/config (invalid JSON, broken markdown formatting).
- Frontend-impacting PRs must include Playwright CLI visual evidence.

## Required Pre-Push Verification

- `npm run quality:quick`
- `npm run backlog:current:check`
- `npm run agent:context:check`
- If Java backend changed: run `mvn -q test` in `apps/api-java`
- If frontend changed: run Playwright CLI flow audit and attach evidence paths

## Scope Discipline

- Backend issue: avoid unrelated frontend redesign.
- Docs/backlog issue: avoid runtime behavior changes.
- CI issue: avoid product logic changes unless needed for fix.

## PR Evidence Template

- Objective:
- Files changed:
- Validation commands executed:
- Contract impact:
- Risks:
- Rollback:
- UX Evidence (Playwright):
