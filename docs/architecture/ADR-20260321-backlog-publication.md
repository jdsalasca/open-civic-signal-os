# ADR-20260321: Publishing The Backlog As A Checkable Claim

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** the publish half of GitHub #35 and #50
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/backlog-publications`)

## Context

The explainability snapshots from the earlier ADR freeze what an assembly saw. That answers a
question about a moment in the past. It does not answer the question a visitor has:

> Is what I am looking at **now** the thing that was published?

Without a recorded ordering hash there is nothing to compare against, and a public ranking could
shift under visitors with nothing to notice it. That was the gap both issues named, and it was left
open on purpose rather than closed on partial work.

## Decision

### Publishing names the artifact, and records a hash of it

`POST /api/community/backlog-publications` freezes the current ranked backlog into an
explainability snapshot and records:

- the snapshot id, so the ranking itself is recoverable and tamper-evident
- an `orderingHash` over the published set
- the formula version in force
- a label, required

`GET .../current` returns the one publication the community is currently serving, with a live
comparison. `GET .../verify` returns both hashes and what a mismatch means.

### The hash covers the set and its scores, not the order

This is the decision the ADR exists for, and the obvious alternative is wrong.

The public endpoint sorts by score alone (`sort = "priorityScore"`), so rows with **equal scores**
come back in whatever order the database chooses. Hashing the literal order would produce
verification failures for a reason nobody can act on — and a check that cries wolf is worse than no
check, because it teaches people to ignore it.

So the hash is over the **sorted** `id:score` lines. Two lists are the same published backlog if
they contain the same signals at the same scores. Tie order, which is arbitrary anyway, is not
part of the claim.

Pinned by `correctingATitleShouldNotBreakTheMatch` and
`aNewReportBelowThePublishedSetShouldNotBreakTheMatch`: neither changes the published set, so
neither should fail.

### A mismatch says what changed and does not accuse anyone

`verify` distinguishes the three real causes — added, removed, or rescored — and states plainly
that this is **not proof of tampering**, because a new report or a vote legitimately changes a
ranking. It closes with what to do: publish again, or state which version is authoritative.

A check that reported only "different" would be unusable; one that implied misconduct every time a
resident filed a report would be actively harmful.

### Claim and check are both public

`GET /current` and `GET /verify` are `permitAll` in `SecurityConfig`. A claim only its publisher can
check is not one a visitor can trust, which is the whole point.

`POST` and the history remain gated on `MANAGE_OPEN_DATA_EXPORTS`: publishing declares what the
public sees, which is a moderation act rather than a read.

### Exactly one current publication, enforced by the database

A partial unique index (`... WHERE is_current = TRUE`) is the natural expression and **H2 does not
support it**, so the test suite cannot run it. The portable equivalent is a nullable slot: the
current row holds `1`, superseded rows hold `NULL`, and `NULL`s never collide in a unique index on
either H2 or Postgres. `(community_id, current_slot)` is therefore unique while allowing any number
of superseded rows.

The column reads oddly and the reason is now written into the migration, because the next person to
look at it will otherwise "simplify" it into a boolean and lose the guarantee.

## Consequences

- A visitor can ask whether the backlog they see is the one that was published, and get an answer.
- The published set is recoverable from a frozen snapshot and its hash.
- Legitimate changes (new reports, votes, typo fixes) do not produce false alarms.
- Publishing again supersedes cleanly, with the history intact.
- Which version is live is a database guarantee, not a convention.

## Known limits

- **No external anchor.** If the publication row and its hash were both altered, verification passes.
  A published digest or a signature is needed against an attacker who can write to the database. The
  same limitation as the explainability snapshots, stated there too.
- **The check compares against the live ranking for one community at a time.** A global backlog
  publication, as the anonymous `/backlog` page serves, is not covered.
- **Nothing forces the public view to serve the published version.** The check reports a mismatch; it
  does not prevent one. Auto-pinning the public view to the published snapshot is a separate change.
- The hash is not a signature, so it proves integrity, not authorship.
- `itemCount` fixes the compared size. Publishing 20 then having a 21st high-scoring report arrive
  still mismatches, because the displaced item leaves the set — correct, but worth knowing.
- No scheduled republish, and no alert when a publication goes stale. The freshness monitor does not
  cover publications.