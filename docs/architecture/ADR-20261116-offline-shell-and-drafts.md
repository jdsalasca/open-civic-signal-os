# ADR-20261116: Offline shell and report drafts

Status: accepted
Date: 2026-11-16

## Context

Community leaders report from the street, where mobile connectivity drops mid-form. Reopening the
app used to mean a browser error page and a lost report. The app already ships a web manifest but no
service worker, so nothing survived a lost connection.

## Decision

- A production-only service worker (`public/sw.js`) caches the app shell and content-hashed static
  assets. Navigation is network-first with the cached shell as fallback, then `/offline.html`.
- `/api/**` is never intercepted or cached. A stale backlog, or another person's notification, must
  never be served from disk.
- Registration happens in `main.tsx` only when `import.meta.env.PROD`, because a cached shell in
  development would serve yesterday's code after every edit.
- Report drafts live in `localStorage`, scoped by username (`signalos:report-draft:<user>`), so a
  shared device does not show one neighbour's draft to another. Drafts are restored on mount,
  removed on successful submit or explicit discard, and never saved when the form is empty.
- The layout shows a non-blocking offline banner driven by `online`/`offline` events.

## Consequences

- No backend or contract change; no new dependency.
- The shell cache is refreshed by every online navigation, so a deploy does not require a cache
  version bump. `VERSION` is bumped only when the caching strategy itself changes.
- Offline mode is read-only: forms still submit only when the network returns. Queueing submissions
  would require conflict handling and is deliberately out of scope.
- Private browsing may reject `localStorage`; drafts are then silently unavailable and the form keeps
  working.
