# Round 59 evidence

Deliverable: the parity check compares column **types**, not just names.

## The gap

Rounds 56 to 58 made the suite's H2 schema stand in for production and proved it matches PostgreSQL on
table and column **names**. A column that exists in both engines with the same name and a different type
passed all of it.

## What the two engines actually spell

Measured across all 582 columns rather than assumed:

| | H2 | PostgreSQL 15 |
| --- | --- | --- |
| UUID | 203 | 203 |
| string-ish | 199 (`CHARACTER VARYING`) | 136 `character varying` + 63 `text` |
| timestamp | 99 (`TIMESTAMP`) | 99 (`TIMESTAMP WITHOUT TIME ZONE`) |
| integer | 44 | 44 |
| boolean | 18 | 18 |
| double | 9 | 9 |
| date | 7 | 7 |
| bigint | 3 | 3 |

**162 columns differ raw, in exactly two pairs.** The other 420 agree exactly - including the
integer/bigint split, which the normalisation deliberately keeps distinct. A migration that turned a
`bigint` into an `integer` would still be caught.

So the whole normalisation is two rules, and it is written down in the test rather than hidden in a
helper:

- H2 reports `TEXT` columns as `CHARACTER VARYING`; PostgreSQL keeps them apart. Both map to `string`.
- H2 calls it `TIMESTAMP`; PostgreSQL spells out the absent time zone. Both map to `timestamp`.

Anything else is compared verbatim, so an unanticipated spelling difference surfaces instead of being
absorbed.

## Verified it fails, with the 99 columns named

Removed the `TIMESTAMP` rule and re-ran. The test fails and names every affected column:

```
Expecting empty but was: {"ASSEMBLY_AGENDA_ITEMS|CREATED_AT"="TIMESTAMP vs timestamp without time zone",
 "ASSEMBLY_DECISIONS|RECORDED_AT"=..., "CIVIC_COMMENTS|CREATED_AT"=..., ... 99 entries}
```

That is a real detection, not a tautology: the comparison walks 582 columns and reports the ones that
disagree.

**The first mutation attempt was a false negative.** The multi-line replacement I used to strip the rule
did not match - the file has CRLF line endings and the replacement had LF - so the "mutated" run was
actually the unmutated code and it passed. A mutation that silently does not apply is worse than no
mutation: it produces a green result that certifies nothing. Confirmed by grepping the file for the rule
before believing either result, then re-doing it with the editor.

## What this still cannot see

H2's `information_schema` does not report `text` separately from `varchar`, so a column that is
`VARCHAR(255)` in PostgreSQL and `TEXT` in the suite passes this check. Distinguishing them means
comparing `character_maximum_length` across engines, and the two engines report that differently enough
to need its own investigation. Written into the test's javadoc rather than left as a surprise.

Nullability is likewise not compared: `is_nullable` is meaningful on both engines and would catch a
migration that makes a `NOT NULL` column nullable on one engine only.

## Verification

| Check | Result |
| --- | --- |
| `npm run schema:pg:verify` | 49 migrations, validate clean, names and types agree |
| Mutation: `TIMESTAMP` rule removed | 1 failed, 99 columns named |
| Full suite, `clean` | **444** run, 3 skipped (parity trio without Docker) |
| `npm run agent:preflight` | passed |
| OpenAPI parity | 178 / 147 / 9 - unchanged |

## Known limits

- **varchar vs text is invisible**, as above. The next honest step in this chain, and the reason it is
  written down rather than glossed.
- **Nullability not compared**, and it is cheap to add: `is_nullable` on both sides.
- **Precision and scale not compared** - `numeric_precision`, `numeric_scale`. Nothing in the schema uses
  a `NUMERIC` today, so this is untested ground rather than a known gap.
- The workflow that runs this still **has never executed on GitHub** (round 58's caveat, unchanged).