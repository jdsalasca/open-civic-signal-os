# Testing

**Analysis Date:** 2026-03-04

## Test Stack

**Backend:**
- Spring Boot integration tests with MockMvc and profile-specific test config.
- Coverage areas include signal detail/meta, prioritization regression, duplicate detection, and RBAC/auth hardening.
- Test location: `apps/api-java/src/test/java/org/opencivic/signalos`.

**Frontend:**
- Playwright specs under `apps/web-react/src/tests`.
- Focus includes auth edge cases, role switch UX, community threads paging, state integrity, mobile navigation.

## Test Organization

- Backend tests are mostly integration-style (`*IT`, `*Test`) and validate endpoint behavior + domain determinism.
- Frontend tests are end-to-end style against routed flows and UX contracts.
- Additional quality checks implemented as node scripts (`scripts/agent-*.mjs`).

## Quality Gates and Commands

- Root preflight script: `npm run agent:preflight`.
- Backend tests: `mvn -q test` (in `apps/api-java`).
- Frontend build: `npm run build:web`.
- Release quality gate: `npm run quality:release`.

## CI Coverage

- `.github/workflows/ci.yml` runs Node prioritize flow and backend tests.
- UX evidence gate checks PR body for Playwright proof when frontend files change.
- Multiple governance gates verify context, backlog references, ADR/contract consistency.

## Current Gaps / Fragility

- Playwright wrapper behavior can fail on Windows path/process combinations.
- Agent gate workflow still has a backend package step with `-DskipTests` (policy mismatch).
- Release coverage threshold is configured as zero, reducing enforceability.
- OpenAPI parity tests are not yet comprehensive for all active endpoints.

## Recommended Testing Priorities

1. Add regression test for anonymous access to publicly permitted signal endpoints.
2. Add contract parity checks for endpoints currently used by frontend but absent in OpenAPI.
3. Strengthen Windows execution path tests for `agent:ux:pw`.
4. Enforce non-zero coverage thresholds and meaningful coverage summary generation.

---
*Testing analysis: 2026-03-04*
*Update after major test strategy or CI gate changes*
