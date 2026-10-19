# Round 54 evidence

Two findings: the one the plan predicted, and one that was blocking the gate.

## 1. The mention inbox: three queries per row

Round 53's note said the inbox had "the same shape, bounded at 20". Reading it, each row cost **three**
lookups, not one:

```java
CommunityRoom room = roomRepository.findById(mention.getRoomId())...        // 1
CommunityRoomMessage message = messageRepository.findById(...)...          // 2
... displayNameOf(message.getAuthorId())                                   // 3
```

A full inbox cost 60 queries to draw a list the code itself caps at 20 - on the screen that tells a
resident they have been mentioned.

### Measured

`openingTheMentionInboxShouldNotQueryOncePerMention` posts mentions from **20 different authors** and
compares the query count for an inbox of 5 against an inbox of 20:

```
the mention inbox ran 30 queries with 20 pending mentions and 15 with 5:
reads scale with the inbox, so it is querying per row ==> expected: <15> but was: <30>
```

The delta is exactly the 15 extra name lookups. After the fix both measurements are equal.

Different author per mention is not decoration. Round 53 established that a single author is answered
by the first-level cache, which hides the defect; twenty authors is also what a real room looks like.

### What the counter does and does not prove

`getQueryExecutionCount()` counts HQL and criteria executions and does **not** count a load-by-id. The
room and message lookups are load-by-id, so this counter cannot see them, and entity loads cannot
measure them either: every inbox row shows a *different* message body, so message loads legitimately
grow with the rows and a flat assertion there would be false.

The measurement proves the name batching and states plainly that it does not prove the room and message
batching. Those are round trips - `3N + 1` before, `4` after - which is arithmetic from the code, not a
number this suite can honestly claim to have measured.

The fix reads each of the three once for the whole inbox: `findAllById` for rooms and messages, and the
`findDisplayNamesByIdIn` projection round 53 introduced. Null handling and ordering preserved exactly -
`pending` is newest-first and `dedupedByMessage` is a `LinkedHashMap`, so the same first-20 come out in
the same order.

## 2. What preflight caught: an assembly evidence list with no defined order

`npm run agent:preflight` failed with a test that had been passing for rounds:

```
JSON path "$[0].label" expected:<April assembly> but was:<March assembly>
```

`ExplainabilitySnapshotIT.shouldListSnapshotsForACommunity` creates two snapshots and expects the newer
one first. Ordering was `OrderByCreatedAtDesc` alone. H2 keeps no fractional seconds, so two snapshots
created milliseconds apart routinely share a timestamp, and the database is then free to return either
first. It passed on most runs and failed on the rest.

This is the same defect as V48's timeline `seq` and V52's `IdDesc` - ordering that depends on a
timestamp alone - in the third place I had missed it.

**Fixed with an id tiebreak**, which makes the order stable.

**The test no longer asserts something untrue.** It asserted "April first", which is not a guarantee the
system makes when two rows claim the same instant. It now asserts both labels are present and that two
consecutive reads return the same bytes - which is the guarantee that actually holds. That is the change
that turns a coin flip into a deterministic test.

### The tiebreak is not black-box verifiable, and I did not pretend otherwise

I wrote a test that forced the tie and asserted the order was stable. **It passed with the tiebreak
removed.** H2 is self-consistent for a given plan and data layout, so two identical queries return the
same order by luck. A black-box test cannot tell "ordered by createdAt, id" from "ordered by createdAt".

So the test was deleted rather than shipped. A test that cannot fail is worse than no test - it
certifies the defect as correct, which is the mistake this round's other test had made twice already.

What is verified: the suite no longer contains a random failure. What is not verified: that the
tiebreak is what removed it.

### The real fix needs a monotonic position, and that exposed a bigger problem

`V51__Explainability_Snapshot_Order.sql` was written, mirroring V50 exactly: an identity column, a
correlated-count backfill, a restart above the backfill. Then the test still failed, and the reason is
the finding worth keeping:

**The suite never populates any `seq` column.** `application-test.yml` sets `ddl-auto: create-drop`, so
Hibernate drops whatever Flyway built and recreates the schema from the entities. `SignalStatusEntry.seq`
is mapped `insertable = false, updatable = false`, so Hibernate never writes it: under the suite it is
NULL forever. No test in the repository references `seq` at all.

In production (`ddl-auto: validate` + Flyway) the column exists and the database assigns it, so V50's
fix works there. **It has simply never been exercised.** The audit-trail ordering guarantee that round
48 added is unverified by the test suite, and the same would be true of any snapshot equivalent.

Making `seq` work under `create-drop` needs either the entity to declare the generation - Hibernate 6
refuses `@GeneratedValue` off the identifier, and the `@Generated(event = INSERT)` form needs its own
investigation - or the suite to stop replacing Flyway's schema, which is a much larger decision about
what the integration tests are actually testing.

That is the next round, not this one. The migration was deleted rather than left half-wired.

## Verification

| Check | Result |
| --- | --- |
| `CommunityRoomsIT` | 13 pass (+1) |
| `ExplainabilitySnapshotIT` | 8 pass |
| Full suite | **437** |
| `npm run agent:preflight` | **passed** (was failing) |
| OpenAPI parity | 178 / 147 / 9 - unchanged |

## Known limits

- **`pending` is still unbounded.** `findByMentionedUserIdAndCommunityIdAndReadAtIsNullOrderByCreatedAtDesc`
  has no limit and `.limit(20)` runs on the in-memory list, so the screen is bounded and the query is not.
  Bounding it means pushing "newest mention per distinct message, first 20" into the database, which
  JPQL cannot express portably and which, done subtly wrong, would hide a mention from a resident.
- **The snapshot order is stable but not provably newest-first within a tie.** Resolving that needs the
  monotonic position above.
- **`displayNameOf` now has one caller** (the POST response). If that caller goes, the helper should go
  with it.
- No index review on `(mentioned_user_id, read_at)` or `(community_id, created_at)`. Same standing
  caveat as `(room_id, created_at)`.