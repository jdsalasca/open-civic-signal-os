# ADR-20260321: Freshness Monitoring That Admits Ignorance

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #51
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/freshness`)

## Context

Freshness already appears on dashboards as a per-record string: "updated 3h ago". This ADR is
about the thing that is missing, which is monitoring.

A staleness alert that fires on a broken intake and a staleness alert that fires on a genuinely
quiet community are the same alert, and they send a responder to opposite places. The first
wants someone checking a webhook; the second wants nobody doing anything at all. A monitor that
cannot tell them apart is worse than no monitor, because it trains people to dismiss it.

## Decision

### Every verdict carries a cause, and UNKNOWN is the expected answer

`SourceFreshness.cause` is `ON_CADENCE` only when the surface is behaving normally. Every
silence that cannot be explained from the data is `UNKNOWN`, with a `reason` that states both
candidate causes and refuses to pick between them.

This is the load-bearing decision. The alternative was a monitor that reported "stale" and left
the responder to guess, which is where the wrong-guess cost comes from. The guidance string
names the two things to check in order: the intake path, then whether a municipal integration
was the channel carrying the data.

### Silence is judged against the community's own cadence

Not a global threshold. The baseline is the **median gap between that community's own
consecutive events**, so "stale" means "quieter than this place normally is".

A community that reports weekly and has been silent for a month is worth investigating. One
that files twice a year and has been quiet for a month is not, and a global threshold would page
someone for it.

Median rather than mean, for the same reason the surge baseline uses one: a single long gap
caused by a holiday or a freeze would become that community's "normal" gap and mask every future
alert. Pinned by `oneLongGapShouldNotBecomeTheNormalGapAndMaskFutureAlerts`.

### No cadence means no verdict, not a clean bill of health

Below four events the verdict is `NO_BASELINE`; with none at all it is `NO_HISTORY`. Both carry
`cause: UNKNOWN`.

This is a deliberate refusal. A community that has never held a decision is not a community
whose decisions stopped arriving, and reporting it as healthy would be a false all-clear. The
reason string says "setup question, not a dropout".

### Staleness is monitored per surface, not per community

`REPORTS`, `PROPOSALS`, `DECISIONS`, `STATUS_CHANGES`. They fail independently and mean
different things. A community with reports but no proposals is not necessarily broken, and a
community with proposals and no decisions has a different problem than one with neither.

Note that a broken intake takes out several surfaces at once, which is exactly why
`staleCount` is returned: three surfaces going quiet simultaneously is one incident, not three.

## Consequences

- Silence is always explained or explicitly declared unexplainable.
- A responder is told what to check, not just that something is wrong.
- Communities with genuinely low activity are not paged.
- Thresholds are relative to each community's own behaviour.
- The monitor never returns a false all-clear for a surface it cannot judge.

## Known limits

- **No alerting transport.** This reports; nothing pages anyone. Wiring it to email or webhook
  is the obvious next slice, and it needs a decision about who gets woken up.
- Cadence is computed from raw event history with no exclusion for known outages, so an
  incident that produced a 90-day gap can raise the baseline for that community.
- No seasonality: a community with a reliably quiet August will read as stale every August.
- `NO_HISTORY` and `NO_BASELINE` are permanent states for a surface nobody uses. A community
  that only files proposals and never votes will see `REPORTS` as `NO_HISTORY` forever, which is
  noisy. Treating unused surfaces as not-applicable is a follow-up.
- `totalEvents` is the only volume context returned; a richer history would help a responder
  confirm whether recent behaviour matches the baseline period.