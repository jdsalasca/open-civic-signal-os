# ADR-20260321: Dashboard Filters Must Change The Query

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #40
- **Contract:** `packages/contracts/openapi.yaml` (`minScore` on `/api/signals/prioritized` and `/api/signals/top-10`)

## Context

The dashboard shipped with five quick filters. Four of them behaved. One did not.

`NEW`, `IN_PROGRESS`, and `RESOLVED` went into the request as `status`, and `ALL` sent no
parameter. `CRITICAL` was different: it was applied in the browser,

```ts
signals.filter((s) => (s.priorityScore ?? 0) >= CRITICAL_SCORE_THRESHOLD)
```

over the rows already fetched, with the threshold hardcoded to `220` in the component.

AGENTS.md names this exact anti-pattern: *no visual-only filters: any dashboard filter chip must
change the underlying query or deterministic dataset, not just local highlight state*.

## Decision

### `minScore` goes into the query, and the total reflects it

`GET /api/signals/prioritized?minScore=220` filters in SQL via
`findByStatusNotInAndPriorityScoreGreaterThanEqual`, so `totalElements` is the real count of
critical signals in the community.

The difference is not cosmetic. The old code reported "how many of the twenty rows I happen to
have are critical". A resident looking at a backlog of 400 signals with 90 critical ones was told
"3", because three critical signals happened to fall inside one page. That is a filter that
understates the size of a problem by an order of magnitude, and the kind of quiet miscount this
project exists to avoid.

### A threshold must never resurface moderated content

The threshold-only branches keep the `FLAGGED`/`REJECTED` exclusion that the unfiltered path
already applies, via `HIDDEN_STATUSES`.

This was nearly a real bug. The first implementation used a plain
`findByPriorityScoreGreaterThanEqual`, which would have meant that switching a filter **on** brought
back content a moderator had removed, and switching it off hid it again. Moderation state is not
something a score threshold is allowed to undo. Covered by
`thresholdMustNotResurfaceModeratedSignals`.

### One threshold, published, not two copies

`SignalController.DEFAULT_CRITICAL_SCORE_THRESHOLD` is the single source, returned as
`criticalScoreThreshold` on `GET /api/signals/meta`. The dashboard reads it and uses it in the
query. The local constant survives only as a bootstrap value for the first paint before `/meta`
resolves, and is named `CRITICAL_SCORE_THRESHOLD_FALLBACK` to say so.

The old component constant is gone, because a second copy of a threshold is how the ingest
formula copy happened in the first place.

### The threshold combines with status rather than replacing it

`status=NEW&minScore=220` narrows both. The repository has four score-aware finders rather than a
single method with optional parameters, because each combination is a distinct query and pretending
they are one is how the moderation exclusion got dropped in the first place.

### Invalid thresholds fail loudly

A negative `minScore` returns `400`. Silently treating it as "no filter" would answer a question
nobody asked: a caller requesting "everything above minus ten" would get the entire backlog with
no indication that their filter did nothing. `NaN` and `Infinity` are rejected the same way.

### The test asserts on requests, not on the DOM

`dashboard-filter-query.spec.ts` records every outgoing `signals/prioritized` query and asserts
that `minScore=220` appears after the click and that the unfiltered request does not carry it. It
also asserts the value is the server's, not a client constant.

Asserting on rendered rows could not have caught this bug: the old implementation rendered
correct-looking critical rows. The defect was in the number shown and the query not changing, and
only the requests reveal that.

## Consequences

- The critical count a resident sees is the real count for the whole community.
- The threshold has one definition, changeable in one place.
- Turning on a filter cannot expose moderated content.
- Invalid filter input fails loudly instead of silently no-opping.
- The threshold is query-pushdown, so it costs an index scan on an existing index rather than a
  full table read.

## Known limits

- `priority_score` is a persisted column recomputed by `withScore` on read. The filter therefore
  runs against the stored value, not a live calculation. If the stored value is stale relative to
  the current formula, the filter reflects the stale number until the signals are rescored.
  `priority_score` is persisted because the ranking already indexes it, and changing that is a
  larger piece of work than this issue.
- Only `minScore` is pushed down. `category` and a date range are still client-side on this
  endpoint.
- The bootstrap fallback could briefly apply 220 if the server threshold were ever changed below
  that, for the one paint before `/meta` resolves. The CRITICAL click is the thing that matters and
  it happens after the effect re-runs with the server value.
- No test asserts the threshold against `top-10` end-to-end through the browser, only through the
  API.