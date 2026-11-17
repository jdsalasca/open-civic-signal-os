# ADR-20261117: One page header and one card surface

Status: accepted
Date: 2026-11-17

## Context

The interface worked, but screens had drifted: the feed used a bespoke header while every other page
used `CivicPageHeader` at a much larger size; cards overrode the radius token with `rounded-3xl`;
the eyebrow rendered as a pill with a sparkle icon; and button padding made every action feel heavy.
Dead feed CSS (icon, aside, old page header) also remained.

## Decision

- **One header:** `CivicPageHeader` is the only page heading. It renders a plain eyebrow, a
  token-sized title and a muted description, with actions on the right. A `compact` variant gives
  the feed the same one-line heading as the rest of the app without the oversized title.
- **One card surface:** `CivicCard` no longer overrides the radius with `rounded-3xl`; every card
  uses `--radius-paper` (1rem). Paddings, borders and shadows stay on the shared token path.
- **Tighter controls:** PrimeReact buttons use `0.7rem 1rem` padding instead of `0.92rem 1.25rem`,
  empty states use a smaller title and less vertical padding, and the eyebrow is text, not a pill.
- **Personal section matching:** `/mine` is part of the navigation model for the topbar context
  ("Mi Actividad") even though the desktop topbar no longer lists it.
- Removed dead feed CSS for the old page header, side card and per-card icon.

## Consequences

- Every screen shares the same heading rhythm and card radius; the feed no longer looks like a
  separate design system.
- Buttons remain at least 2.75rem tall for touch while taking less horizontal space.
- Tests that assert heading level 1, page-header test ids and empty-state text remain valid.
- No backend, contract or schema change.

## Files

- `apps/web-react/src/components/ui/CivicPageHeader.tsx`
- `apps/web-react/src/components/ui/CivicCard.tsx`
- `apps/web-react/src/components/ui/CivicEmptyState.tsx`
- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/components/Layout.tsx`
- `apps/web-react/src/styles/main.scss`
