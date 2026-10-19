# ADR-20260319: Community Open Data Export And Scoped API Contract

- Status: Accepted
- Date: 2026-03-19
- Story: `OCS-P1-052`

## Context

Open Civic Signal OS already exposes community decisions, trust metrics, moderation, privacy controls, and onboarding/help. The next trust-critical gap is controlled civic data export: communities need interoperable CSV/JSON delivery for public records and external consumers, but unrestricted API access would weaken auditability and make rate control and revocation harder to prove.

Before this story, open-data delivery was not a first-class contract, external consumers had no scoped token model, and export usage was not visible as a dedicated community workspace in the frontend.

## Decision

Introduce a backend-owned open data contract with:

- one community-scoped export center for:
  - signals
  - proposals
  - proposal voting summaries
  - decisions
  - trust metrics
- one export format model:
  - `CSV`
  - `JSON`
- one token model for scoped external access:
  - create token
  - revoke token
  - list scope, status, last-used time, and rate limit
- one permission scope for management:
  - `MANAGE_OPEN_DATA_EXPORTS`
- one auditable access trail for export/token usage
- backend-owned rate limiting for token access instead of frontend-enforced throttling

The frontend renders the export workspace, dataset actions, and token guidance, but export eligibility, token scope, rate limiting, and audit logging remain backend-owned.

## Consequences

Positive:

- communities can publish structured civic data without opening unrestricted API access
- scoped tokens support external integrations while preserving revocation and throttling
- export activity is auditable and tied to a community context
- the frontend can present one clear open-data workspace instead of scattering export actions across unrelated screens

Trade-offs:

- the first release ships a focused token-and-export model rather than a full developer portal
- rate limits are intentionally explicit and conservative to protect backend stability
- community data export policy still depends on backend ownership, so the frontend remains presentation-only

## Validation

- targeted backend integration coverage for export delivery, token scope checks, rate limits, and audit logging
- OpenAPI updated in the same change set
- frontend open-data workspace and nav wiring added with Docker-backed validation in the implementation run
- host `agent:preflight` remains blocked by missing `tsc`, and Docker browser evidence could not be rerun in the final session when the dev runtime was unavailable
- GSD planning/state/backlog/changelog updated with the shipped story
