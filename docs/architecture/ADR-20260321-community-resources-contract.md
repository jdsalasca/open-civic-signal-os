# ADR-20260321: Shared Resource Booking And Conflict Contract

- Status: Accepted
- Date: 2026-03-21
- Story: `OCS-P1-045`

## Context

Community halls, meeting rooms, projectors, and vehicles are scarce shared assets. Today they are allocated by whoever asks first in a private message, which produces double bookings, opaque priority, and no record of who decided what.

Before this story there was no resource entity, no booking window, no conflict rule, and no approval trail. Conflict detection implemented in the browser would be advisory only: two people booking the same slot at the same time would both see "available" and both succeed.

## Decision

Introduce a backend-owned booking contract with:

- one resource per community carrying its published policy:
  - `requiresApproval`
  - `minNoticeHours`
  - `maxBookingHours`
- one booking record with status `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, or `CANCELLED`
- two permission scopes:
  - `BOOK_RESOURCES`
  - `MANAGE_RESOURCES`

Conflict detection is a half-open overlap test in the database, not in memory:

```
b.startsAt < :endsAt AND b.endsAt > :startsAt
```

Only `PENDING_APPROVAL` and `APPROVED` bookings block a window. Half-open semantics mean back-to-back bookings (one ends exactly when the next starts) are allowed, while any real overlap is rejected with HTTP 409 and a reason the UI can translate.

The decision to make `PENDING_APPROVAL` blocking is deliberate. If pending requests did not block, the approver queue would grow into a list of mutually exclusive requests and the coordinator would be arbitrating conflicts by hand. Blocking pending requests makes the queue conflict-free by construction, which is why approving a pending booking cannot introduce a new overlap.

Cancellation sets status `CANCELLED` rather than deleting the row, which frees the window while preserving who requested and released it.

The backend derives `upcomingBlockingCount` and the caller's own booking status so the client never recomputes availability.

## Consequences

Positive:

- double bookings are prevented at request time, not merely warned about
- the approval queue is conflict-free, so approving never creates a clash
- cancelling frees the window without erasing the request history
- the published policy (notice, duration, approval) is visible next to the booking form, so the rules are explainable before committing

Trade-offs:

- conflicts are rejected rather than escalated to a coordinator. The message tells the requester to ask the organizer, but there is no automated waitlist or priority ordering.
- overlap detection is exact-timestamp based. A community that wants recurring slots or resource-preparation buffers needs a separate slot entity.
- `minNoticeHours` and `maxBookingHours` are per resource, not per community. A community-wide policy would need a separate settings record.

## Validation

- `CommunityResourceIT` covers auto-approval vs pending queue, half-open overlap semantics (overlapping rejected, back-to-back allowed), approval queue visibility split between member and manager, requester-visible decision note, notice and duration rules, cancellation freeing the window, archived resources rejecting bookings, and member-create denial
- OpenAPI updated in the same change set with resource, booking, board, and decision schemas including the 400/403/409 branches
- `community-resources.spec.ts` covers policy rendering, upcoming availability, approval queue with a decision note, and the booking and create forms
- `global-state-integrity.spec.ts` extended to cover `/communities/resources`