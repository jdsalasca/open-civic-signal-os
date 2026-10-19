# ADR-20260321: Monthly Transparency Report Pipeline

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #48 (story) and GitHub #28 (script)
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/transparency-report`)

## Context

Two requirements pointed at the same missing thing.

`AGENTS.md` says every release must publish a "what changed for communities" summary.
`docs/community/current-backlog.md` carries `story:OCS-P1-004` as the next enablement layer.
Neither existed as anything executable.

The platform already had a trust-metrics dashboard, and the tempting read was "the report is
just the dashboard with a print button". That reading is wrong, and the difference is the whole
decision.

## Decision

### A report is over a closed month; a dashboard is over a rolling window

They are not the same artifact at different widths.

A dashboard answers "what should I do right now", so a rolling 30/90/365-day window is
correct. A published report answers "what did you do in March", and it gets quoted in a
council session and challenged months later. "The last 30 days" produces different numbers on
every render, so the figures in a published document could not be reproduced or refuted.

`period` accepts `YYYY-MM` or a full ISO date and **defaults to the previous calendar month**.
Defaulting to the current month would, on the 3rd of April, publish three days of data as
"this month's report".

### No wall-clock reads in the computation

Every figure derives from the stored period bounds. The only exception is `generatedAt`, which
describes the run rather than the community and is the single field allowed to vary.

The trap this avoids is real and was nearly shipped: days-open for a still-unresolved signal is
measured to the **end of the reported period**, not to today. Measuring to today means a March
report regenerated in June shows different numbers, which looks like a data correction and is
actually a computation drift. Pinned by
`daysOpenForUnresolvedSignalsShouldNotDriftWithTheWallClock` and
`regeneratingTheSameMonthShouldProduceIdenticalFigures`.

### The comparison column is mandatory, and the unaddressed list is non-negotiable

Every metric carries `previousValue`, `delta`, and `direction`. A renderer cannot publish the
trendless half by accident.

The generator **refuses to run** on a report missing a previous period, a metric without a
comparison, or a missing `formulaVersion`. That last one matters: a figure nobody can tie to a
scoring formula cannot be re-derived after the formula changes.

`unaddressed` is the part a resident uses to judge whether the institution is keeping up. A
report listing only resolved items is marketing. Rejected signals are counted separately from
resolved and deliberately kept out of `actioned`: a rejection is a legitimate outcome, but it
is not a civic win.

### The renderer refuses to invent a verdict

Resolution time and the still-open count get an explicit improved/worse reading, because lower
is genuinely better. Rising intake gets **the bare delta and no framing**.

This started wrong. The first implementation read a rising report count as "worse", which is
also an invention: more reports may mean more problems or more participation, and the data
cannot distinguish those. A transparency report has no business guessing, so only the two
measures with an unambiguous direction are annotated. Pinned by
`leaves an ambiguous intake metric unframed` and `does not claim rising intake is a win`.

### The script is idempotent and fails fast

`scripts/generate-transparency-report.mjs` writes stable-key-ordered JSON and Markdown, and
skips the write when bytes are unchanged. Regenerating a month produces an empty diff, so a
figure that moves shows up as a reviewable change rather than a silent correction.

Fail-fast covers a malformed period, an unreachable API, a non-200, a missing previous period,
a non-numeric metric value, and a missing formula version. A half-rendered report that looks
plausible is worse than no report.

## Consequences

- A published month is reproducible and challengeable.
- A report cannot present intake growth as a failure, nor failure as success.
- Open items are always named, so the document cannot read as promotional.
- The report is diffed against committed data changes, so drift is visible in review.
- `generatedAt` is the only volatile field, which is enough for freshness without breaking
  reproducibility.
- The report requires community membership. A published artifact for a community a caller does
  not belong to is not currently reachable; a public share link is a future slice.

## A past period is answered from the audit trail, never from live state

The `unaddressed` list filtered on `signal.status`, the mutable column. The `SIGNALS_STILL_OPEN` metric
and the `actioned` list already read the audit trail bounded by the period end, so the two disagreed:
February's report published "1 still open" in its metrics beside an unaddressed list that omitted the
same issue.

That is the worst kind of wrong for this product. A closed month's record of what a community failed to
address **shrank every time the community did the work later** — a fence fixed in April silently vanished
from February's list, with no trace that the published figure had changed. Nothing errored, and every
individual number was defensible on its own.

So the list is now computed the same way as its own metric: reported in the period, and no settled
status entry at or before the period end. `statusAt` shows the status as it stood then, rather than
today's, for the same reason.

This is safe because settled states are terminal — `SignalStatus.canTransitionTo` refuses to leave
`RESOLVED` or `REJECTED` — so "was it settled by then" cannot change after the fact. The digest made the
same call for a closed week, and the weekly digest's preparation now stores the artifact for the same
reason.

Pinned by `theStillOpenCountAndTheUnaddressedListMustAgree`, which asserts the metric and the list in
one report answer the same question. It is the strongest form of the test: it fails on internal
inconsistency rather than on a fixture, so it cannot be satisfied by getting one side right.

### What is still not reproducible

**Priority scores have no history.** `priorityScore` is mutable and there is no ledger of its past
values, so the score shown for a past period is today's score. An item could appear in February's
unaddressed list at a score it did not hold in February.

Stating that in the javadoc was not enough. The record previously claimed flatly that "everything is
derived from the stored period bounds, so regenerating the same month later yields the same figures",
and a consumer told a report is reproducible will reasonably rely on that. So the gap travels in the
payload as `reproducibilityLimits`, where the dashboard, the municipality or the journalist integrating
against the JSON will actually meet it. The OpenAPI description of `generatedAt` said it was "the only
field that varies"; that was false for the same reason and now names the scores.

**Superseded by the score ledger below.** The disclosure now describes the narrower remaining case:
signals recorded before score history existed.

### Score history, recorded as fact rather than as policy

`priorityScore` is mutable and had no ledger, so a past period showed the score the item carries now.
Round 46 disclosed that in the payload. This is the other half.

`signal_score_entries` records the score, its four inputs, the formula version and the **cause** at
each of the three operations that move a score:

| Cause | What moved it |
| --- | --- |
| `INGEST` | The signal was recorded and scored for the first time. |
| `SUPPORT_VOTE` | A resident supported the issue, raising its votes. |
| `DUPLICATE_MERGE` | Duplicates were merged in, summing their votes. |

There is no fourth. Approving a formula change does **not** rescore anything at runtime — it records a
decision and requires a change to `PrioritizationFormula` plus a version bump, so the ledger cannot
silently miss a mass retroactive rescoring.

**Why recording a cause matters:** a score that rose because a resident supported an issue is a
different fact from one that rose because two duplicates were merged. A trail that cannot tell them
apart is not much of a trail.

**Why this was not blocked on a governance decision.** The obvious question is "when does a score
become official". That question is not needed here: the ledger records what happened at each operation,
which is a fact, and the read side applies the "as of the period end" rule this report already applies
to statuses. Any future policy about which score counts can be applied at read time, where it can be
stated, rather than being baked into the storage.

The raw inputs travel with the score, read off the signal rather than off `ScoreBreakdown`, which
holds the weighted terms. An audit record wants the numbers a person declared, not the arithmetic
applied to them.

### What remains unreproducible

Signals recorded **before** this ledger existed have no entry, so their past score is unknowable. The
read falls back to the current score and `reproducibilityLimits` says exactly that, rather than
claiming the whole report is reproducible. Backfilling is not possible: the history was never kept.

A signal with no entry answers **empty** at the repository, never a guess, so a caller can tell
"unrecorded" from "known".

Pinned by mutation: reverting `scoreAtPeriodEnd` to the live score makes
`aClosedMonthShouldShowTheScoreItHeldNotTheScoreTheIssueReachedLater` fail — after the fixture was
corrected so the live score and the period-end score **differ**, since identical values let a mutated
implementation pass.

## Known limits

- `actioned` and `unaddressed` are capped at 10 each. A community with more needs pagination
  or a separate appendix, not a longer default.
- Resolution time is measured from signal creation to the first closing status entry. A
  reopened signal counts its first close, which understates elapsed effort. Reopen tracking is
  a separate piece of work.
- No scheduling. The script is manual by design: an automatic monthly publish needs a
  decision about who is accountable when the numbers come out wrong.