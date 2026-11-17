# ADR-20261117: Polish after the redesign audit

Status: accepted
Date: 2026-11-17

## Context

A read-only Terra review of the redesigned app found ten issues. The highest-value ones affected
central actions: duplicate sends, a filter that looked empty while more pages existed, fragment
links that did not reach their card, menus that ignored Escape/outside clicks, a formula failure with
no feedback, onboarding showing "no results" while loading, and no community switcher in the mobile
drawer.

## Decision

- **No duplicate sends:** `publish` and `sendReply` use refs as re-entry guards in addition to the
  disabled buttons, so repeated `Ctrl/Cmd+Enter` cannot create two threads or comments.
- **Filter is not a dead end:** the **Load older** control and the 30-day note render outside the
  list/empty branches, so a filtered empty page can still fetch older items.
- **Fragment links land:** `useHashTarget` resolves `#blog-post-…` / `#thread-…` after the target
  page renders its list, scrolls to it and focuses it.
- **Menus behave:** the **Más** and profile menus close on outside click and Escape; theme, text size
  and language expose `aria-pressed`.
- **Formula failure is visible:** the "why this is ranked" card shows an inline alert with retry,
  like the timeline, while the case remains usable.
- **Onboarding loading:** the community list announces loading with `role="status"` and only shows
  "no results" after the request completes.
- **Mobile switching:** the drawer includes a labelled community selector; the topbar keeps its
  compact selector on wider screens.

## Consequences

- Repeated shortcuts are safe; a slow request cannot be double-submitted from the composer.
- A filter with no matches keeps a path to older content instead of declaring the community empty.
- Clicking a card on the feed lands on the chosen post/thread when it is on the loaded page.
- Menus no longer trap the reader; keyboard users can dismiss them.
- Remaining from the audit: the scroll cache still keeps only the first page, and `CivicCard` titles
  remain `h3`; both are follow-ups rather than blockers.
- No backend, contract or schema change.

## Files

- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/views/CommunityBlog.tsx`
- `apps/web-react/src/views/CommunityThreads.tsx`
- `apps/web-react/src/views/SignalDetail.tsx`
- `apps/web-react/src/views/Onboarding.tsx`
- `apps/web-react/src/components/Layout.tsx`
- `apps/web-react/src/utils/useHashTarget.ts`
- `apps/web-react/src/styles/main.scss`
