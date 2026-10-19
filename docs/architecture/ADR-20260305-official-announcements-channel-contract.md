# ADR-20260305 Official Announcements Channel Contract

- Status: accepted
- Date: 2026-03-05

## Context

Community blog updates were previously mixed into a generic timeline without strong trust cues, pinning, or archive behavior. Phase 5 requires a dedicated official communications channel that communities can trust and search without confusing it with discussion surfaces.

## Decision

We expanded the official-announcements contract around community blog posts.

- Treat community blog posts as official announcements with explicit `official` metadata.
- Add `pinned` to keep a small set of current announcements above the chronological stream.
- Add archive metadata with `archivedAt` and `archivedBy`.
- Add archive retrieval endpoint with text and date filters:
  - `GET /api/community/blog/archive`
- Add archive mutation endpoint:
  - `PATCH /api/community/blog/{postId}/archive`
- Allow pinning on create/update through backend-owned request fields.

## Consequences

Positive:

- Institutional updates are visually and contractually distinct from discussion.
- Communities can keep urgent official notices visible without destroying chronology.
- Historical announcements remain searchable without cluttering the live channel.

Tradeoffs:

- Blog post contract is now more specialized and tied to official-communications behavior.
- Archive filters become part of the versioned API surface.

## Notes

- Only roles allowed by `CREATE_OFFICIAL_UPDATE` and `UPDATE_OFFICIAL_UPDATE` may publish or archive announcements.
- Discussion ordering remains a separate concern and stays outside this contract.
