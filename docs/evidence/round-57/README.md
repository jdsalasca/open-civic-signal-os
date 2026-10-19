# Round 57 evidence

Deliverable: the migrations run against PostgreSQL too, and the suite's schema is checked against it.

## The gap round 56 left

Round 56 made the integration tests build their schema with Flyway on H2, and stopped there with one
caveat written down: *"Only H2 exercises the migrations... 'written to be portable' is not 'verified on
both'."*

That caveat mattered more than it looked. From that moment the suite stood in for production, and the
only thing checked was that H2 agreed with the entity mappings. Whether H2 agreed with **PostgreSQL** -
the engine production actually runs - was nobody's job.

## What was never known: do the migrations work on PostgreSQL at all

They do. All 49, first try:

```
Successfully applied 49 migrations to schema "public", now at version v50 (execution time 00:04.000s)
```

Then `validate`, which is the check that only a database with history can give: a migration whose
checksum no longer matches what ran.

## And do the two engines come out the same

The more interesting question, and the answer is yes, exactly:

| | H2 (the suite's schema) | PostgreSQL 15 |
| --- | --- | --- |
| tables | 62 | 62 |
| columns | 582 | 582 |
| only in this one | 0 | 0 |

Nothing was enforcing that. A migration that quietly did the right thing on only one engine would pass
the whole suite and fail on deploy.

## How it is checked

`npm run schema:pg:verify` starts a throwaway PostgreSQL on `127.0.0.1:55432`, applies the real
migration files with the official Flyway image, validates, then runs `PostgresSchemaParityIT` against it.

The parity test compares table and column **names, not types**. The engines spell types differently on
purpose and a type comparison drowns in false positives; a missing or extra column is the failure that
has actually bitten - V50's `seq` existed in the migration and in production, and nowhere else.

It compares against the suite's own live H2 schema rather than a checked-in snapshot, so it cannot drift
from what the tests actually run.

`PostgresSchemaParityIT` skips unless `POSTGRES_VERIFY_URL` is set. Requiring Docker for every run would
make it the most skipped test in the repository; this way it runs in `npm run schema:pg:verify` and in CI
where a database is available, and does not make `mvn test` depend on Docker.

### Verified it can fail, with real data

Ran against a PostgreSQL whose schema had been dropped:

```
[the PostgreSQL schema is empty or unreachable - did the migrations run against it?]
```

Two of two failed. Then the migrations ran and the same command passed. The red was a genuinely empty
database, not a simulated one.

### Two dead ends on the way, both of which looked like "the schemas disagree"

The script reported `FAILED` with no Maven output at all, twice, for reasons that had nothing to do with
schemas:

- `spawnSync mvn ENOENT` - Maven is not on a Node process's PATH on Windows even though PowerShell
  finds it.
- `spawnSync mvn.cmd EINVAL` - `execFileSync` cannot execute a `.cmd` directly; it needs `cmd.exe`.

Both surface as "verification failed" and neither says a word about schemas, which is exactly the kind
of failure that gets "fixed" by loosening an assertion. The script now resolves Maven explicitly and
captures its output so a real failure prints what Maven said.

## Why a throwaway container and not the developer's database

The first attempt pointed at `civic-db` from `infra/docker-compose.yml` and failed authentication: the
volume was initialised with an older password than the one in `infra/.env`. Two ways to read that - reset
the volume, or leave it alone. Resetting destroys local data that is not mine to destroy, and worse, it
would make this check depend on the state of a database someone works in.

So the verification brings its own PostgreSQL, on its own port, with a throwaway password that is bound
to loopback and removed with the container. Two consecutive runs both passed with no leftover container.

**Not fixed, and it should not be silently:** the developer's `civic-db` volume has a password that does
not match `infra/.env`, so `docker compose up` against it will fail to authenticate until someone
recreates the volume or corrects the file. That is local state, not a repository defect.

## Verification

| Check | Result |
| --- | --- |
| `npm run schema:pg:verify` | 49 migrations applied, validate clean, schemas agree |
| Same, run twice | identical, no leftover container |
| Red check: unmigrated PostgreSQL | 2 failed |
| Full suite, `clean` | **443** run, 2 skipped (the parity pair, no Docker) |
| `npm run agent:preflight` | passed |
| OpenAPI parity | 178 / 147 / 9 - unchanged |

## Known limits

- **Not in CI yet.** `schema:pg:verify` needs Docker, so it is a command a person runs rather than a gate
  that runs on every push. Wiring it into a workflow is the obvious next step and is deliberately not
  bundled here.
- **Names, not types.** A migration that creates `VARCHAR(255)` where the entity expects `TEXT` on one
  engine only would pass. Comparing types needs a normalisation table per engine and would be its own
  round.
- **PostgreSQL 15 only**, which is what `infra/docker-compose.yml` pins. A different major version is not
  covered.
- **The Flyway CLI is 11.20.3** via `flyway/flyway:11`, while the application depends on a Flyway version
  resolved by Maven. Two versions reading the same history is a small risk; the `validate` step is what
  would surface it.