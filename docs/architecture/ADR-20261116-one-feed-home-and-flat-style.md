# ADR-20261116: One community feed at home and a flatter civic style

Status: accepted
Date: 2026-11-16

## Context

The citizen had two homes: a report dashboard at `/` and a community feed at `/communities/feed`.
The first was a ranked table, the second a composer with conversations, reports and announcements.
A citizen landing after sign-in could read reports but could not tell where to opine, and the
navigation exposed fifteen tools before the first post. The visual language also stacked serif
display type, cut-paper radii, washi tape, glass, gradients and repeated shadows on the same screen.

## Decision

- `/` is the **community feed** for every signed-in person. It is the one place to read reports,
  read announcements, opine in conversations and start a report.
- The analytics dashboard moves to `/dashboard` and is labelled **Overview / Resumen**. It is shown
  in the progressive menu for staff and advanced mode, not in the citizen's core path.
- The core navigation is **Home, Report, My activity**. Everything else lives in **More options**.
  Citizens in simple mode see the community essentials there; staff and advanced mode see the
  analytical and administrative tools.
- The feed has an explicit **Report a problem** action in the page header and an opinion composer at
  the top with the copy “Share an opinion or ask a question in …”.
- A failed feed load shows a persistent **Retry** card, never an empty community.
- The shared style moves to a **flat civic service** language: Nunito Sans for headings and body,
  one consistent radius scale, a single soft elevation, no washi tape, no rotated brand sheet, no
  gradient card headers, and quiet text actions inside feed cards.
- `CivicPageHeader` renders the eyebrow only when a page passes one; the default “community
  workspace” pill is gone.

## Consequences

- Dashboard specs and helpers now navigate to `/dashboard`; the citizen feed is tested at `/`.
- The public landing is unchanged for logged-out visitors.
- Staff keep the analytics, aging and notification panels on `/dashboard`.
- The feed remains the source of truth for opinion; the report detail is still the place to follow a
  case end to end. A public read-only case detail remains a later flow improvement.
- The style tokens are simpler and cheaper to keep consistent across views.
