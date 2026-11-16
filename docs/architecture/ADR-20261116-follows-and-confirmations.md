# ADR-20261116: Following content and neighbour confirmations

Status: accepted
Date: 2026-11-16

## Context

The personal notification inbox exists, but it only notifies authors. A member who wants to follow
a report or a conversation has no way to say so, and a report has no lightweight way to show that
more than one neighbour saw the same problem.

## Decision

- Add `content_follows` with one row per person and target (`SIGNAL`, `THREAD`, `BLOG`). The feed
  returns `following` in one batch, and actions on followed content notify every follower.
- Add `signal_confirmations` with one row per person and report. It is **evidence**, not a scoring
  input: the prioritization formula is unchanged until a separate formula decision is approved.
- Followers and confirmations are scoped to the authenticated user; self-notifications are skipped.
- The report screen and feed expose both actions inline.

## Consequences

- Migration V57 and V58, plus indexes on `(target_type, target_id)` and `signal_id`.
- Notification volume grows with followers; the inbox is capped at the latest 50 and the UI at 8.
- Confirmation counts are shown, but the ranking remains deterministic and explainable.
- Proposal follows can reuse the same table later with a new target type.
