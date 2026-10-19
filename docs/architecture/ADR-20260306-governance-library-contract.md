# ADR-20260306 Governance Library Contract

- Status: accepted
- Date: 2026-03-06

## Context

`story:OCS-P1-046` requires one governance library where communities can find rules, minutes, budgets, reports, and agreements before conflicts escalate. The system already supports proposals, deliberation, and execution, but it does not yet provide a searchable repository for formal community records with explicit visibility and version history.

## Decision

Add backend-owned governance documents with:

- one document domain linked to one community
- categorized document types: `STATUTE`, `REGULATION`, `MINUTES`, `AGREEMENT`, `BUDGET`, `REPORT`
- explicit visibility levels: `PUBLIC`, `COMMUNITY`, `ADMINS`
- version history with current version pointer and change summaries
- permission scope `MANAGE_GOVERNANCE_LIBRARY` for publishing and versioning records
- membership-gated reads plus stricter role checks for admin-only documents

Expose this through:

- `GET /api/community/governance?communityId=...`
- `GET /api/community/governance/{documentId}`
- `POST /api/community/governance`
- `POST /api/community/governance/{documentId}/versions`

## Consequences

- governance-sensitive records now live under one auditable backend contract instead of fragmented frontend-only references
- communities can search rules and agreements by type, audience, and text before starting a new conflict or duplicate debate
- version history remains visible, which strengthens continuity for later decision-ledger work
- future public meeting and decision-ledger surfaces can link directly to the same governance documents without redefining document semantics

## Validation

- `mvn -q -Dtest=GovernanceLibraryIT test`
- `docker exec infra-civic-web-dev-1 sh -lc "cd /workspace && npm --workspace apps/web-react run build"`
- `npm run agent:context:check`
- `npm run backlog:current:check`
- `npm run agent:adr:check`
