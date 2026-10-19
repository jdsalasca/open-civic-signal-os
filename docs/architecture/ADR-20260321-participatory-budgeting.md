# ADR-20260321: Participatory Budgeting

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #32
- **Contract:** `packages/contracts/openapi.yaml` (`/api/community/participatory-budgets`)

## Context

The platform does not hold money, and this does not change that. What it can do is record what a
community decided to allocate and report whether the arithmetic closes, which is a different and
more useful thing than a payment system.

The hard part is the input. `CommunityProposal.estimatedCost` is **free text**: "USD 4000" and
"about 3k" are both plausible things a resident writes, and only one of them is an amount.

## Decision

### An unreadable cost is reported, never guessed

`CostTextParser` reads a currency marker and a number. Anything else — "about 3k", "TBD",
"1000-2000", "USD 4000 for materials" — returns empty, and the allocation records `amountMinor: null`
with the raw text preserved.

This is the decision the ADR exists for. A budget that silently invented a number would produce an
allocation that looks decided and is not, and the failure would surface as a wrong total in a
meeting rather than as an error.

The parser is deliberately narrow. It reads:

- `USD 4000`, `4000 USD`, `$4000`, `€1200`, `£500`
- `4000` bare, read as the envelope's currency
- `1.200,50` and `1,200.50`, resolved by the rule that the last separator is decimal when followed
  by one or two digits

It refuses everything else. Pinned by 13 parser tests, including `shouldRefuseAnApproximationRatherThanGuess`
and `shouldRefuseARange`.

### Money is minor units in a long

`totalBudgetMinor` and `amountMinor` are `long`. A double would round, and the rounding would surface
in a council meeting rather than in a test.

Formatting respects the currency's own fraction digits, so `JPY 4000` is 4000 minor units rather
than 400000. Pinned by `shouldRespectTheCurrencysOwnFractionDigits`.

### An unreadable cost makes "it fits" unknown, not confirmed

This is the second decision worth stating. A selection that looks within budget while three
proposals have no readable amount is **not within budget, it is unknown**.

`unreadableCosts` is reported, the interpretation says the selection is "not fully costed" and that
whether it fits is "unknown rather than confirmed", and each unreadable allocation shows the text the
parser was given. Pinned by `anUnreadableCostShouldMakeTheSelectionUnknownRatherThanConfirmed`.

Without that, a community would read a green "within budget" and fund a set it cannot actually cost.

### The simulation does not choose

It reports what a selection costs and whether the envelope covers it. **Which proposals to fund is
the community's decision**, and the platform has no standing to make it. The interpretation says so
on every response.

This is the same principle as the policy sandbox: a tool that returned a winner would be making a
value judgement silently.

### Costs are read once, at envelope creation

Every proposal gets an allocation row with its cost parsed at that moment. Reading lazily per request
would let a proposal edited later change a past simulation, which is the non-determinism the digest
and transparency report both avoid.

### Selection replaces rather than toggles

`POST /{id}/selection` takes the whole list. Toggling one at a time would let a caller end up with a
half-applied change if something failed partway.

### One allocation per proposal per envelope

A unique index on `(budget_id, proposal_id)`. A proposal counted twice would spend the same money
twice.

## Consequences

- A community can see whether a proposed allocation fits, with the arithmetic visible.
- An unreadable cost is a named finding rather than a silent omission.
- Money cannot round.
- The platform does not pick winners.
- A past simulation cannot be changed by editing a proposal afterwards.

## Known limits

- **No UI.** The endpoints exist and are tested; there is no budgeting screen. That is the remaining
  half of this issue.
- **The parser is a heuristic.** `1.200` is read as 1200, which is right in most of Europe and wrong
  if the author meant 1.2. The parser refuses anything ambiguous, but this specific case is not
  ambiguous to it and could be wrong to a reader.
- **No multi-currency envelopes.** One currency per envelope, and a proposal whose text names a
  different one is read at face value rather than converted. There is no exchange rate and inventing
  one would be worse.
- **No voting integration.** The envelope records a selection; it does not tally votes or derive the
  selection from them. Linking the two is a separate decision about what a vote means for a budget.
- **No phases or deadlines.** An envelope has no open or close date in the API, though the column
  exists.
- **No cost editing.** A proposal's cost is read once at envelope creation. Correcting a typo means
  creating a new envelope, which is deliberate but inconvenient.
- **No partial funding.** A proposal is in or out; there is no "fund 60% of this".
- **`MAX_ALLOCATIONS` is 500.** A community with more proposals must split the exercise.