# ADR-20260321: Policy Simulation Sandbox

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #54
- **Contract:** `packages/contracts/openapi.yaml` (`/api/policy-simulation`)

## Context

`FormulaChangeProposalService` already answers "what would this one weight change do": how many
positions move, and the largest movers. That is a **diff**.

A diff is not enough to decide a policy. A change that moves forty percent of the backlog is not
necessarily a problem. A change that moves forty percent of the backlog **in one direction for one
category** is a policy choice, and it should be visible as one before anyone votes on it.

## Decision

### The unit of output is per-category gain and loss, not positions moved

`PolicySimulationService` reports, per scenario, a `CategoryShift` for each category: how many
signals gained, how many lost, the net change, and the **average places gained per signal**.

The average is the number a decision should be argued about, because it says how much a category is
affected rather than how many items happened to move. Ten signals moving one place each is not the
same policy event as one signal moving ten places, and a count cannot tell them apart.

`reading` names the category that gains most and the one that loses most, in a sentence:
*"Votes-heavy: 100% of the backlog changes position. infrastructure gains an average of 3.0
place(s). utilities loses an average of 3.0 place(s)."*

That sentence is the finding. The counts are supporting detail.

### It does not rank the scenarios

There is no "best" weight set, and the response says so on every call.

"Best" depends on what a community wants to prioritise, which is a value judgement the platform has
no standing to make. A sandbox that returned a winner would be making that judgement silently, and
the community would be arguing about the platform's ranking rather than about its own priorities.

Pinned by `shouldRefuseToRankTheScenarios`, which asserts the interpretation says so.

### One backlog snapshot, reused for every scenario

`simulate` reads the backlog once and runs every scenario against it.

Re-reading per scenario would let a concurrent report change the sample between scenarios, and the
comparison would be meaningless in a way nobody could see. `sampleSize` is reported once, for the
whole comparison, rather than per scenario, because it is the same sample.

### At most five scenarios, rejected rather than truncated

More than five is a spreadsheet, not a decision aid. The limit is a `400`, not a silent trim, so
nobody compares a partial set believing it was complete.

### Every scenario needs a name

An unnamed scenario cannot be referred to in the discussion it is meant to inform. Rejected with
that reason.

### Nothing is stored, and it is public

Like the single-change preview it extends. A community arguing about weights should be able to run
the comparison itself rather than ask the platform owner to run it for them, and a simulation that
persisted would start to look like a decision.

### It does not predict the future

Changing the weights changes what people report and vote on, so the backlog under any scenario would
not be this one. The interpretation says so, same as the single-change preview.

## Consequences

- A weight change can be seen as a policy consequence before it is decided.
- The category that gains and the one that loses are named, not left to be inferred from a count.
- The platform does not pick a winner, so the argument stays about the community's priorities.
- Scenarios are compared against one sample, so the comparison is like for like.
- Nothing is persisted, so a simulation cannot be mistaken for a decision.

## Known limits

- **No UI.** The endpoint exists and is tested; there is no sandbox screen. That is the remaining
  half of this issue.
- **Category is the only grouping.** Category is reporter-selected, so inconsistent filing splits a
  real group across two categories and hides its shift. Same limitation as the bias diagnostics.
- **No geography.** `locationLabel` is free text, so "which neighbourhood gains" cannot be answered.
  That is the dimension a participatory budgeting decision would most want.
- **No cost or budget modelling.** This simulates ranking weights only. A real policy sandbox for
  budgeting would need money, which the platform does not model.
- **No saved scenarios.** A community cannot keep a named set to re-run next month; the request
  carries them each time.
- **Ties are ordered by id**, so a shift caused only by a tie is arbitrary. The single-change preview
  says this in its interpretation; this one does not repeat it, which is an inconsistency.
- The scan is bounded at 2000 signals, so a very large backlog is simulated from its top 2000 by
  current score.