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
