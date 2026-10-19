# ADR-20260321: Bias Diagnostics, Not Fairness Weighting

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #9
- **Contract:** `packages/contracts/openapi.yaml` (`/api/signals/bias-diagnostics`)

## Context

The issue asks for "fairness weighting and bias diagnostics". The two halves are not equally
buildable, and treating them as one request would have produced something dishonest.

**Fairness weighting is not buildable here.** Weighting a ranking by equity requires equity data:
who lives where, which areas are underserved, which populations under-report. This platform holds
none of it. An "equity multiplier" would therefore be a number invented to look fair, applied to
real priorities, invisibly. That is exactly the black-box governance the product forbids, and it
would be worse than no feature: it would let the platform claim a fairness property it cannot
substantiate.

**Bias diagnostics are buildable**, using data that does exist. If reports about roads wait twenty
times as long as reports about parks, that is measurable, explainable, and fixable, and nobody had
to invent a demographic weight to see it.

## Decision

### No weighting is applied, and the report says so on every response

`scopeStatement` is returned with every diagnostic: it states that this measures consistency of
treatment between categories, that it does **not** measure fairness across populations, and that no
fairness weighting is applied to any score.

This is in the payload rather than only in the docs because the report is titled "bias
diagnostics". Left to inference, a reader would reasonably conclude the platform had measured
fairness across people. It has not, and a report that stayed quiet about that would be its own kind
of dishonesty.

### The comparison standard is the best-treated category, not the community average

This changed during implementation, and the reason is the whole point.

The first version compared each category against the community median. A test with half the
categories at 2 days and half at 40 produced a community median of 21, so the slow category read as
`40 / 21 = 1.9x` against a `2.0x` threshold and **was not flagged at all**. The average masked
precisely the disparity it was supposed to surface.

The standard is now the best-treated eligible category: fastest median resolution, ties broken on
resolution rate, then attention, then name for determinism. `referenceCategory` is named in the
response so a reader sees what the comparison was drawn against.

The justification is not statistical, it is procedural: **a community cannot call unequal treatment
unavoidable while one of its own categories is already doing better.** That is a claim a council
can act on, and it cannot be argued away as an average.

### Disparity needs enough reports, and insufficient ones are counted not flagged

`MIN_CATEGORY_REPORTS = 3`. Below it, the category is marked `NO_BASELINE` and `categoriesWithoutBaseline`
records how many.

Two reports averaging 90 days is noise. Flagging it would teach people to ignore the flags, which
costs the feature its entire value — the same reasoning as the surge detector's volume floor.

### Flagged signals are excluded

A report a moderator removed should not distort the picture of how the platform treats people.

### Three flags, each with its inputs

- `SLOWER_RESOLUTION`: median days at or above the reference's times `2.0`.
- `LOWER_RESOLUTION_RATE`: resolved share at or below the reference's minus `0.30`.
- `LOWER_ATTENTION`: votes per report at or below half the reference's.

Every threshold is readable at `/api/signals/bias-diagnostics/formula` and echoed in the report,
so a flag is never a verdict without its arithmetic.

Resolution-time comparison is skipped when the reference is zero. Otherwise "twice as slow" would
be `0`, and every category would flag against a community that simply has not resolved anything yet.

### Read-only, and gated

No write route exists. A diagnostic that could change a score would be the weighting this ADR
declines to build.

Access requires `MANAGE_OPEN_DATA_EXPORTS` rather than being public. A report naming specific
categories as treated differently is a claim an institution should be able to review before it
circulates.

## Consequences

- Disparate treatment between categories is visible, quantified, and attributable.
- The platform makes no fairness claim it cannot support.
- A slow category cannot hide behind an average that it is itself pulling up.
- Thin categories are counted rather than turned into noise.
- One test failed and forced the redesign from average to reference. That test is kept.

## Known limits

- **Category is the only grouping.** Category is self-selected by the reporter and free text
  upstream, so inconsistent filing splits a real group across two categories and hides its
  disparity.
- **No geography.** `locationLabel` is free text, so disparity by area cannot be measured. That is
  the same missing geography the surge detector names, and it is the measurement that would matter
  most for equity.
- No income, tenure, language, or disability data. Those are the dimensions that would make this a
  fairness measurement, and collecting them is a privacy decision this ADR does not make.
- `referenceCategory` can be a single unusually well-run category, which would make everything else
  look disparate. The named reference is the mitigation: a reader can see it and judge.
- Resolution time is measured to the first closing status entry, so a reopened signal understates
  elapsed effort.
- No false-positive review workflow. A flagged category is a prompt to look, not a finding.
- No UI. The diagnostic is an API and a published formula.
- Attention is proxied by community votes, which measures engagement, not institutional response.