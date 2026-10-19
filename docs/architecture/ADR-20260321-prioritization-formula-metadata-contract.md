# ADR-20260321: Published Prioritization Formula Metadata Contract

- Status: Accepted
- Date: 2026-03-21
- Closes: GitHub `#26`, `#47`
- Story: `OCS-P1-003`

## Context

The platform ranked signals with a fixed weighting, but published none of it. Two places described it and neither was the source of truth for the other: `TrustPacket.CURRENT_FORMULA` was a hand-written string, and `PrioritizationServiceImpl.getBreakdown` was the code that actually computed the score. Nothing checked that they agreed.

Worse, the frontend "Why Ranked Here" panel had its own fourth copy as hardcoded i18n strings. A weight change in the backend would leave the user-facing explanation silently wrong, which is worse than not publishing a formula at all for a system whose premise is explainability.

## Decision

Publish the formula as data, from one place, and read it everywhere:

- `GET /api/signals/formula` returns `PrioritizationFormulaResponse`: `version`, `formula`, `effectiveFrom`, `weights[]` (each with `factor`, `input`, `expression`, `cap`), `cappedFactors[]`, and a plain-language `changeNote`.
- `PrioritizationFormulaService` is the single published source. `version` is asserted to equal `PrioritizationServiceImpl.TRANSFORMATION_VERSION`, so the formula version and the version stamped on every signal cannot drift apart silently.
- The endpoint is public, alongside `/prioritized`, `/top-10`, and `/meta`. A formula you must log in to inspect is not transparent.
- `SignalDetail` now renders the weights it fetches from this endpoint. The four hardcoded formula strings in `i18n.ts` are deleted rather than left as a fallback, because a stale fallback is exactly the failure mode this story exists to remove.

## Consequences

Positive:

- the user-visible ranking explanation is now generated from the same data the backend publishes, so it cannot silently disagree with the scoring code
- each factor states the raw field it reads and its exact arithmetic, including where it saturates
- a formula change is a version bump plus a change note, which is reviewable in a PR

Trade-offs:

- **The published weights are still mirrored, not derived.** `PrioritizationFormulaService` hardcodes the same numbers that `getBreakdown` applies. The honest fix is to make the service the source and have `getBreakdown` read the weights from it, which is a larger refactor than this story should carry. The mirror is marked with a `ponytail:` comment naming that upgrade path.
- `PrioritizationFormulaIT` narrows the gap by asserting the published caps match the computed contributions at saturating inputs, and that the versions agree. That is a guardrail, not proof; a weight that changed in one place but stayed in-range would not be caught.
- the formula is global. There is no per-community or per-category override, and none is planned; adding one would make the public formula a partial truth.
- no formula history is stored. `version` and `effectiveFrom` describe the current rule; the previous weights are not queryable.

## Validation

- `PrioritizationFormulaIT` covers the public response shape, that the published caps equal the computed contributions for saturating inputs (`min(3000/10, 30) = 30`, `min(75/5, 15) = 15`), and that `PrioritizationServiceImpl.TRANSFORMATION_VERSION` equals the published `VERSION`
- OpenAPI updated with `PrioritizationFormula` and `FormulaWeight` schemas and a `SignalSourceChannel`-adjacent audit surface
- `signal-provenance.spec.ts` asserts the panel renders the backend expressions (`urgency * 30`, `min(affectedPeople / 10, 30)`) rather than fixed copy
- five now-dead i18n keys removed: `why_ranked_desc`, `urgency_factor`, `affected_formula`, `votes_formula` in both locales