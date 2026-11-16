# ADR-20261116: Civic daylight palette and a feed-first simple home

Status: accepted
Date: 2026-11-16

## Context

The Paper Craft palette was built on warm cream and terracotta, and the dark theme read as brown.
Field feedback was blunt: the first screen looked drab, the hero stacked four pill-shaped controls,
and a citizen could not tell where to report or what to read first. The product is a community feed,
not an analytics console, for most of its users.

## Decision

- Replace the warm Paper Craft palette with **Civic daylight**: cool light neutrals, deep ink,
  teal action, blue community data, amber attention. The cut-paper geometry, type and motion stay.
- Light becomes the default theme. The dark theme is retuned to cool navy + mint so existing users
  who chose dark do not have to switch to get the new look.
- Keep the contrast gate (`npm run check:contrast`) as the source of truth; all declared pairs must
  still pass WCAG AA.
- Simple mode home:
  - identity, place and freshness become one quiet text line instead of four pills;
  - one primary **Report a problem** action stays in the hero;
  - the report feed moves to the first screen;
  - the analytics rail (chart, metrics, digest) is hidden for non-staff citizens;
  - the "what needs attention" cards move below the feed;
  - the duplicate topbar report button is hidden in simple mode (the sidebar, mobile nav, hero and
    feed header already expose the action).
- Simple mode filters become one swipeable row on phones instead of three stacked rows.
- The table row action becomes a text link, and technical headers (`Intelligence Context`, `Index`,
  `SIG-ID`) become plain language (`Report`, `Priority`, no internal id).
- The feed header's primary action becomes **Report a problem**; the composer stays one click away.

## Consequences

- No backend, contract or dependency change.
- Existing users keep their stored theme; the dark theme is retuned in place.
- The public landing illustration keeps its warm paper-cut buildings as an illustration; only the
  interface palette changed.
- Advanced mode and staff still get the chart, metrics, digest and aging panels.
