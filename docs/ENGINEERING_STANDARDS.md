# Engineering Standards

## Source of truth

- Backend domain rules are canonical.
- OpenAPI contracts are canonical for API shape.
- Frontend mirrors backend behavior, never replaces it.

## Code quality

- Keep functions small and explicit.
- Prefer descriptive names over short names.
- Avoid hidden side effects.
- Keep data transformations pure where possible.

## Change discipline

- One issue objective per PR.
- Contracts updated with API changes.
- Docs and examples updated with behavior changes.
- No dead code or placeholder TODOs in core paths.

## Reliability

- Fail fast on invalid input.
- Produce actionable error messages.
- Keep deterministic outputs for repeatable runs.
