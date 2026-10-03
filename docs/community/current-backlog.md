# Current Backlog (2026-03-19)

Operational execution source for immediate work in Open Civic Signal OS.

## Canonical Sources

- Primary story inventory: `docs/community/issue-backlog.md`
- Community operating-system expansion pack: `docs/community/community-operating-system-backlog.md`
- Frontend/backend top-20 priority pack: `docs/community/frontend-backend-top20-priority-backlog.md`

## Current State

Shipped Phase 5 foundations:

- `OCS-P1-029` civic identity profiles + privacy controls
- `OCS-P1-030` hierarchical communities + breadcrumbs
- `OCS-P1-031` community-scoped permission policies
- `OCS-P1-032` simple vs advanced experience modes + community-first visual system
- `OCS-P1-033` official announcements channel
- `OCS-P1-034` relevance-ranked community threads
- `OCS-P1-036` backend-owned community home
- `OCS-P1-037` structured issue reporting wizard
- `OCS-P1-038` signal timeline + assignment audit trail
- `OCS-P1-039` community map + cross-community heat view
- `OCS-P1-040` structured proposal templates
- `OCS-P1-041` proposal deliberation with typed pro/con/evidence
- `OCS-P1-043` community project boards
- `OCS-P1-046` governance library + searchable agreements
- `OCS-P1-047` decision ledger linked to proposals, governance, and execution
- `OCS-P1-048` public trust metrics dashboard
- `OCS-P1-042` verifiable community voting
- `OCS-P1-049` moderation queue and sanction policy engine
- `OCS-P1-050` privacy center and sensitive-data access logs
- `OCS-P1-051` civic onboarding, tours, and help center
- `OCS-P1-052` open civic data exports and scoped API
- `OCS-P1-035` real-time coordination rooms, mentions, and mute controls
- `OCS-P1-044` volunteering and activity slots
- `OCS-P1-045` shared resource booking and conflict rules
- `OCS-P1-053` outbound integrations layer
- `OCS-P0-005` full audit metadata from ingest to publish
- `OCS-P1-003` versioned and published formula metadata
- `OCS-P1-001` issue aging, trends, and SLA risk views

## Now (Next 7 Days)

Recommended next execution order:

1. platform hardening pack from `docs/community/frontend-backend-top20-priority-backlog.md`
2. `story:OCS-P1-004` transparency monthly report pipeline

Execution note:

- `story:OCS-P1-001` is shipped with a backend-owned `GET /api/signals/aging` returning SLA risk, age buckets, median age, and a created-versus-resolved trend read from the status audit trail, plus a staff dashboard panel carrying a freshness timestamp. The remaining hardening items are compose secret policy and OpenAPI parity.

## Next (7-21 Days)

1. Prepare the next enablement layer.
  - platform hardening pack from `docs/community/frontend-backend-top20-priority-backlog.md`
  - `story:OCS-P1-004`

## P0 Execution Queue (Agent Order)

This section is preserved for repository compatibility. The active P0-equivalent execution order is:

1. platform hardening pack from `docs/community/frontend-backend-top20-priority-backlog.md`
2. `story:OCS-P1-004`

## Wave Sequencing

### Wave 1: Trust Loop Completion

1. `story:OCS-P1-001`

### Wave 2: Safety and Data Responsibility

2. platform hardening pack from `docs/community/frontend-backend-top20-priority-backlog.md`

### Wave 3: Operational Growth

8. `story:OCS-P1-017`
9. `story:OCS-P1-021`
10. `story:OCS-P1-022`
11. platform hardening pack from `docs/community/frontend-backend-top20-priority-backlog.md`

### Wave 4: Cross-App Hardening

12. `story:OCS-P1-016`
13. `story:OCS-P1-018`
14. `story:OCS-P1-019`
15. platform hardening pack from `docs/community/frontend-backend-top20-priority-backlog.md`

## Story Notes

- `OCS-P1-047` shipped and now closes the proposal -> decision -> execution trust chain with explicit approval basis and execution ownership.
- `OCS-P1-048` shipped and now exposes backend-owned public proof of freshness, participation, execution progress, and case throughput for each community.
- `OCS-P1-042` shipped and now closes the proposal -> vote -> decision legitimacy gap with backend-owned eligibility rules, tally visibility, and anti-abuse counters.
- `OCS-P1-049` shipped and now adds auditable moderation reports, queue-based review, and proportional sanctions without moving civic policy logic into the frontend.
- `OCS-P1-050` shipped and now adds user-facing privacy controls, sensitive-data access logs, and backend-owned community publication policy management without moving trust rules into the frontend.
- `OCS-P1-051` shipped and now adds backend-owned onboarding state, searchable bilingual help guides, and contextual workflow help panels without moving role/audience logic into the frontend.
- `OCS-P1-052` shipped and now adds community-scoped CSV/JSON exports, scoped API tokens, backend rate limits, and auditable access logs without moving export policy into the frontend.
- `OCS-P1-035` shipped and now adds community-scoped coordination rooms, server-resolved mentions, per-user mute and mention-read state, and a server-sent room event stream without moving notification policy into the frontend.
- `OCS-P1-044` shipped and now adds capacity-enforced volunteer activities with published signup windows, releasable signups, and an organizer attendance trail without moving slot policy into the frontend.
- `OCS-P1-045` shipped and now adds policy-carrying shared resources, half-open conflict detection, a conflict-free approval queue, and requester-visible booking decisions without moving allocation rules into the frontend.
- `OCS-P1-053` shipped and now adds hashed-secret outbound channels with JDK-only HTTP delivery, per-attempt delivery records, visible failures, and operator retry, so a broken channel is never silent.
- `OCS-P0-005` shipped and now stamps every signal with its ingest channel, source reference, and scoring rule version, so a ranked number can be traced back to how it was produced.
- `OCS-P1-003` shipped and now publishes the scoring formula as versioned data from one service, with the user-facing ranking explanation rendered from it instead of duplicated copy.
- `OCS-P1-001` shipped and now derives SLA risk, age buckets, median age, and created-versus-resolved throughput server-side, with a staff dashboard panel that carries a freshness timestamp.

## Definition of Ready for Any New Story

- Story ID exists in `docs/community/issue-backlog.md`
- API, contract, and UI ownership are explicit
- Acceptance criteria are measurable
- Validation commands are specified
- Rollback path is documented

## Historical Note

Older phase-4 clarity/usability items and pre-Phase-5 audit backlogs remain documented in their dedicated source files, but they are not the primary execution queue anymore unless a trust-critical regression reopens them.
