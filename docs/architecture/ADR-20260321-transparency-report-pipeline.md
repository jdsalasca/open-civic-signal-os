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

## Known limits

- `actioned` and `unaddressed` are capped at 10 each. A community with more needs pagination
  or a separate appendix, not a longer default.
- Resolution time is measured from signal creation to the first closing status entry. A
  reopened signal counts its first close, which understates elapsed effort. Reopen tracking is
  a separate piece of work.
- No scheduling. The script is manual by design: an automatic monthly publish needs a
  decision about who is accountable when the numbers come out wrong.