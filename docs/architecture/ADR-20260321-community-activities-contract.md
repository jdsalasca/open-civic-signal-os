# ADR-20260321: Volunteer Activities, Capacity, And Signup Window Contract

- Status: Accepted
- Date: 2026-03-21
- Story: `OCS-P1-044`

## Context

Many community actions need people and a time slot, not just comments. Threads cannot express "twelve volunteers, Saturday 7am, signups close Friday noon". Without a first-class activity model, communities either overbook real-world actions or move capacity negotiation into private chats, which removes any public record of who committed to what.

Before this story there was no activity entity, no capacity model, no signup window, and no attendance record. Any of those rules implemented in the frontend would be unenforceable and unauditable.

## Decision

Introduce a backend-owned volunteer activity contract with:

- one activity per community, carrying `startsAt`/`endsAt`, a published `signupOpensAt`/`signupClosesAt` window, and a `signupCapacity`
- one signup record per `(activity, volunteer)` with `CONFIRMED`/`CANCELLED` status, so releasing a spot frees capacity instead of deleting the record
- one attendance record per signup: `PENDING`, `ATTENDED`, or `NO_SHOW`, timestamped
- two permission scopes:
  - `JOIN_ACTIVITIES`
  - `MANAGE_ACTIVITIES`
- one derived, backend-owned `signupWindowState` (`UPCOMING`, `OPEN`, `FULL`, `CLOSED`, `CANCELLED`) plus `fillRatePercent`, so the client never has to re-derive capacity

Rule enforcement lives in `CommunityActivityService` and surfaces through the existing `ConflictException` (HTTP 409), which is already the repository's contract for state conflicts:

- a signup is rejected when the window has not opened, has closed, or capacity is reached
- a duplicate confirmed signup for the same volunteer is rejected
- releasing a spot is only allowed before `signupClosesAt`; after that the volunteer must contact the organizer, which keeps the closed window honest
- schedule validation rejects zero capacity, an end before a start, an inverted window, or a window that closes after the activity starts

The frontend renders the board, signup/release actions, fill rate, roster, and attendance controls, but capacity, window state, and attendance writes remain backend-owned.

## Consequences

Positive:

- communities can commit people to real-world actions with a public capacity and deadline
- releasing a spot frees capacity without erasing who was involved
- attendance turns participation into evidence that can be reconciled with the decision ledger later
- rule violations return explicit reasons the UI can translate, instead of a generic failure

Trade-offs:

- slots are per activity, not per time slice. Recurring shifts inside one activity would need a separate slot entity.
- attendance is recorded by the organizer role and is not cross-checked against a signature or geolocation
- there is no waitlist. When an activity is full the only path is to retry later, or ask the organizer to raise capacity

## Validation

- `CommunityActivityIT` covers organizer creation authorization, member join within the window, capacity enforcement, duplicate signup rejection, closed-window rejection on join, release allowed in-window and blocked out-of-window, attendance authorization split between organizer and member, and schedule/capacity validation
- OpenAPI updated in the same change set with activity, roster, signup result, and attendance schemas
- `community-activities.spec.ts` covers window state rendering, fill rate, capacity transition to full, attendance recording, and the organizer creation form
- `global-state-integrity.spec.ts` extended to cover `/communities/activities`