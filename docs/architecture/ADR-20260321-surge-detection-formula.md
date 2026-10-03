# ADR-20260321: Surge Detection As A Visible Formula

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #25
- **Contract:** `packages/contracts/openapi.yaml` (`/api/signals/surges`)

## Context

The heat map already counts signals per community. What was missing was the temporal
comparison and the threshold, which is where this kind of feature usually goes wrong.

"Surge detected" is a badge that ends up on a council officer's screen. If it appears for the
wrong reason the cost is not a bug report, it is an officer spending an afternoon on a flood
that was three potholes, and then not looking at the badge again. The failure is silent and it
costs trust, which is the one thing this project is about.

So the design question was never "how do we detect a surge". It was "how does someone check
our answer".

## Decision

### The unit is community plus category, not neighbourhood

The issue asked for neighbourhood trends. The platform has no validated neighbourhood
geography: `locationLabel` is free text written by residents, so "Barrio Centro", "barrio
centro", "Centro barrio" and "centro" are four buckets. Grouping on it would put
confident-looking badges on whichever spellings happened to match, and a badge that lands on
the wrong area is worse than no badge.

Community plus category is the unit a council officer actually works with, and it is a unit the
data supports. Neighbourhood granularity needs a geography this platform does not have; that
is a separate piece of work, not a threshold tweak.

### The baseline is a median over adjacent, non-overlapping, prior windows

Median, not mean. A mean over a window containing the spike is inflated by the spike, so a
genuine surge reports a smaller ratio the larger it gets. A median does not move for one
outlier window, which is exactly the signal being sought.

Adjacent and non-overlapping, because overlapping windows let one busy afternoon appear in
several of them and inflate the baseline with the activity being measured.

**Excluding the current window was a bug the tests caught.** The first implementation's most
recent baseline window *was* the current window, so the baseline already contained the spike
and the surge vanished. Pinned by
`baselineWindowsShouldExcludeTheCurrentWindowAndNotOverlap` and
`shouldExcludeTheCurrentWindowFromTheBaseline`.

A consequence worth stating: with four windows, at least two must look normal for the median
to mean anything. One busy week does not create a baseline.

### An absolute volume floor, and suppressed zones are counted

Without a floor, a quiet zone going from one report to three is a "300% surge". A dashboard
full of those teaches people to ignore the marker, which costs the feature its entire value.

Below the floor the verdict is `SUPPRESSED_LOW_VOLUME` rather than `SURGE`, and the count is
returned as `zonesSuppressedByMinVolume`. A suppressed zone is still a zone someone might be
waiting on, so hiding it entirely would be its own small dishonesty.

### Thresholds are query parameters, echoed back

A community's volume differs from a city's. `surgeMultiplier`, `minCurrentCount`,
`baselineWindows`, and `windowDays` are parameters so a community can tune what counts without
a deployment, and they come back in the response. A surge is never reported without the
thresholds that judged it.

Invalid values are rejected, not coerced. `surgeMultiplier=0` would otherwise divide by zero
and silently mean something else.

### Every verdict carries its arithmetic

`baselineCounts` travels with each zone so a human can check the median rather than trust it,
and `reason` states the comparison in plain language: "14 reports against a baseline median of
3 is 4.7x, at or above the 2.0x threshold."

`ratio` is `Infinity` when there is no baseline, which is arithmetically right and useless in a
report, so it renders as `no-baseline`.

`GET /api/signals/surges/formula` returns the versioned formula and default thresholds alone,
so a dashboard can explain itself without fetching every zone.

## Consequences

- A surge can be audited: current count, each baseline window, the median, the ratio, the
  threshold.
- False positives from quiet zones are suppressed and counted rather than displayed.
- A community tunes its own sensitivity without a deployment.
- Bad thresholds fail loudly.
- Detectable only at community+category granularity. Stated rather than approximated.

## Known limits

- A property-based suite over seeded synthetic series now covers the formula, added after the
  window bug described above: windows never overlap each other or the current window, the median
  sits between its extremes and does not move for a single outlier, a surge requires both volume
  and ratio, the ratio is monotone in current count for a fixed baseline, and no reason string
  can leak `Infinity` or `NaN`. Still not exhaustive, but the class of mistake that slipped
  through the examples is now covered.
- No seasonality handling. A community with a reliably busy August will see summer surges every
  year. A year-over-year comparison would be the fix and is a different slice.
- No per-zone baseline persistence, so a zone created last week has no meaningful baseline and
  reports `no-baseline` rather than waiting.
- No geospatial check. Two signals 50m apart and two 20km apart both count once, because there
  is no geography to weigh them by.