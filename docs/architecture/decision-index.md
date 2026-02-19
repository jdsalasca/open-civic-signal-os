# Decision Index

Track product and technical decisions that affect behavior, contracts, or policy.

## Required ADR Triggers

Create/update an ADR when any of the following changes:

- API contract (`packages/contracts/openapi.yaml`)
- Domain invariants or policy logic
- Security/auth model
- Data model with migration implications
- Any behavior change requiring migration notes

## ADR List

- Add ADR files here using `ADR-YYYYMMDD-<slug>.md`.
- Keep status updated (`proposed`, `accepted`, `superseded`).

## Review Rule

If contract changes are present in a PR, at least one ADR file must be touched.
