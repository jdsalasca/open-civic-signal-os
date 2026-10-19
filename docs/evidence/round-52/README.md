# Round 52 evidence

Deliverable: counting a room's messages no longer reads them.

## What round 51 left behind

Round 50 bounded the room payload and round 51 made the UI admit the truncation. Both left the
database doing the work the limit was supposed to prevent.

`getRoom` read every message and then applied `.limit()` in memory:

```java
List<CommunityRoomMessage> allMessages = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
List<CommunityRoomMessage> messages = allMessages.stream().limit(effectiveLimit).toList();
```

The payload was bounded. The query was not. A room with 1,204 messages still read all 1,204 rows to
send 50.

## The one round 50 missed, which is worse

The same file, a few lines down, in `toSummary`:

```java
List<CommunityRoomMessage> messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(room.getId());
// ... messages.size(), and messages.get(0).getCreatedAt()
```

`toSummary` runs **once per room** on every workspace open. It loaded each room's entire history to
call `.size()` on it and read the newest timestamp. The workspace is the screen a coordinator opens
*to find out which room is busy* - it was reading every message of every room to draw a list of rows.

This is the defect the plan's rule 5 predicts: two paths for the same concept, one reviewed, one not.
Round 50 audited the endpoint it had just changed and left its sibling alone.

## The test had to measure I/O, not output

The counts were correct before and after. `messageCount` was 60, then 60. Asserting it proves nothing,
which is exactly rule 1 in the plan: a test that asserts a value the broken code also produces cannot
fail.

So the test compares the same query against the same room with two history sizes - 5 messages, then
60 - and asserts the number of entity loads is unchanged. If reads scale with history, it is loading
messages instead of counting them.

```java
long withFiveMessages = workspaceEntityLoads();
// ... 55 more messages posted ...
long withSixtyMessages = workspaceEntityLoads();
assertEquals(withFiveMessages, withSixtyMessages, "...");
```

Before the fix:

```
opening the workspace read 66 entities for a 60-message room and 11 for a 5-message one:
the reads scale with history, so it is loading messages instead of counting them
==> expected: <11> but was: <66>
```

After: both are the same, and the count still comes back right.

Two things had to be got right for this to be a real measurement rather than a green lie:

- **`entityManager.clear()` before measuring.** The test posts messages through the same persistence
  context. Without the clear, the workspace is served from the first-level cache and reports 0 loads -
  passing for entirely the wrong reason. `assertEquals(0, ...)` was my first version and it would
  have been worthless.
- **Hibernate 7 dropped per-entity statistics**, so `getEntityLoadCount(Class)` does not compile. The
  differential form above is what replaced it, and it is the better assertion anyway: it is immune to
  whatever else the screen happens to load.

## The fix

- `CommunityRoomMessageRepository` exposes only `findByRoomIdOrderByCreatedAtDescIdDesc(roomId, Pageable)`
  and `countByRoomId`. The unpaged variant is **deleted**, not deprecated - there is no accessor to fall
  into by accident, which is how the plan's rule 4 asks a duplicate to be removed.
- `getRoom` passes `PageRequest.of(0, effectiveLimit)` and takes the total from `countByRoomId`.
- `toSummary` takes the count from `countByRoomId` and the newest timestamp from a page of **1**.

`IdDesc` breaks `createdAt` ties on purpose. Two messages in the same second previously ordered
unpredictably, and under a limit that means a message can be skipped or shown twice between two page
requests - a paging bug that only shows up on busy rooms.

## Verification

- `CommunityRoomsIT`: 9 tests green.
- Full suite: **433** (was 432).
- `npm run agent:preflight` passed. OpenAPI parity: 178 routes / 147 documented / 9 intentionally
  undocumented - unchanged, as expected: no endpoint or field changed in this round.

## Known limits

- **The workspace still issues two queries per room** (a count and a page-of-one). That is O(rooms)
  round trips, not O(messages), which is the difference that matters here, but a single grouped
  aggregate query would be better at high room counts. Left alone: it needs a projection and a
  decision about how unread counts compose, which is a separate change.
- **No index review.** `(room_id, created_at)` is assumed to exist from the entity mapping. Worth
  confirming against the production schema before a large community is onboarded.
- The room detail page re-reads the full page when the limit is raised; there is still no cursor.