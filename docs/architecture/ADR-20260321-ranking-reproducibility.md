# ADR-20260321: Ranking Reproducibility Is Observed, Not Reimplemented

- **Status:** Accepted
- **Date:** 2026-03-21
- **Decides:** GitHub #44
- **Script:** `scripts/check-ranking-reproducibility.mjs` (`npm run ranking:check`)

## Context

The issue asked for a reproducibility script for ranking outputs. There already was one:
`src/prioritize.mjs`, and it produced `examples/prioritized-backlog.json`.

Reading it was the finding. It contained a hand-written copy of the scoring formula:

```js
urgency: signal.urgency * 30,
impact: signal.impact * 25,
affectedPeople: Math.min(signal.affectedPeople / 10, 30),
communityVotes: Math.min(signal.communityVotes / 5, 15)
```

which is `PrioritizationServiceImpl.getBreakdown` transcribed into JavaScript.

## Decision

### The copy is deleted, and that is the actual fix

Three reasons, in order of how much damage each one does:

1. **It violated the stated architecture.** AGENTS.md says the backend owns domain
   calculations and no other layer implements ranking independently. This file was a second set
   of weights that nobody reviews in the same place as the first.

2. **A copy cannot detect that it has drifted.** The real formula is versioned
   (`PrioritizationFormulaService.VERSION`) and published at `GET /api/signals/formula`. This file
   had no way to learn the version. It would have kept cheerfully agreeing with a stale formula
   long after the backend moved on, and the "verification" would have kept passing. That is the
   worst shape for an audit tool: it looks like a check while testing nothing.

3. **It sorted by score with no tiebreaker.** Equal scores fell to input order, which is neither
   the backend's order nor guaranteed stable across a differently ordered input file. A ranking
   whose ties depend on input order is not a ranking anyone can cite a position from.

`src/prioritize.mjs` now only validates the shape of the example dataset: unique ids, required
fields, numeric inputs non-negative. That check is genuinely useful offline and invents no
scores. `examples/prioritized-backlog.json` is deleted, because its existence implied it was the
platform's ranking.

### Reproducibility means the backend agrees with itself

The new script does not rank anything. It:

1. Reads `GET /api/signals/formula` and refuses to continue without a `version`, so any claim it
   makes is anchored to a formula.
2. Calls `GET /api/signals/prioritized` **twice** with identical parameters.
3. Hashes the ordering (id and score only) and compares.
4. Writes a capture recording the formula version, the expression, the filters, and the ordered
   entries, so a later run is a reviewable diff.

Divergence exits 1 and prints the first differing position with both sides. It also says plainly:
*do not cite a rank position from this endpoint*.

### Could-not-check is not the same as passing

Three exit codes: `0` reproducible, `1` not reproducible, `2` could not check.

The distinction matters more than it looks. An unreachable API or a formula with no version is
not evidence of reproducibility, and a script that returned 0 in those cases would be a green
light for an unverified claim. Exit 2 exists so "we could not verify" is visible in CI rather
than silently reading as success.

An empty ranking is also exit 2. An empty list is trivially identical across two calls and would
"pass" while proving nothing.

### The hash covers position, not prose

Only `id` and `priorityScore` are hashed. Title and category are excluded deliberately: editing a
title should not make a ranking look unreproducible. The question is whether the *order held*,
not whether someone corrected a typo.

### Verification does not require a live backend

`--input <capture.json>` re-verifies a saved capture against its own recorded hash, so CI and a
reviewer can confirm an artifact was not edited after it was written without standing up a
database. This is the same guarantee the explainability snapshots provide, for a much smaller
artifact.

## Consequences

- One implementation of the ranking formula, in the place that owns it.
- The reproducibility claim is anchored to a formula version rather than to a copy.
- "Could not verify" is distinguishable from "verified" in CI.
- An empty or unreachable backend cannot produce a pass.
- Captures are diffable artifacts, so a ranking that moves is a reviewable change.
- `npm run prioritize` no longer produces a backlog, so any workflow that expected that file has
  to call the backend. This is the intended breakage.

## Known limits

- Two calls detect nondeterminism, not prove determinism. A ranking that varies once an hour would
  pass. Catching that needs many samples over time, which is a monitoring concern rather than a
  gate.
- No tiebreaker is asserted. The script checks that the backend is *consistent with itself*; it
  does not decide what the correct order among equal scores should be. That remains a product
  decision, and the surge and snapshot ADRs sort by score then id as an implementation choice.
- Not wired into `agent:preflight`, because preflight must pass without a running backend. It is a
  release-time and manual check. Making it part of the gate would require the API in every CI run,
  which is a bigger change than this issue.
- Captures accumulate in `docs/reproducibility` and are not pruned.
- The hash is not an external anchor. If someone edits a capture and its recorded hash together,
  `--input` verification passes. Same limitation as the explainability snapshots, and stated there
  too.
