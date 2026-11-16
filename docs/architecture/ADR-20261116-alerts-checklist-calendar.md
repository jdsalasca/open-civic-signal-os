# ADR-20261116: Neighbourhood alerts, first-action checklist and activity calendar

Status: accepted
Date: 2026-11-16

## Context

Following content is now possible, but a member still has no way to say "tell me when my
neighbourhood publishes a new report or event". New members also land on a dashboard without a
concrete first action, and activities have no calendar export or reminder.

## Decision

- Add `community_alert_subscriptions` (one row per person and community). Subscribers receive
  in-app notifications on new reports and new activities; the author/organizer is skipped.
- Add `GET /api/onboarding/checklist` with three booleans derived from existing rows (membership,
  report/support, follow). No new state table; the checklist is a read model.
- Add `GET /api/community/activities/{id}/calendar` producing a minimal RFC 5545 file, and a
  scheduled reminder roughly 24 hours before an activity with a `reminder_sent_at` marker.
- All additions are additive; the ranking formula and existing tables are untouched.

## Consequences

- Migration V59 and V60.
- The hourly reminder query is bounded to a two-hour window and marks the activity after the run,
  so repeated scheduler ticks are idempotent.
- The checklist disappears once complete, keeping the home calm for returning members.
- Alerts are opt-in and in-app only; email delivery remains a later integration.
