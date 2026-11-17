# ADR-20261117: Fewer controls, one next step

Status: accepted
Date: 2026-11-17

## Context

The user asked for a design for distracted people: fewer buttons, cleaner space, clear next actions.
After the monochrome top bar, the app still showed four primary links, a theme wheel, a four-chip
filter row, five actions per card, and a three-button checklist before the rest of the page.

## Decision

- **Top bar:** three destinations (Inicio, Chat, Reportar) plus **Más**. **Mi Actividad**, theme
  (light/dark), text size (A / A+), preferences and sign-out live in the profile menu. The separate
  theme wheel is gone.
- **Simple mode menu:** collaboration pages (official blog, talks, activities) only appear in
  advanced mode or for staff. Simple mode's **Más** keeps Comunidades, Ayuda and Preferencias.
- **Feed filter:** one native **Mostrar** select replaces four filter chips. Alerts are a single
  bell icon with `aria-pressed` and a label/title.
- **Card actions:** **Me gusta** and **Comentar** keep their labels and counts; confirm and follow
  become icon-only controls with accessible names and counts. **Celebrar** is removed from the card
  (the API keeps the reaction type). The decorative per-card icon is gone.
- **First steps:** the three-button checklist becomes one line with progress and only the next
  pending action. Completed steps disappear; all-complete still hides it.

## Consequences

- The home presents one writing surface, one filter, one alert control and two primary card actions.
- Everything removed from the main path remains reachable: `/mine` and theme/text size in the
  profile menu, collaboration pages in advanced mode or by URL.
- Tests were updated for the filter select, `aria-pressed` alerts, icon-only follow/confirm and the
  compact checklist.
- No backend, contract or schema change.

## Files

- `apps/web-react/src/components/Layout.tsx`
- `apps/web-react/src/components/OnboardingChecklist.tsx`
- `apps/web-react/src/views/CommunityFeed.tsx`
- `apps/web-react/src/styles/main.scss`
- `apps/web-react/src/tests/community-feed.spec.ts`
- `apps/web-react/src/tests/onboarding-checklist.spec.ts`
- `apps/web-react/src/tests/accessibility-text-scale.spec.ts`
