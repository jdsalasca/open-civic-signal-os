# ADR-20261116: Monochrome chrome and top navigation

Status: accepted
Date: 2026-11-16

## Context

After the previous round, feedback from the production user was still negative: the navy/teal dark
theme felt heavy, the left sidebar pushed all content to the right while adding little, the home had
too much chrome (brand panel, community card, first-steps block) before the composer, and getting to
a nearby community took too many steps.

## Decision

- **Monochrome tokens:** light is white/grey with black action; dark is true black/grey with white
  action. Teal/blue no longer appears in navigation, buttons, links, focus rings or surfaces. Status
  and chart colours remain, because they carry meaning.
- **Top navigation:** the desktop sidebar is removed. A compact top bar holds the brand, the four
  primary destinations (Inicio, Chat, Reportar, Mi Actividad), **Más opciones**, the community
  selector, notifications, theme and a profile menu. The mobile drawer and bottom bar remain.
- **Less before the feed:** the first-steps checklist moves below the timeline, so the composer is
  the first thing under the title. The right column stays removed.
- **One-click nearby join:** the location notice now asks for the nearest community too. If it
  matches the detected city, the banner offers **Unirme a {comunidad}** and activates it without
  going through onboarding.
- The community selector is a native `<select>`: conventional, keyboard accessible and free of the
  clipped second line the styled dropdown produced.

## Consequences

- The content uses the full width; there is no left column to push it right.
- Black/white themes read as conventional light and dark modes; colour only signals status or data.
- The interface-mode badge lives in the profile menu, so a test that expected it permanently visible
  must open the menu (the curated CI suite does not assert it).
- Mobile keeps the existing drawer and bottom navigation.
- No backend, contract or schema change.

## Files

- `apps/web-react/src/styles/theme.scss`
- `apps/web-react/src/styles/main.scss`
- `apps/web-react/src/components/Layout.tsx`
- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/tests/community-feed.spec.ts`
