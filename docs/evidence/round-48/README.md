# Round 48 evidence

Two deliverables: the transparency report shows the score a period actually had, and the status
timeline stopped depending on which engine stores it.

## Part 1: score history

Round 46 disclosed in the payload that the report showed today's score for a past period, and recorded
the full fix as pending a governance decision. It was not actually blocked, and finding out why was the
useful part of this round.

### Why it was not blocked

The obvious question is "when does a score become official". Nothing here needs it. There are exactly
**three runtime operations** that move a score, all in `PrioritizationServiceImpl`:

| Cause | What moved it |
| --- | --- |
| `INGEST` | The signal was recorded |
| `SUPPORT_VOTE` | A resident supported it |
| `DUPLICATE_MERGE` | Duplicates were merged in, summing votes |

I checked for a fourth before building anything: **approving a formula change does not rescore at
runtime.** `FormulaChangeProposalService.decide` records a decision and its ADR requires a change to
`PrioritizationFormula` plus a version bump, so there is no mass retroactive rescoring to miss.

So the ledger records facts — what happened, when, and why — and the read side applies the "as of the
period end" rule the report already applied to statuses. Any future policy about which score counts
belongs at read time, where it can be stated, rather than baked into storage.

### The test that caught my own fixture

`recordingASignalShouldStoreItsFirstScore` failed with 0 entries. The fixture saved the signal through
the repository, which bypasses `createSignal` where the INGEST entry is written. **A signal made outside
the service correctly has no history**, so the fixture was asserting nothing about production — the same
trap as the messaging connector tests in round 40.

### Verified by mutation, after fixing the test

The first mutation run **passed**, which meant the test could not tell a correct read from a mutated
one: the fixture had the same value (40.0) in the ledger and on the signal. With the live score at 320
and February's ledger at 40, reverting `scoreAtPeriodEnd` to the live score fails:

```
[ERROR] Tests run: 11, Failures: 1
[ERROR]   aClosedMonthShouldShowTheScoreItHeldNotTheScoreTheIssueReachedLater
```

A test that cannot fail when the code is broken is not evidence. This is the second time this session
that a green mutation run turned out to be the defect rather than the reassurance.

### Still not reproducible, and it says so

Signals recorded **before** this ledger existed have no entry; their history was never kept and cannot
be backfilled. The read falls back to the current score and `reproducibilityLimits` now describes that
narrower case rather than the old blanket one. A signal with no entry answers **empty** at the
repository, never a guess, so a caller can tell "unrecorded" from "known".

## Part 2: the timeline order, found by the gate

Preflight failed on `SignalAssignmentTimelineIT` — a test I had not touched — expecting
`STATUS_CHANGED` first and getting `ASSIGNED`. `clean test` had passed minutes earlier, so it was
nondeterministic rather than broken.

The timeline ordered by `created_at` alone. **H2's `TIMESTAMP` keeps no fractional seconds**, so two
events written in the same second tie and the database may return either first. PostgreSQL keeps
microseconds, which is why it never surfaced in development. Latent, not new.

An audit trail that renders two identical requests in different orders is not an audit trail, and three
consumers read it: the timeline endpoint, `getStatusHistory`, and the institutional bridge. Fixed with a
`seq` identity column (`V50`), mapped read-only, and a query that sorts by `created_at DESC,
coalesce(seq, 0) DESC`. Existing rows are backfilled by `(created_at, id)`, with the sequence restarted
above them; the true order between two rows written in the same second was never recorded and is not
invented.

`row_number() OVER (...)` in an `UPDATE SET` clause is not portable — H2 rejects it — so the backfill
uses a correlated count, which both engines accept. Found by running it, not by reasoning about it.

### Proof it is no longer a flake

`npm run agent:preflight` twice consecutively: passed, passed.

## Full verification

- `mvn clean test`: **429 pass, 0 fail** (was 424; +5) — `mvn-clean-test.txt`
- `npm run agent:preflight`: `Agent preflight passed.`, twice — `agent-preflight.txt`
- `npm run contract:parity:check`: `178 routes across 194 controller sources, 147 documented paths,
  9 intentionally undocumented.` — `contract-parity.txt`
- Targeted: `TransparencyReportIT` 11, `SignalScoreHistoryIT` 4, `PrioritizationFormulaIT` 3,
  `SignalAssignmentTimelineIT` 1