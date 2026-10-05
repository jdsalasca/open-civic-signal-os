# Round 50 evidence

Deliverable: opening a coordination room no longer downloads its whole history.

## The defect

`GET /api/community/rooms/{roomId}` took no `limit` and the service loaded every message in the room.

The inconsistency was visible in the same controller: the endpoint immediately above it,
`GET /api/community/rooms/workspace`, *does* take a `limit` and passes it to `CommunityListLimits`.

On a platform whose stated UX goal is field use on a phone with little bandwidth — a stated goal in
`AGENTS.md`, not my invention — a room that has run for months was downloaded, deserialised and sent
whole on every open. Nothing errored; it just got slower for everyone as the room aged.

## Two things fixed, not one

**The unbounded read.** `limit` added to the route and the service, resolved through the existing
`CommunityListLimits.resolveLimit` (default 50, cap 200) rather than a new limit of my own.

**A second, worse instance found while fixing it.** The SSE stream endpoint called
`roomService.getRoom(...)` and **discarded the result**. It was calling it purely for the access
check, so every stream connection loaded a room's recent history for nothing. Now
`requireRoomAccess(...)`, which does the membership check and the room lookup and builds no view.

## Capping silently would be its own lie

The obvious way to ship this is to truncate the array and leave the rest of the payload alone. Then a
consumer rendering "no older messages" from a capped list is wrong, and nothing in the response says
so.

So `messageCount` reports the **true total** (it previously reported the array length, so the two were
the same number by construction and the field carried no information), and a new `hasMoreMessages`
states that the list is a page. `hasMoreMessages` is in the contract's `required` list for the same
reason: a consumer that ignores it must at least be able to see it is there.

## Verified by mutation

Replacing the resolved limit with `Integer.MAX_VALUE`:

```
[ERROR] Tests run: 8, Failures: 2
[ERROR]   a room read should be bounded, got 60 messages ==> expected: <true> but was: <false>
```

## A test bug worth recording

My first version of `aCallerCanAskForMoreOfABusyRoomAndTheCapIsRespected` asserted the returned array
equals `MAX_LIMIT` (200) for a room with 60 messages. It cannot: the cap is a ceiling, not a target. The
assertion now checks what the test is actually for — a limit larger than the room returns the room
whole, says there is nothing more, and never exceeds the shared maximum.

Third time this session that the wrong assertion, not the wrong code, was the thing producing a
confident wrong answer.

## Full verification

- `mvn clean test`: **432 pass, 0 fail** (was 430; +2) — `mvn-clean-test.txt`
- `npm run agent:preflight`: `Agent preflight passed.`, `Contract change with ADR update detected;
  gate passed.` — `agent-preflight.txt`
- `npm run contract:parity:check`: `178 routes across 194 controller sources, 147 documented paths,
  9 intentionally undocumented.` — `contract-parity.txt`
- Targeted: `CommunityRoomsIT` 8 pass
- `git diff --stat packages/contracts/openapi.yaml`: 19 insertions, 2 deletions — the contract change
  is the limit parameter plus the two response fields, nothing else drifted.