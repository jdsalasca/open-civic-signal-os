# Task Pack: Backend

## Inputs

- Story ID
- Domain/API acceptance criteria
- Contract impact expectation

## Implement

1. Apply deterministic backend behavior change.
2. Keep response/error contracts explicit.
3. Update contract/ADR when applicable.

## Verify

1. `mvn -q test` (in `apps/api-java`)
2. `npm run agent:adr:check` (if contract touched)
3. `npm run agent:context:check`

## Output

- Code diff
- Validation evidence
- Migration impact and rollback
