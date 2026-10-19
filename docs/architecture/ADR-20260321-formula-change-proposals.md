# ADR-20260321: Formula Change Proposals, And What They Do Not Do

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #34
- **Contract:** `packages/contracts/openapi.yaml` (`/api/formula-change-proposals`)

## Context

AGENTS.md requires that every scoring change include a formula diff and reproducibility evidence,
and that formula changes carry an ADR. There was no way to *propose* one, and no way to see what a
change would do before committing to it.

The second half is the interesting one. A community arguing about weights is arguing about a
number. What they actually need is to see that a proposal moves forty percent of the backlog, and
which items rise and fall.

## Decision

### The impact preview is the valuable half, and it is a simulation

`POST /api/formula-change-proposals/preview` reorders the current backlog under the proposed
weights and reports: the sample size, how many signals change position, the largest movers with
their before and after positions, and both expressions.

Nothing is stored, nothing is applied, and no scoring code reads it. It is safe to call with any
weights a community wants to consider, which is why it is public.

### The preview refuses to be read as a forecast

`interpretation` is returned on every preview and states, in the payload rather than only in docs:

- **It does not predict the future.** Changing how signals are scored changes what people report
  and vote on, so the backlog under the new weights would not be this one. A figure like "38% moves"
  invites a decider to read it as a prediction, and it is not one.
- **It does not judge.** It shows the mechanical consequence of the numbers; whether the change is
  right is a separate decision.
- **Ties are ordered by id**, so a movement caused only by a tie is arbitrary rather than a real
  shift in priority.
- **Small samples are called out** when there are fewer than twenty signals, because position
  changes over three signals are not meaningful.

### Approving does not apply anything, and that is structural

The formula is a constant in `PrioritizationFormula` that a deploy changes. There is deliberately
**no column the scoring code consults**: storing a row with a different status cannot change what a
resident sees. Applying an approved proposal means changing the constant, bumping the formula
version, and writing an ADR.

`applicationNote` restates this on every proposal response, and
`approvingShouldRecordADecisionAndNotChangeTheScoring` asserts that the live formula is untouched
after an approval.

A workflow that could hot-swap the scoring weights through a database row would be the black box
this project forbids, and it would make the published formula version a lie.

### Zero and negative weights are rejected at construction

`ProposedWeights` validates in its compact constructor. A zero multiplier makes every signal tie,
and a negative one produces negative scores. Both would surface as a confusing preview that looks
like a legitimate finding rather than as the invalid input it is.

### Scope

The preview is public; proposing and deciding require `SUPER_ADMIN`, and reading the record requires
a session.

The formula is platform-wide, so there is no community scoping that makes sense. Pretending a
community could change the platform's scoring would misrepresent who actually can.

### The rationale is required

A scoring change nobody explained cannot be judged after the fact. The rejection message says so.

## Consequences

- A proposed change comes with the evidence it would produce, computed from real signals.
- The preview cannot be mistaken for a forecast or for a verdict.
- An approval is a recorded decision, never a live configuration change.
- Invalid weights fail at construction rather than producing a plausible-looking preview.
- The proposal record keeps the weights and preview *as they were*, so a decision is judged against
  what the decider saw.

## Known limits

- **The formula cannot be changed without a deploy.** That is deliberate, but it means the workflow
  ends in a code change rather than in the platform. A community cannot self-serve a scoring change.
- **The preview reorders only, it does not re-score.** It does not recompute the stored
  `priorityScore` values, so it cannot show what the numbers would become, only the order.
- **No vote or multi-party approval.** One `SUPER_ADMIN` decides. Community ratification is a
  different mechanism and is not built.
- **No notification.** A proposed change is invisible until someone lists the proposals.
- Closed signals are excluded from the sample, so the preview covers the open backlog only.
- **A tie-driven movement is reported as a movement.** The interpretation says ties are arbitrary,
  but the `positionsMoved` count includes them, so the headline number can overstate real change. A
  tie-aware count would be more precise and is not implemented.
- The `limit` bounds the scan at 2000 signals, so a very large backlog is previewed from its top
  2000 by current score.

## Prior work this depends on

This was possible only after the weights were consolidated into one definition. While the published
formula could silently differ from the running one, a change preview would have been measured
against a baseline nobody could trust.