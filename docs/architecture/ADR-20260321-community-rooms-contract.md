# ADR-20260321: Community Coordination Rooms, Mentions, And Mute Contract

- Status: Accepted
- Date: 2026-03-21
- Story: `OCS-P1-035`

## Context

Communities coordinate active issues and events today through asynchronous threads only. Working groups need lightweight synchronous coordination: a shared room per community or project group, the ability to notify a specific neighbour with `@username`, and the ability to mute a noisy room without losing the record of what happened.

Before this story there was no room model, no mention resolution, and no user-specific notification state. Any mention or mute behaviour living in the frontend would be unauditable and impossible to reconcile with the trust ledger the project already maintains for proposals, decisions, moderation, and privacy access.

## Decision

Introduce a backend-owned coordination room contract with:

- one room per community, optionally bound to a project board (`projectBoardId`) so rooms can exist "per community or project group"
- one append-only message model per room
- one mention model resolved server-side from `@username` against community membership, ignoring unknown handles and self-mentions
- one mute model scoped to `(room, user)`
- one auditable mention-read timestamp so notification state is user-specific and historical
- two permission scopes:
  - `POST_ROOM_MESSAGE`
  - `MANAGE_ROOMS`

Real-time delivery uses a server-sent event stream (`text/event-stream`) per room rather than a WebSocket/STOMP broker. SSE is provided by the already-present `spring-boot-starter-web`, needs no new dependency, and matches the actual traffic shape: clients post messages over authenticated REST and the server pushes change notifications. The frontend reads the stream with `fetch` + `ReadableStream` because native `EventSource` cannot send the `Authorization` header the JWT filter requires.

The frontend renders room lists, mention highlights, mute toggles, and the mention inbox, but room membership checks, mention resolution, mute state, and unread counts remain backend-owned.

## Consequences

Positive:

- working groups can coordinate in one auditable place per community or project
- mention state is per-user and timestamped, so notification history survives and can be explained
- mute state never deletes messages, so muting reduces noise without erasing the record
- no new runtime dependency was introduced

Trade-offs:

- the event broker is an in-memory per-JVM registry, so a multi-instance deployment needs Redis pub/sub or a STOMP broker before events can be relied upon across nodes
- SSE connections are long-lived and hold a server thread; the timeout is bounded at 30 minutes and the client reconnects
- rooms are community-scoped only. Private/direct messaging is explicitly out of scope for this story

## Validation

- `CommunityRoomsIT` covers room creation authorization, mention resolution and self-mention suppression, per-user unread counts, mention read acknowledgement, per-user mute state, and non-member isolation
- OpenAPI updated in the same change set with room, message, mention, mute, and event schemas
- `community-rooms.spec.ts` covers the workspace render, mention message post, mute toggle, and room creation form
- `global-state-integrity.spec.ts` extended to cover `/communities/rooms` so the new route cannot reset persisted community state