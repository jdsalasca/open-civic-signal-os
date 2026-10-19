# Round 60 evidence

Deliverable: the parity check compares nullability and string length too, closing the gap round 59
documented as invisible.

## Measured first, again

| | H2 | PostgreSQL 15 |
| --- | --- | --- |
| NOT NULL | 441 | 441 |
| nullable | 141 | 141 |
| bounded string lengths | 24 distinct values | the same 24, same counts |
| unbounded strings | 63, reported as length `1000000000` | 63, reported as `text`, no length |

Nullability already agrees exactly. All 136 bounded string columns agree exactly, across 24 distinct
lengths. The 63 unbounded ones are where the engines describe the same thing differently, which is the
rule this round had to encode.

## What this closes

Round 59 wrote this down as a known blind spot:

> H2's information_schema does not report `text` separately from `varchar`, so a column that is
> `VARCHAR(255)` in PostgreSQL and `TEXT` in the suite passes this check.

The length is exactly what distinguishes them, so comparing it closes the gap. H2 spells an unbounded
string `1000000000`; PostgreSQL reports no length at all. Both fold to `unbounded`, and a real
`VARCHAR(255)` against a `TEXT` no longer looks identical.

### Proven with a real divergence, not a simulated one

Migrated PostgreSQL, then shortened one column the way a careless migration would:

```
alter table users alter column username type varchar(64)   -- was 255
```

The suite still had 255. The test fails, naming the length mismatch: *"these string columns have the same
name and type but a different maximum length, so the suite would accept a longer value than
production"*.

## A tautological mutation, and why it was not the one that counted

First attempt at proving the length test bites: replace the compared value with `"bounded"` on both
sides. It **passed**. That is not a detection failure, it is a tautology - stop comparing lengths and
there is nothing left to fail. It does show the test's value is entirely in comparing the numbers, which
is why the real `ALTER TABLE` above is the mutation worth recording.

The nullability mutation was different and did fail: `suite.nullable() != other.nullable()` was inverted
so only some disagreements registered, and the test reported it. Verified with grep before believing the
result, after round 59's lesson about a mutation that silently does not apply.

## The test caught my own wrong assumption

Wrote `Integer.MAX_VALUE` as H2's marker for "no limit", because that is what an unbounded length looks
like. The test failed on all 63 columns with `1000000000 vs unbounded`.

Measured value, not the plausible one - the same rule that produced rounds 52, 55 and 59. It is now a
named constant with the reasoning in a comment.

A second one, on the way: `(Integer) rows.getObject(5)` threw for every test when PostgreSQL was
connected and was fine when it was not, because H2 returns an `Integer` and the PostgreSQL driver returns
a `Long` for `character_maximum_length`, a domain over `integer`. A failure that only appears on one
engine reads like a schema problem; it was a cast.

## The shape of the test changed

The three parallel structures from rounds 57-59 - a `Set` of names and two `Map`s of types - became one
`record Column(String key, String type, boolean nullable, Integer maxLength)` read from both engines the
same way. Four assertions now read a column description rather than reaching into parallel maps, and the
`seq` check from round 55 is expressed against the same record.

## Verification

| Check | Result |
| --- | --- |
| `npm run verify` -> `npm run schema:pg:verify` | 49 migrations, validate clean, all four dimensions agree |
| Real mutation: `username` varchar(64) in PostgreSQL | 1 failed |
| Nullability mutation | 1 failed |
| Full suite, `clean` | **446** run, 5 skipped (parity quintet without Docker) |
| `npm run agent:preflight` | passed |
| OpenAPI parity | 178 / 147 / 9 - unchanged |

## Known limits

- **Precision and scale are still not compared** - `numeric_precision`, `numeric_scale`. Nothing in the
  schema uses `NUMERIC`, so this is untested ground rather than a known gap.
- **Default values and column order are not compared.** Column order can matter for `SELECT *` consumers,
  and no test in the suite would notice a reordering.
- **The workflow has still never run on GitHub** (round 58's caveat, unchanged for a third round).
- The three old stashes and two superseded `codex/*` branches remain in the repository, deliberately
  (round 58).