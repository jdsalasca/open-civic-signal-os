# ADR-20260321: Weekly Digest As A Closed-Week Snapshot

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** the generation half of GitHub #43; leaves delivery for #22 and #20
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/weekly-digest`)

## Context

`CommunityIntegrationChannel.EMAIL_DIGEST` existed as a channel with nothing to send. The dashboard
had a `DigestSidebar` rendering whatever rows were already on screen. There was no server-side
digest: nothing that decided what belonged in a weekly bulletin, and nothing that could stop the
same bulletin going out twice.

## Decision

### Generation and delivery are separate, and only generation is built here

Generating a digest is deterministic, testable, and harmless. Sending one reaches residents. The
email connector exists; handing it a digest body is a genuinely separate decision about who on the
community side is accountable when a bulletin goes out wrong. Building half of that wiring and
leaving the question unanswered would be worse than not wiring it.

`POST /publish` records the publication and returns the sealed body. It does not send.

### A closed ISO week, addressed as 2026-W13

Not "the last seven days". A window whose end moves produces different content every time it is
regenerated, so a resident who saved last week's bulletin could never check it. `week` defaults to
the **previous completed** week, because on Wednesday the current week is three days of data and
mailing that as "this week's digest" would misdescribe the community.

Nothing in the content reads the wall clock. The only live value is `generatedAt`, which describes
the run.

### The top list is the backlog as of the week end, from the audit trail

This changed during implementation, and a failing test forced it.

My first version listed the currently-unresolved signals. That meant a digest for March
regenerated in June would include a report filed in April — the same non-determinism the closed
week exists to prevent, reintroduced one layer down.

The list is now the open backlog **at the end of the reported week**:

- Created after the week ended: not part of that week's backlog.
- Closed before the week ended: resolved by then, not outstanding.
- Created before the week ended and not yet closed: outstanding. **Including an old report that is
  still open** — a backlog digest that omitted long-standing items would hide exactly the problems
  it exists to surface.

Closure is read from `signal_status_history`, not the mutable `status` column, because the column
carries no timestamp and cannot be asked what it said last March. Pinned by
`aClosedWeekShouldNotChangeWhenLaterReportsArrive`.

### Every item carries its reason

`whyRanked` is the same four inputs the published formula uses: urgency, impact, people affected,
community votes. Not a generated sentence about them. AGENTS.md requires every list to expose why
an item is ranked where it is, and the point of a digest is that a resident can check the claim.

### Rejections are counted separately from resolutions

A rejection is a legitimate outcome with a recorded reason, but it is not a civic win. Rolling the
two into one "closed" number would make a month of rejections look like a month of delivery.

### Publishing is idempotent per week, enforced by the database

A unique index on `(community_id, week_key)`. A second publish for the same week returns `409`.

This is deliberate and not a soft warning. A weekly bulletin delivered twice reaches residents
again, and it does so at exactly the moment the platform is asking to be trusted to send things
once.

### The body is sealed with a hash

`contentHash` is a SHA-256 over the rendered body, and the body is kept verbatim in
`community_digest_publications`. A recipient can verify the digest they received matches what was
recorded rather than a later edit — the same guarantee the explainability snapshots provide.

## Consequences

- A past week's digest reproduces exactly.
- A digest cannot silently absorb activity from outside its week.
- Residents cannot receive the same weekly bulletin twice.
- Every item's position is checkable against the published formula.
- Resolutions and rejections are not conflated.

## Known limits

- **No delivery.** Nothing sends. The channel exists; wiring it needs the accountability decision
  above.
- **No scheduling.** `week` defaults to the previous completed week, but nothing runs on a
  schedule. #22's "scheduled weekly bulletin" is not done.
- **No per-recipient rendering.** One body per community. A resident who filed three of the listed
  items sees no acknowledgement of that.
- **No unsubscribe or preference handling.** Those belong with delivery, and delivery is not built.
- Categories are the only grouping; there is no geographic section, for the same missing-geography
  reason the surge detector and bias diagnostics both name.
- The digest is limited to 50 items and says so in the body when the list is truncated.
- Regenerating a week whose data was *corrected* after publication produces a different body than
  the sealed one. That is correct behaviour — the seal records what was sent — but nothing surfaces
  the difference, so a community cannot currently be told "the numbers you received have since been
  revised".