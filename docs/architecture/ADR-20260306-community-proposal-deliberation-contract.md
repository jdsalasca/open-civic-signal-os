# ADR-20260306 Community Proposal Deliberation Contract

- Status: accepted
- Date: 2026-03-06

## Context

`story:OCS-P1-040` created one structured proposal surface, but proposal discussion was still effectively flat. Communities could compare proposals, yet they still lacked a decision-supporting debate model that separates support, objections, questions, and evidence.

`story:OCS-P1-041` requires the API to own deliberation semantics so the frontend does not invent local argument types, local counters, or moderation rules that could drift from governance behavior.

## Decision

Introduce a dedicated `community_proposal_deliberation_entries` domain and expose backend-owned proposal deliberation endpoints:

- `GET /api/community/proposals/{proposalId}/deliberation`
- `POST /api/community/proposals/{proposalId}/deliberation`
- `PATCH /api/community/proposals/{proposalId}/deliberation/{entryId}/moderate`

Each deliberation entry is typed as one of:

- `PRO`
- `CON`
- `QUESTION`
- `EVIDENCE`

The backend owns these invariants:

- contribution counters are derived server-side
- `EVIDENCE` entries require one public supporting link
- moderation hides or restores one entry without deleting the proposal itself
- deliberation access stays community-scoped through existing membership and permission rules

For this story, contribution and moderation reuse existing community scopes:

- `ADD_THREAD_MESSAGE` for creating entries
- `MODERATE_THREAD_MESSAGE` for moderation

## Consequences

- Proposal debate becomes auditable and comparable instead of a flat thread of mixed intent.
- Evidence can be surfaced distinctly without frontend-only heuristics.
- Moderators can remove abusive or duplicate contributions while preserving proposal continuity and civic memory.
- Future voting and decision-ledger work can rely on stable, backend-owned deliberation structure and counts.
