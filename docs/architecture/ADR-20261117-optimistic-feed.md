# ADR-20261117: Optimistic feed actions and instant revisit

Status: accepted
Date: 2026-11-17

## Context

The interface is now simpler, but every interaction still waited for a round trip: liking, following
or confirming changed only after the server answered, publishing waited for a full feed refetch, and
returning to the feed always painted skeletons first. The user asked for the app to feel fast.

## Decision

- **Optimistic actions:** like, follow and confirm update the card immediately, keep a per-action
  pending guard, reconcile with the server response, and roll back only the touched fields on error.
- **Optimistic publish:** the create response already contains the thread and its first message, so
  the composer prepends the new card, closes, and toast immediately. The feed refetches in the
  background to reconcile.
- **Instant revisit:** a module-level, in-memory cache keyed by `user + community` paints the last
  feed immediately while the fresh page loads. It is never written to storage and never shared
  across accounts.
- **Cheaper paint:** the top bar and secondary buttons drop `backdrop-filter`; the bar uses a solid
  surface. Decorative blur remains only where it is small and meaningful.

## Consequences

- Taps feel instant; a failed request restores the previous state and shows the existing error toast.
- Publishing no longer waits for a feed read; a background reconcile keeps server truth.
- A revisit shows the previous page for a moment, then refreshes.
- Memory grows with one feed page per community visited in the session; bounded by the page size.
- No backend, contract or schema change.

## Files

- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/styles/main.scss`
- `apps/web-react/src/tests/community-feed.spec.ts`
