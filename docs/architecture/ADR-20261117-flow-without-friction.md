# ADR-20261117: Flow without friction

Status: accepted
Date: 2026-11-17

## Context

The feed already felt instant for actions, but moving between screens still cost time: route chunks
loaded on first click, opening a case reset the feed scroll, the composer did not take focus, there
was no keyboard shortcut to publish or comment, and only the title of a card was clickable.

## Decision

- **Warm the next chunks:** 1.5 seconds after the app mounts, it imports the Reportar, case detail,
  Mi Actividad and Chat views in the background, so the first click does not wait for a network
  round trip.
- **Restore feed position:** the feed saves the shared scroll container's position on unmount and
  restores it when the feed is painted again, keyed by community. Opening a case and coming back
  keeps the reader where they were.
- **Composer focus and shortcuts:** opening the composer focuses the title; `Ctrl/Cmd+Enter`
  publishes; `Escape` closes. The comment box focuses on open and `Ctrl/Cmd+Enter` sends.
- **Tap the card:** clicking anywhere on a feed card outside a button, link or field opens the item.
  The title remains the keyboard-accessible link.

## Consequences

- Navigation to the most likely next screens is faster after the first seconds on the app.
- Returning from a case preserves context instead of forcing a new scroll.
- Writing and commenting work without reaching for the mouse.
- The card click handler ignores interactive descendants, so existing actions and the title link
  keep their behavior.
- Prefetch is idle-time work; a very slow connection may still load a chunk on demand.
- No backend, contract or schema change.

## Files

- `apps/web-react/src/App.tsx`
- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/tests/community-feed.spec.ts`
