# ADR-20261116: Nearest-city endpoint contract

Status: accepted
Date: 2026-11-16

## Context

The onboarding flow uses the browser location to propose the closest active city. The backend
route `GET /api/communities/nearest?latitude=...&longitude=...` has been live since the place
catalog landed, but it was missing from `packages/contracts/openapi.yaml`, so contract parity
counted it as an undocumented active route.

## Decision

Document the route as a public, read-only lookup:

- required `latitude` and `longitude` query parameters (double);
- `200` with the existing `Community` schema, or `404` when no active city exists near the
  coordinate;
- no authentication, no persisted coordinates, no PII in the response.

## Consequences

- The onboarding flow can rely on the contract instead of an implicit route.
- Contract parity is clean without adding an exception to `INTENTIONALLY_UNDOCUMENTED`.
- No database, migration or response-shape change: the endpoint already returned `Community`.
- The public surface stays read-only; coordinates are used for the lookup and are not stored.
