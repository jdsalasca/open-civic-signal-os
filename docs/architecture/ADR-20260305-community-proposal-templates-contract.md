# ADR-20260305 Community Proposal Templates Contract

- Status: accepted
- Date: 2026-03-05

## Context

Community proposals were still arriving as ad-hoc text. That made comparison, prioritization, and later decision tracking unreliable because every proposal used a different structure and often lacked a link to an existing issue or supporting evidence.

`story:OCS-P1-040` requires one backend-owned proposal contract with deterministic required sections so the frontend only renders the workflow and does not invent proposal semantics.

## Decision

Introduce a dedicated `community_proposals` domain with one initial template key, `STANDARD_COMMUNITY_PROPOSAL`, and expose backend-owned create/list/detail/update endpoints:

- `GET /api/community/proposals`
- `GET /api/community/proposals/{proposalId}`
- `POST /api/community/proposals`
- `PUT /api/community/proposals/{proposalId}`

Each proposal now requires:

- `title`
- `problemStatement`
- `proposedSolution`
- `estimatedCost`
- `beneficiariesSummary`
- `supportingLinks[]`

Optional linkage to an existing issue is represented with `relatedSignalId`, validated server-side so the linked issue must belong to the same community.

Proposal creation is governed by the community permission scope `CREATE_PROPOSAL`.

## Consequences

- Communities can compare proposals using one shared structure instead of free-form text dumps.
- Proposal trust remains backend-owned because validation, issue linkage, and permission enforcement live in the API.
- The frontend can present a guided proposal workflow and a comparable detail view without implementing proposal policy logic.
- Future deliberation, voting, and decision-ledger work can build on a stable proposal identifier and shape.
