# Definition of Done

A roadmap issue is Done only if all checks pass.

## Backend

- Endpoint/service logic implemented in Java.
- Validation and error handling included.
- No business rule duplicated only in frontend.

## Contracts

- OpenAPI updated for any API change.
- Field names and types aligned with actual responses.

## Frontend

- Connected to real endpoint or documented fallback.
- Loading, error, and empty states implemented.
- UI labels clear for public/community users.

## Docs

- README/docs updated for behavior changes.
- Any new command documented.

## Verification

- `npm run prioritize` passes.
- `npm run build:web` passes.
- Java package compiles or blocker explicitly documented.

## Governance

- Issue references PR and vice versa.
- Milestone and priority labels kept consistent.
