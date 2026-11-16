# ADR-20261116: Feed authorship, resilient chat, and honest failure states

Status: accepted
Date: 2026-11-16

## Context

The Terra audit of the citizen flows found that a feed card did not say who wrote it or how many
comments it had, the community chat stopped receiving messages after the server closed the stream
(30 minutes by design) or the network dropped, "Mis reportes" silently showed only the first page,
and several screens presented a network failure as an empty state.

## Decision

- `CommunityFeedItem` (API and contract) gains `authorName`, `authorRole` and `commentCount`.
  Signals and official posts read the author in one batched query and count comments in one grouped
  query. Thread entries keep `commentCount` null because their replies are thread messages.
- The card shows **Por {nombre}**, an **Oficial** badge for institutional authors, and the comment
  count on the comment action. Publishing a comment increments the count locally.
- The chat stream reopens with exponential backoff (2s → 30s), stops on 401/403, and refreshes the
  room and workspace after a reconnect to catch messages sent while it was down.
- When the workspace changes community, the selected room is kept only if it belongs to the new
  workspace; otherwise the first valid room is selected.
- "Mis reportes" reads every page (100 per request, bounded at 20 pages) so the table and its
  metrics cover the full history, and a failed load shows a retry state instead of "no reports".
- The onboarding community list shows a retry state on failure and the search field has a visible,
  associated label.

## Consequences

- No new endpoint. The feed response grows by three fields; clients that ignore them are unaffected.
- "Mis reportes" makes at most 20 requests for a very large history. If a citizen exceeds 2,000
  reports, a server-side summary endpoint becomes the next step.
- The chat reconnect is bounded by network and auth; a forbidden or missing session does not loop.
- Failures are now distinguishable from empty data in the feed, my reports and onboarding.

## Files

- `apps/api-java/.../web/dto/CommunityFeedItemResponse.java`
- `apps/api-java/.../service/CommunityCollaborationService.java`
- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/views/CommunityRooms.tsx`
- `apps/web-react/src/views/MySignals.tsx`
- `apps/web-react/src/views/Onboarding.tsx`
- `packages/contracts/openapi.yaml`
