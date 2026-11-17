# ADR-20261117: Treat API LocalDateTime as UTC

Status: accepted
Date: 2026-11-17

## Context

A post created five hours earlier rendered as **"in five hours"** for readers in UTC-5. The API
serializes Java `LocalDateTime`, a wall-clock string with no offset. The API container runs in UTC,
but the browser parsed the string as browser-local time, moving every timestamp five hours into the
future in Bogotá. The same shift affected the case timeline, comments and notifications.

## Decision

- Add `parseApiDate`: date-time strings without an offset are parsed as UTC; date-only strings stay
  local so a calendar day does not move to the previous day.
- `formatRelativeTime`, `formatStamp`, the notification inbox and the metrics trend use it.
- Values that already carry `Z` or an offset are respected, so optimistic local timestamps and any
  future offset-aware API keep working.

## Consequences

- Past posts read "5 hours ago" instead of "in 5 hours"; timeline and notification times match the
  reader's clock.
- The change is frontend-only. A future backend change to offset-aware timestamps remains
  compatible.
- Local development against a non-UTC API would display shifted times; the Docker baseline runs UTC,
  matching production.

## Files

- `apps/web-react/src/utils/parseApiDate.ts`
- `apps/web-react/src/utils/formatRelativeTime.ts`
- `apps/web-react/src/utils/formatStamp.ts`
- `apps/web-react/src/components/ui/NotificationBell.tsx`
- `apps/web-react/src/components/MetricsGrid.tsx`
- `apps/web-react/src/tests/community-feed.spec.ts`
