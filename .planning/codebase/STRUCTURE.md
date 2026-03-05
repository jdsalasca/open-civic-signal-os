# Structure

**Analysis Date:** 2026-03-04

## Repository Layout

- `apps/api-java/` - Spring Boot API, domain services, repositories, migrations, tests.
- `apps/web-react/` - React frontend app (views/components/stores/styles/tests).
- `packages/contracts/` - OpenAPI contract (`openapi.yaml`).
- `docs/` - governance, backlog, standards, playbooks, architecture and quality docs.
- `scripts/` - agent/quality/release/contract helper scripts.
- `infra/` - docker compose runtime definitions.
- `src/` + `examples/` - root-level MVP ingestion/prioritization scripts and sample data.

## Backend Structure (`apps/api-java`)

- `src/main/java/org/opencivic/signalos/web/` - controllers and API DTO mapping.
- `src/main/java/org/opencivic/signalos/service/` - domain/business services.
- `src/main/java/org/opencivic/signalos/repository/` - JPA repositories.
- `src/main/java/org/opencivic/signalos/domain/` - entities/value objects/enums.
- `src/main/resources/db/migration/` - Flyway migrations (`V1...V11`).
- `src/test/java/org/opencivic/signalos/` - integration/security/regression tests.

## Frontend Structure (`apps/web-react/src`)

- `views/` - route-level screens (Dashboard, SignalDetail, ReportSignal, Communities, etc.).
- `components/` and `components/ui/` - shared UI + domain widgets.
- `store/` - persisted global state (`useAuthStore`, `useCommunityStore`, `useSettingsStore`).
- `api/` - axios client/interceptors.
- `styles/` - theme and base styling.
- `tests/` - Playwright specs for UX and integrity checks.
- `constants/` and `utils/` - reusable UX/domain helpers.

## Contracts and Governance

- Contract source: `packages/contracts/openapi.yaml`.
- Backlog source: `docs/community/issue-backlog.md`, `docs/community/current-backlog.md`.
- Agent governance: `AGENTS.md`, `.agentic-rules`, `CODEX.md`, `GEMINI*.md`.

## Automation and CI

- Workflows: `.github/workflows/` (CI, gates, release quality, UX evidence, ADR checks, issue seeding).
- Script entrypoints: root `package.json` (`agent:*`, `quality:*`, `docker:*`).

## Data/Artifacts Paths

- Playwright evidence output: `output/playwright/`.
- Coverage summary: `coverage/coverage-summary.json`.
- Generated frontend build artifacts (forbidden in commits): `apps/web-react/dist/`.

## Key Structural Notes

- Monorepo keeps runtime code + governance + automation in one place.
- Backend and frontend are clearly separated by app boundaries.
- Contract exists as dedicated package but currently requires parity hardening with live endpoints.

---
*Structure analysis: 2026-03-04*
*Update after major directory or workflow changes*
