# ADR-20260308: Community Moderation Contract

- Status: Accepted
- Date: 2026-03-08
- Story: `OCS-P1-049`

## Context

Open Civic Signal OS now supports community threads, structured proposal deliberation, decision ledgers, and trust metrics. Those participation surfaces increase civic value, but they also increase abuse, harassment, and misinformation risk. The previous moderation surface only covered flagged signals and did not provide one auditable workflow for community content, sanctions, and policy-backed enforcement.

## Decision

Introduce a backend-owned community moderation contract under `/api/community/moderation/*` with:

- report creation for supported community content targets:
  - `THREAD_MESSAGE`
  - `PROPOSAL_DELIBERATION`
- explicit reason codes:
  - `ABUSE`
  - `HARASSMENT`
  - `SPAM`
  - `MISINFORMATION`
  - `OFF_TOPIC`
  - `OTHER`
- one moderation queue response that includes:
  - open, actioned, and dismissed counts
  - active sanction count
  - report detail, preserved content preview, and action history
- one enforcement request model that supports:
  - `DISMISS`
  - `ENFORCE`
  - optional content hiding
  - optional sanction ladder action
- one sanction ladder for first release:
  - `WARN`
  - `LIMIT_POSTING_7_DAYS`
  - `SUSPEND_7_DAYS`
  - `SUSPEND_30_DAYS`
- one dedicated permission scope: `MANAGE_MODERATION_QUEUE`

Sanction checks are enforced through `CommunityAccessService`, not frontend role heuristics. Frontend only renders queue state, reporting forms, and enforcement actions returned by the backend contract.

## Consequences

Positive:

- communities gain one transparent moderation queue instead of fragmented hide-only controls
- reports, sanctions, and hidden-content actions remain traceable without deleting the underlying civic record
- community-scoped permission policies govern moderation access consistently with the rest of the operating system
- active sanctions can block posting actions deterministically and return an explicit machine-readable error code

Trade-offs:

- first release only targets thread messages and proposal deliberation entries; blog comments and other surfaces can join later on the same contract pattern
- sanction revocation and appeal workflows are deferred even though the schema keeps space for them
- the legacy signal-flag moderation path remains separate until it is intentionally migrated or retired

## Validation

- targeted backend integration coverage for report creation, enforcement, dismissal, and sanction-blocked posting
- OpenAPI updated in the same change set
- frontend moderation queue and reporting surfaces aligned to the contract
- GSD state, backlog, and changelog updated with the shipped story
