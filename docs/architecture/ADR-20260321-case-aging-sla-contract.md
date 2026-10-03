# ADR-20260321: Case Aging And SLA Risk Contract

- Status: Accepted
- Date: 2026-03-21
- Closes: GitHub `#23`, `#45`
- Story: `OCS-P1-001`

## Context

Institutions are accountable for how long a civic case stays open, but the platform exposed no way to see that. `Signal` carried `createdAt` and a status, and nothing joined the two into an answer to "is this community keeping up?".

The obvious frontend implementation — compute `Date.now() - createdAt` in the browser — would be wrong in a way that matters here: a citizen and an institution looking at the same dashboard could see different risk for the same case if their clocks or filters differed, and a client-computed threshold would not be something a public accountability page could cite.

## Decision

Add a backend-owned aging contract at `GET /api/signals/aging`:

- `slaTargetDays` — one flat target for every case, default 30, overridable per request
- `slaRisk` per case, derived server-side: `BREACHED` above the target, `AT_RISK` at or above 80% of it, otherwise `ON_TRACK`
- `ageBuckets` — `FRESH_0_7`, `AGING_7_14`, `STALE_14_PLUS`, `OVERDUE`
- `medianAgeDays` over unresolved cases
- `atRiskSignals` — only non-`ON_TRACK` cases, worst first, capped at 20
- `trend` — 30 points of created-versus-resolved counts, so a community sees throughput, not just backlog
- `generatedAt` — freshness timestamp on the payload, per the "every dashboard feature must expose data freshness" rule

Resolved-per-day counts are read from `signal_status_history` rather than a denormalized `resolved_at` column on `signals`. That keeps one source of truth for when a case changed state, and means the trend is derived from the same audit trail the timeline view shows.

Membership is enforced when `communityId` is present, so a community's aging data is not readable by non-members. The unscoped call covers the platform-wide backlog, matching the existing `/signals/prioritized` behaviour.

The dashboard fetches aging in its own effect rather than inside `loadData`. The dashboard cache is keyed on the signal page and filter; folding aging in would either invalidate that cache on every SLA change or quietly serve a stale panel.

## Consequences

Positive:

- institutions can see unresolved volume, at-risk count, breached count, median age, and whether they are closing cases faster than they open them
- risk is a single server-side definition, so a public page and the staff dashboard cannot disagree
- the trend reads from the existing audit trail rather than a second, divergent record of when a case closed

Trade-offs:

- **one flat SLA target for every case.** A pothole and a hospital outage get the same 30 days. Per-category or per-priority targets need a policy record; the service carries a `ponytail:` comment naming that as the upgrade path rather than guessing weights now.
- the 80% "at risk" threshold is a constant, not a community setting.
- the trend is a fixed 30-day window with no comparison period, so a seasonal community cannot see a longer baseline.
- `atRiskSignals` is capped at 20. A community with more than 20 problem cases sees a truncated worst-offender list, and the counts are the only complete signal.
- `belongsToCommunity` resolves each status-history entry back to its signal with a lookup. That is an N+1 over the trend window; at community scale it is fine, but it wants a join or a projection if the window grows.

## Validation

- `SignalAgingIT` (7/7) covers bucketing and risk ranking across fresh/aging/stale/breached/resolved cases, median age, the default target when omitted, the created-versus-resolved trend read from status history, non-member rejection, the exact risk boundaries (23 on track / 24 at risk / 31 breached against a 30-day target), and JSON content type
- OpenAPI updated with `SignalAging`, `SignalAgingItem`, `SignalAgingBucket`, `SignalAgingTrendPoint`, and `SignalSlaRisk`
- `dashboard-aging.spec.ts` covers the staff panel (counters, buckets, worst breaching case, freshness) and asserts the panel is absent for citizens with zero requests to the endpoint