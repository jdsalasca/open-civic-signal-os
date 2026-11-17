# ADR-20261116: Atomic publish, resilient case timeline, and a lighter thread preview

Status: accepted
Date: 2026-11-16

## Context

Three loose ends remained from the Terra audit:

- Publishing from the feed created the thread and then posted its body in a second request. If the
  second call failed, the thread existed with no body, and retrying could duplicate it.
- `SignalDetail` loaded the case, its history and the formula with `Promise.all`; a failed history
  request rejected the whole page, so the reader was sent away from a case they could still read.
- The feed loaded **every message of every thread** on the page to show the first one as a summary.

## Decision

- `CreateCommunityThreadRequest` accepts an optional `initialMessage` (max 2000). The service creates
  the thread and its first message in one `@Transactional` call, requiring `ADD_THREAD_MESSAGE` when a
  body is present, and returns the thread with its messages. The feed composer sends one request.
- `SignalDetail` uses `Promise.allSettled`. A failed case request still leaves the page (there is no
  case to show); a failed history shows an inline **Retry** inside the timeline card and keeps the
  case, score and actions on screen. The formula keeps its optional behaviour.
- `CommunityThreadMessageRepository.findEarliestByThreadIds` returns only the earliest message per
  thread, and the feed summary uses it instead of loading the whole thread history.

## Consequences

- A publish either produces a thread with its body or nothing.
- Roles that can create a thread but not post messages now get a clear denial instead of an empty
  thread.
- A history outage is visible and recoverable without losing the case.
- The feed reads one row per thread for summaries instead of the full history; a thread with tied
  timestamps can return more than one candidate, and the collector keeps the first.
- No schema or dependency change.

## Files

- `apps/api-java/.../web/dto/CreateCommunityThreadRequest.java`
- `apps/api-java/.../service/CommunityCollaborationService.java`
- `apps/api-java/.../repository/CommunityThreadMessageRepository.java`
- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/views/SignalDetail.tsx`
- `packages/contracts/openapi.yaml`
