# Integrations

**Analysis Date:** 2026-03-04

## External Services

### PostgreSQL
- Purpose: Persistent storage for users, signals, community, engagement, status history.
- Config: `SPRING_DATASOURCE_*` env vars.
- Local runtime: `civic-db` in `infra/docker-compose.yml`.
- Migrations: Flyway scripts in `apps/api-java/src/main/resources/db/migration`.

### SMTP / Mail Delivery
- Purpose: Verification and welcome email flows.
- Config: `SPRING_MAIL_*` + fallback profile config in `application.yml`.
- Local runtime: `civic-mail` (Mailpit) in compose.
- Risk noted in backlog: delivery failures must surface degraded-mode UX.

### JWT/Auth Context
- Purpose: Access token + refresh cookie auth model.
- Config: `JWT_SECRET` and CORS origin settings.
- Integration points: `AuthController`, `JwtService`, request interceptor in frontend.

### Browser Automation / UX Evidence
- Purpose: Playwright-based UX proof for frontend-impacting changes.
- Entrypoint: `npm run agent:ux:pw` -> `scripts/playwright-cli.mjs`.
- Artifact path: `output/playwright/`.

## Internal Integration Boundaries

### Backend <-> Frontend API
- Contract location: `packages/contracts/openapi.yaml`.
- Frontend client calls through `apps/web-react/src/api/axios.ts`.
- Headers: `Authorization`, `X-Community-Id`.
- Noted concern: implementation has routes not fully represented in OpenAPI.

### Domain Modules (backend)
- Signals and prioritization: `SignalController` + `PrioritizationServiceImpl`.
- Community collaboration: `CommunityCollaborationController` + `CommunityCollaborationService`.
- Auth/security: `AuthController`, `SecurityConfig`, JWT filter/service.

## CI/CD and Governance Integrations

- GitHub Actions workflows enforce context, quality, ADR/contract and UX evidence gates.
- Agent quality scripts: `scripts/agent-*.mjs`, `scripts/check-contract-adr.mjs`, `scripts/release-quality-gate.mjs`.
- Issue seeding integration: `.github/workflows/seed-community-issues.yml`.

## Observability Integrations

- Health endpoints (`/actuator/health`, `/api/health`) available.
- Metrics exposed via actuator and custom micrometer counters/timers.
- Prioritized endpoint metrics test exists (`PrioritizedMetricsEndpointIT`).

## Integration Risks

- Contract drift between backend routes and OpenAPI can break external consumers.
- Windows path/process handling in Playwright wrapper can block mandatory UX evidence.
- Preflight/CI policy mismatch (skip tests in one gate) reduces trust in integration quality signals.

---
*Integration analysis: 2026-03-04*
*Update after API/infra/auth boundary changes*
