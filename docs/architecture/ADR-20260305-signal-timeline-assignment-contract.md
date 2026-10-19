# ADR-20260305: Signal Timeline and Assignment Contract

- Date: 2026-03-05
- Status: Accepted

## Context

Signals already exposed status history, but the model did not capture assignment changes and did not clearly separate lifecycle events from ownership events. That made follow-up opaque once a report entered institutional handling.

`story:OCS-P1-038` requires every case to show who changed what, when, and who currently owns the case.

## Decision

Extend signal behavior with:

- current assignee on the signal response via `assignedToUsername`
- typed timeline events via `eventType`
- assignment mutation via `PATCH /api/signals/{id}/assign`

Timeline remains backend-owned and returns one normalized event list for frontend rendering.

## Consequences

Positive:

- responsibility is visible without reading internal notes
- assignment changes become auditable alongside lifecycle transitions
- detail UI can render one coherent case timeline instead of inferring events from raw status records

Tradeoffs:

- timeline schema becomes richer and requires migration support
- assignment currently targets usernames, not a dedicated institutional directory object
- comments remain separate from timeline until a later casework expansion

## Validation

- backend integration tests for assignment + lifecycle history
- RBAC test for assignment endpoint
- frontend Docker build and detail timeline UI coverage
