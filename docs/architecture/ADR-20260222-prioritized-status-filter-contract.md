# ADR-20260222 Prioritized Status Filter Contract

- Status: accepted
- Date: 2026-02-22

## Context

Dashboard quick filters displayed active state but did not always alter the prioritized dataset query. This created "visual-only filters" and inconsistent behavior between UI state and data.

## Decision

Add an optional `status` query parameter to `GET /api/signals/prioritized`:

- Accept comma-separated lifecycle values (for example `NEW`, `IN_PROGRESS`, `RESOLVED`).
- Keep existing default behavior when `status` is omitted (exclude `FLAGGED` and `REJECTED`).
- Keep prioritization scoring backend-owned; frontend only requests filtered sets.

## Consequences

- Frontend quick filters can be contract-backed and deterministic.
- API remains backwards-compatible for existing clients that do not send `status`.
- Playwright coverage must assert that filter actions emit the expected query parameter.
