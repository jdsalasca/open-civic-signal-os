# ADR-20261116: Thread authorship, honest follow state, and translated network errors

Status: accepted
Date: 2026-11-16

## Context

The Terra audit found three trust gaps around a single case or conversation:

- The feed read a thread's replies by searching the first page of the community thread list, so a
  thread outside that page looked empty, and every reply was labelled "Someone".
- `SignalTrustActions` started with `following = false`; when the follow lookup failed, the screen
  claimed the case was not followed instead of saying it did not know.
- The axios interceptor wrote fixed English messages ("Civic API Unreachable", "Clearance Denied")
  and views prefer `friendlyMessage` over their own translated keys.

## Decision

- Add `GET /api/community/threads/{threadId}?communityId=...`. It requires membership in the
  community and returns 404 when the thread does not belong to it. The response is the existing
  `CommunityThreadResponse`, now with `authorName` on every message.
- The feed reads the thread by id and lists replies (messages with a parent) instead of the first
  page of threads. The root message remains the body already shown on the card.
- `CommunityThreads` shows the author name, falling back to the previous short member label when an
  older message has no author.
- The follow button has three states: loading (disabled), known (`aria-pressed` reflects the
  server), and error (a **Retry follow** action that re-reads the state instead of toggling it).
- The axios interceptor resolves its messages through the i18n singleton and a new `errors` block
  (network, auth, permission, not found, conflict, server). Backend-provided messages still win.

## Consequences

- One extra request when a thread's comments are opened; the list endpoint is no longer involved.
- Older messages created before `authorName` are still renderable: the UI falls back.
- A failed follow lookup never turns into an accidental unfollow.
- Error toasts follow the active language; technical backend messages remain for domain errors.
- No schema, migration or dependency change.

## Files

- `apps/api-java/.../web/dto/CommunityThreadMessageResponse.java`
- `apps/api-java/.../service/CommunityCollaborationService.java`
- `apps/api-java/.../web/CommunityCollaborationController.java`
- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/views/CommunityThreads.tsx`
- `apps/web-react/src/components/SignalTrustActions.tsx`
- `apps/web-react/src/api/axios.ts`
- `packages/contracts/openapi.yaml`
