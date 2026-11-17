# ADR-20261116: Cursor pagination for the community feed and room history

Status: accepted
Date: 2026-11-16

## Context

The Terra audit found two dead ends:

- The feed asked for 30 days and capped the merged list at 100, with no way to reach older content
  and no statement of the window.
- The room history stopped at a hard 200-message ceiling: the frontend hid the "load older" button
  even when the API reported more messages, because it raised a `limit` instead of paging.

Both screens also loaded more than they needed: the blog and thread feed paths filtered dates and
limits in memory after fetching, and a room page loaded the newest rows to decide whether to show
a button.

## Decision

- `GET /api/community/feed` accepts `before` (ISO local date-time) and `size` (capped at
  50). Each type asks the database for its newest page before the cursor; the merged result is
  sorted and limited once. The response stays a plain array so existing clients do not change.
- The feed shows **Load older posts** while a full page comes back, and always states that it covers
  the last 30 days. A short page hides the control.
- `GET /api/community/rooms/{roomId}` accepts `before`. The room page fetches `limit + 1` rows to
  report `hasMoreMessages` for that page, and the frontend prepends older pages with client-side
  de-duplication.
- Cursors use the timestamp the API returned, not a client-made one, so precision matches the
  column.

## Consequences

- Bounded reads: the blog/thread feed no longer loads a whole timeline to filter it in memory, and
  a room page reads one row more than it shows.
- The feed window is explicit; older than 30 days is still out of scope and would need a date
  selector or an archive endpoint.
- Strictly older cursors can skip items that share the exact same timestamp at a page boundary.
  Timestamps carry microsecond precision in the database, and the tests assert the boundary is not
  duplicated; a composite cursor is the upgrade path if same-instant bursts ever matter.
- No schema or dependency change.

## Files

- `apps/api-java/.../repository/SignalRepository.java`
- `apps/api-java/.../repository/CommunityBlogPostRepository.java`
- `apps/api-java/.../repository/CommunityThreadRepository.java`
- `apps/api-java/.../repository/CommunityRoomMessageRepository.java`
- `apps/api-java/.../service/CommunityCollaborationService.java`
- `apps/api-java/.../service/CommunityRoomService.java`
- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/views/CommunityRooms.tsx`
- `packages/contracts/openapi.yaml`
