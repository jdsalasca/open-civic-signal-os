# ADR-20260307: Community Decision Ledger Contract

- Status: Accepted
- Date: 2026-03-07
- Story: `OCS-P1-047`

## Context

Open Civic Signal OS already supports proposals, deliberation, project boards, and governance documents. The missing trust surface was a formal decision record that links community intent to execution ownership. Without that ledger, communities can debate and plan work but still cannot audit what was formally approved, why it was approved, and who is expected to execute it.

## Decision

Introduce a backend-owned community decision ledger with:

- explicit decision records under `/api/community/decisions`
- one mandatory source link to either a structured proposal or a governance document
- optional link to an execution project board
- explicit approval basis type plus free-text approval basis summary
- explicit execution owner when available
- a dedicated permission scope: `MANAGE_DECISION_LEDGER`

Frontend adds a dedicated `/communities/decisions` route for browse and create workflows. Creation permission is derived from the community permission policy matrix instead of hardcoded role checks.

## Consequences

Positive:

- communities can trace proposal -> decision -> execution in one place
- approval basis is visible before full voting infrastructure lands
- project boards and governance documents become auditable downstream links rather than separate silos
- the permission model stays consistent with community-scoped policies

Trade-offs:

- the initial ledger supports create/list/detail only; decision amendment history is deferred
- vote-result linkage remains partial until `OCS-P1-042` lands
- list filtering is currently in service-layer memory after community membership enforcement; database-level filtered queries can be added later if volume demands it

## Validation

- targeted backend integration coverage for create/list/detail and linked execution context
- OpenAPI updated in the same change set
- GSD state, backlog, and changelog updated with the shipped story
