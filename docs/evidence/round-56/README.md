# Round 56 evidence

Deliverable: the 49 migrations now run in the test suite, and two production bugs came out of it.

## What was actually happening

`application-test.yml` set `ddl-auto: create-drop`. That drops whatever Flyway built and recreates every
table from the entity mappings. So:

- No migration had ever executed in a test.
- The foreign keys, unique indexes and check constraints the migrations write were **absent** from the
  test schema.
- V50's `seq` column (round 55) was absent, which is why it was NULL on every row.

The suite was validating behaviour against a schema that cannot exist in production.

## Two production bugs, found because the constraints were finally there

Both are the same mistake: "release the current row, then insert the new one", where Hibernate orders
**inserts before updates and deletes** within a flush. The old row still held the slot when the new one
landed on it, and the unique index rejected it.

**`BacklogPublicationService.publish`** - V38 puts a unique index on
`(community_id, current_slot)`. Publishing a second backlog therefore failed with a constraint
violation. The feature is literally called "publish again supersedes the previous".

```
Unique index or primary key violation: "PUBLIC.IDX_BACKLOG_PUBLICATIONS_CURRENT
 ON PUBLIC.COMMUNITY_BACKLOG_PUBLICATIONS(COMMUNITY_ID, CURRENT_SLOT) VALUES (... 1)"
```

**`AssemblyFacilitationService.setAgenda`** - V44 puts a unique index on
`(assembly_id, position)`, so replacing an agenda collided with the items it was replacing. Both
existing tests, `publishingAgainShouldSupersedeThePreviousCurrent` and
`settingAnAgendaAgainShouldReplaceItRatherThanAppend`, had been asserting the correct behaviour for
rounds and passing. They were the reproduction; no new test was needed.

Both fixed with an explicit flush before the insert (`saveAndFlush`, `flush`).

## Ten tests were writing data production would reject

The rest were test bugs the missing foreign keys had been concealing:

- `DuplicateDetectionIT`, `PrioritizationServiceIT`, `SignalMetaEndpointIT`,
  `SignalPrioritizedExplainabilityIT` created signals with `UUID.randomUUID()` as the author. There is a
  foreign key from `signals.author_id` to `users.id`; a random id is not a signal, it is an orphan.
  They now create a real user.
- `CommunityBlogCommentCountsIT` did the same for a blog post's author and community.
- `SignalDetailEndpointIT` and `UserProfileVisibilityIT` called `deleteAll()` on parent tables
  (`users`, `communities`) to isolate themselves. With real foreign keys, wiping a parent while children
  reference it is invalid. Both are now `@Transactional` and the global wipes are gone - rollback
  isolates a test properly, a `deleteAll()` only hid the problem.

`PrioritizationServiceIT` also needed `@Transactional` added, because the new user fixture collided on
the unique username between its two tests.

## Twenty test classes were sharing one database

Twenty `*IT` classes used the default `jdbc:h2:mem:testdb` while others declared their own
(`roomsit`, `freshit`, `snapit`). Under `create-drop` that was survivable, because each Spring context
rebuilt the schema. With Flyway the schema is built once and `DB_CLOSE_DELAY=-1` keeps it alive for the
whole JVM, so data leaked between classes. Each of the twenty now declares its own database, matching
the convention the rest of the suite already followed.

## The test that stops anyone turning this back off

`MigrationSchemaIT` asserts the schema was built by the migrations: at least 49 successful entries in
`flyway_schema_history`, a named constraint from V1 (`FK_SIGNAL_AUTHOR`), and V38's
`IDX_BACKLOG_PUBLICATIONS_CURRENT`.

The first version asserted "some foreign key exists" and **passed with `create-drop` restored**,
because an entity mapping that declares a relationship does produce one. Asserting a count is not
asserting provenance. Replaced with the specific names, which do.

Verified by mutation, with `ddl-auto` put back to `create-drop`:

| Assertion | Result with migrations disabled |
| --- | --- |
| V1's `FK_SIGNAL_AUTHOR` | 1 failed |
| V38's unique index | 1 failed |
| Unmutated | 3 pass |

Diagnostic that pinned the numbers: **139 foreign keys** when Flyway builds the schema, **7** when
Hibernate does.

## A stale `target/` nearly made me report a green run that was not

`mvn -o test` without `clean` kept using a stale
`target/test-classes/application-test.yml` that still said `create-drop`. Two tests "failed" for reasons
that had nothing to do with the code, and the diagnostic showed 7 foreign keys - the configuration I
thought was active was not the one running.

This is the same hazard the plan already records for `target/` being tracked, in a new shape: not a
stale `.class` making a test pass against old code, but a stale **resource** making a test run against
old configuration. Every result in this round's evidence comes from `mvn -o clean test`.

## Verification

| Check | Result |
| --- | --- |
| Full suite, `clean` | **441** pass (was 438, +3) |
| `BacklogPublicationIT` | 12 pass - supersede works |
| `AssemblyFacilitationIT` | 12 pass - agenda replace works |
| `MigrationSchemaIT` | 3 pass |
| Mutation: `create-drop` restored | 2 failed |
| `npm run agent:preflight` | passed |
| OpenAPI parity | 178 / 147 / 9 - unchanged |

## Known limits

- **Only H2 exercises the migrations.** The suite runs them on H2; PostgreSQL-specific behaviour is
  still unverified until the suite runs against the production engine somewhere. The migrations were
  written to be portable and V46/V50 already carry H2 workarounds, but "written to be portable" is not
  "verified on both".
- **`validate` checks the shape, not the data.** A migration that creates a column with the right name
  and the wrong constraint passes.
- **`baseline-on-migrate: true` remains.** On a non-empty schema with no history, Flyway baselines and
  skips. Harmless on an empty test database, but it means a corrupted test DB would silently migrate
  nothing rather than fail loudly.
- The twenty test classes that got their own database still rely on `deleteAll()` in a few places.
  Harmless now that the databases are private, and a cleanup rather than a defect.