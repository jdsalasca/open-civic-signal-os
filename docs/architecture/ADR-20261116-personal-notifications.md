# ADR-20261116: Personal notifications

Status: accepted
Date: 2026-11-16

## Context

The existing `notifications` table is a staff relay log: it has no recipient user, no read state
and no deep link, and `/api/notifications/recent` is restricted to staff. Members who report,
open a conversation or support a signal receive no signal that anything happened. That breaks the
return loop of a community product.

## Decision

Add a separate, user-scoped inbox:

- `user_notifications` stores the recipient, actor, a **type** (`THREAD_REPLY`, `SIGNAL_SUPPORT`,
  `SIGNAL_STATUS`), the target (`SIGNAL`/`THREAD` + id), a target title, community, timestamp and
  read state.
- Only structured facts are stored; the client renders the sentence in the active language.
- The first integrations are: a reply to a thread notifies its creator; a support vote notifies
  the report author; a status change notifies the report author. Self-notifications are skipped.
- Endpoints (`/inbox`, `/unread-count`, `/{id}/read`, `/read-all`) are authenticated and scoped to
  the caller. They do not expose another user's notifications.
- The old staff relay table and endpoints remain unchanged.

## Consequences

- New migration V56 and indexes on `(user_id, created_at)` and `(user_id, read_at)`.
- The shell polls the unread count every 60 seconds; the inbox loads on demand.
- Each integration point must be transactional with its source action; a notification failure
  rolls back the action rather than leaving a silent inconsistency.
- Follow/subscription controls and proposal/decision notifications are a later extension.
