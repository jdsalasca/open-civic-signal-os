# Test Strategy

## Pyramid

- Unit tests for core rules/calculations.
- Integration tests for API contract behavior.
- UI tests for critical user journeys.

## Minimum checks per feature

- Happy path test.
- Invalid input test.
- One edge case test.

## Regression discipline

- Keep golden sample datasets for deterministic checks.
- Add regression tests for every bug fix.
- Avoid merging features without executable verification.

## Root Node test coverage

- `npm test` runs every root Node test (`test/*.test.mjs`).
- `npm run coverage:summary` regenerates `coverage/coverage-summary.json` with c8
  (`--all` over `src/**` and `scripts/**`, so untested files count as 0%), normalizes file
  paths to be repository-relative, and must be committed with the change.
- `npm run quality:release` (release gate) reads `total` from that summary and enforces
  `releaseQuality.minimumCoverage` (package.json). Never lower it to make a red gate green;
  add the smallest test that covers the missing lines instead.
- Refresh the summary whenever root tests or `src/`/`scripts/` files change:

  ```sh
  npm test                 # fail fast on a red suite
  npm run coverage:summary # rewrites coverage/coverage-summary.json
  npm run quality:release  # verifies the committed summary passes the gate
  ```
