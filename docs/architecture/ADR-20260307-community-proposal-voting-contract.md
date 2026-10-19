# ADR-20260307: Community Proposal Voting Contract

- Status: Accepted
- Date: 2026-03-07
- Story: `OCS-P1-042`

## Context

Open Civic Signal OS already supports structured proposals, typed deliberation, decision ledger entries, and trust metrics. The remaining legitimacy gap is a verifiable voting layer that communities can inspect before a proposal becomes a decision. That layer must stay backend-owned so eligibility, duplicate blocking, visibility rules, and audit semantics remain deterministic and explainable.

## Decision

Introduce a backend-owned community proposal voting contract under `/api/community/proposals/{proposalId}/vote` with:

- configurable vote mode per proposal:
  - `YES_NO`
  - `SCORE_1_5`
- configurable result visibility:
  - `COMMUNITY`
  - `AFTER_VOTE`
- configurable eligibility:
  - `ALL_MEMBERS`
  - `VERIFIED_MEMBERS`
- one-person-one-vote enforcement using a `(proposal_id, voter_id)` uniqueness guard
- anti-abuse audit counters for:
  - accepted votes
  - duplicate blocked attempts
  - eligibility blocked attempts
  - closed-window blocked attempts

Frontend adds proposal voting UX inside `/communities/proposals`, but vote eligibility, tally visibility, and abuse semantics stay backend-owned.

## Consequences

Positive:

- proposals can move into formal decisions with a visible and inspectable legitimacy step
- communities can understand why a user can or cannot vote before a decision is recorded
- anti-abuse events become auditable inputs instead of hidden moderation behavior
- vote-result visibility stays consistent across UI surfaces because it is contract-driven

Trade-offs:

- first release supports single-ballot voting only; vote updates/retractions are intentionally excluded
- verification depends on the existing user verification model rather than a stronger institutional identity check
- Docker-integrated browser evidence remains blocked on this workstation because Docker Engine is unavailable

## Validation

- targeted backend integration coverage for proposal vote read/cast flows and anti-abuse counters
- `mvn -q test` run in `apps/api-java`
- OpenAPI updated in the same change set
- GSD state, backlog, and changelog updated with the shipped story
