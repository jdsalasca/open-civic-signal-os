# ADR-20261116: Location-first onboarding and a compact community home

Status: accepted
Date: 2026-11-16

## Context

A new account could land on an empty feed with "no community selected". The location flow existed
at `/onboarding`, but nothing took the person there, and the feed still carried a large page header,
two side cards and no visible sense of what was rising in the community.

## Decision

- A signed-in account with no memberships is routed to `/onboarding`. The root waits for the first
  `communities/my` load before deciding, so an existing member is never sent there by an empty store.
- The onboarding already auto-requests geolocation once per tab. It now shows the detected chain
  **barrio → localidad → municipio → departamento → país**, keeps the closest city join, offers the
  public communities sorted by that chain, and marks matches with **Cerca de ti**.
- Finishing onboarding refreshes memberships before returning to the feed, so the root does not
  bounce back.
- The feed page becomes compact: one title and one report action, then composer, filters and cards.
- The community and alerts side cards are merged; a **Tendencias** card shows the three most-reacted
  posts from the current feed. Reactions already come from the feed DTO.
- The composer carries a short good-use note and a **Normas de la comunidad** link.

## Consequences

- New accounts see location before an empty feed; returning members are unaffected.
- No backend or contract change. The detected hierarchy uses the existing reverse-geocode and
  nearest-city endpoints; nearby ordering uses the public community fields.
- Trends are derived from the feed the person already loaded; no extra request.
- The feed is lighter above the fold but keeps the full card content and 44px actions.
- Anonymous public reading still uses the public backlog link; the social feed remains member-only.
