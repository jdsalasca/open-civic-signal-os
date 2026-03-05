# Concerns

**Analysis Date:** 2026-03-04

## P0 Concerns (Immediate)

### 1) Public Endpoint Principal Handling
- `GET /api/signals/prioritized` and `GET /api/signals/top-10` are publicly permitted, but controller mapping relies on `authentication.getName()` in flow.
- Risk: anonymous requests can produce server errors and damage trust surface.
- Files: `apps/api-java/src/main/java/org/opencivic/signalos/config/SecurityConfig.java`, `.../web/SignalController.java`.

### 2) Contract Drift
- OpenAPI does not fully represent active API surface used in runtime/frontend.
- Risk: external integration breakage, contract-first governance violation, hidden regressions.
- Files: `packages/contracts/openapi.yaml`, backend controllers, frontend API calls.

## P1 Concerns (High)

### 3) CI Policy Mismatch
- Agent gate workflow compiles backend with tests skipped.
- Risk: contradictory signal vs documented non-negotiable quality rules.
- File: `.github/workflows/agent-gate.yml`.

### 4) Playwright Tooling Reliability on Windows
- Wrapper path/process behavior can fail with shell/EPERM issues.
- Risk: frontend PRs cannot reliably produce mandatory UX evidence.
- File: `scripts/playwright-cli.mjs`.

### 5) Root Script Determinism
- `src/ingest.mjs` uses random IDs and current timestamps for mapped records.
- Risk: non-idempotent replays and reproducibility drift.
- Files: `src/ingest.mjs`, `src/prioritize.mjs` (parallel scoring implementation outside backend).

## P2 Concerns (Medium)

### 6) Documentation Quality Drift
- README contains malformed markdown sequence (`\n` literal in list).
- Risk: onboarding friction and low trust in docs quality.
- File: `README.md`.

### 7) Coverage Gate Effectiveness
- Coverage threshold currently zero and summary file reports 0 metrics.
- Risk: release gate becomes passable without meaningful test coverage.
- Files: `package.json`, `coverage/coverage-summary.json`.

## Mitigation Strategy

1. Stabilize public endpoint behavior + add regression tests.
2. Bring OpenAPI parity up to active routes with pagination/error schemas.
3. Align all gate workflows with published quality policy (remove skip-tests mismatch).
4. Harden cross-platform UX evidence pipeline and codify fallback behavior.
5. Enforce deterministic ingest/replay contracts and reduce duplicated scoring logic.

---
*Concerns analysis: 2026-03-04*
*Update after each risk remediation phase*
