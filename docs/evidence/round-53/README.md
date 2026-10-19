# Round 53 evidence

Deliverable: the two room screens stop asking the database once per row.

## What the audit was looking for

Round 52 found a room read that loaded a whole history to count it, and left a note: the same shape
probably exists elsewhere. This round audited the shape rather than a symptom.

The smell is not "loads a list". Loading every message of one room is legitimate when the answer needs
every message - bias diagnostics, trust metrics, aging, the transparency report all genuinely read the
whole set. The smell is **a query whose result is used per row of an already-loaded collection**: the
outer loop is driven by something the database could have answered, and each step opens a round trip.

Three instances, all on the two screens a coordinator opens to find out whether anyone is active.

| Screen | Was | Now |
| --- | --- | --- |
| Room detail | 58 queries for 50 messages | 8 |
| Workspace | 61 entity loads for 30 members | 5 |

## Three N+1s, one of which the first test could not see

`mentionsByMessage` asked `findByMessageId` once per message. Up to 200 queries for one screen, and
round 52 had just made 200 the page cap.

`memberIdsByUsername` asked `findById` once per membership. A community of 500 members cost 500 queries
on the workspace screen.

And the one that matters for how this round was written: **`displayNameOf(message.getAuthorId())` was
called inside the `.map()` that renders each message.** One user query per message row, each dragging a
fully managed `User` entity into the session - password hash, roles and all - to read two columns.

That third one is invisible to a test where every message has the same author, because the second and
later `findById` calls hit the first-level cache. My first version posted every message as
`rooms_coord`, saw the mention queries clearly (58 vs 12), and would have passed with the author N+1
fully in place. Seeding one author per message is what exposed it: **58 vs 13**.

An N+1 behind a cache is still an N+1. It only shows up with realistic data, and "realistic" here
means "a conversation with more than one person in it".

## A test that passed with the bug in place

The workspace test was wrong twice before it was right.

It first asserted on `getQueryExecutionCount()`. Reverting the fix - restoring the per-member
`findById` - left it **green**. `getQueryExecutionCount` counts HQL and criteria executions and does not
count a load-by-id, which is exactly what the N+1 used. A test that cannot fail is worse than no test:
it certifies the defect as correct. Switched to `getEntityLoadCount()`, which does count it, and the
same mutation went red immediately (61 vs 5).

The room test keeps `getQueryExecutionCount()`, because its defects are both real queries. Each test
uses the metric its own defect actually moves, and the blind spot is written down here rather than left
for someone to discover: a future `findById` in a render loop would not be caught by the room test.

Round 52's entity-load test needed `em.clear()` before measuring, or the first-level cache answers and
the assertion passes for the wrong reason. Both tests here do it.

## Fourth fix, found by the test rather than by reading

The residual "+1" after the mention fix was not an N+1. Spring Data runs a count query to fill in a
`Page`'s total, fires it only when the page came back full, and `getRoom` **threw that total away** -
it asks `countByRoomId` separately. So the room open counted twice, and the extra probe appeared and
disappeared depending on how many messages happened to be in the room.

Returning `List` with a `Pageable` parameter instead of `Page` applies the same limit without the
count. One fewer query per room open, and the query count stopped depending on the page size for a
reason that had nothing to do with the page size.

## Verification

| Mutation | Result |
| --- | --- |
| `displayNameOf` back inside the render loop | 1 failed - 58 vs 13 |
| per-member `findById` restored, entity-load metric | 1 failed - 61 vs 5 |
| unmutated | 436 pass (12 in CommunityRoomsIT, +3) |

- Full suite: **436** (was 433).
- `npm run agent:preflight` passed. OpenAPI parity unchanged at 178 / 147 / 9 - no endpoint or field
  changed.

A third test covers the state every new room starts in: zero messages, so both page-scoped queries
would be matching an empty list. Not a mutation check - the guards it covers are three lines - but an
empty `IN ()` is a runtime failure waiting for the first room created in a pilot.

## Known limits

## Known limits

- **The mention inbox still resolves a display name per row.** `CommunityRoomMentionResponse` calls
  `displayNameOf` inside a stream, bounded by `.limit(20)`. Same shape, different screen, bounded at 20
  so left for its own round rather than widened here.
- **`toSummary` still costs two queries per room** (a count and a page of one). O(rooms), not
  O(messages), but a single grouped aggregate would be better at high room counts.
- **No index review.** `(room_id, created_at)` is assumed from the entity mapping; worth confirming
  against the production schema before onboarding a large community.
- The projections added here (`findUserIdsByCommunityId`, `findDisplayNamesByIdIn`) are the only place
  in the codebase using JPQL for this shape. Most other services still load entities to read a few
  columns; that is the next place to look, not a claim that it is fixed.