# Domain Decision Pack

This pack ensures domain and contract decisions remain explicit and traceable.

## Components

- `docs/architecture/decision-index.md`
- `docs/architecture/ADR_FUNCTIONAL_TEMPLATE.md`
- `.github/ISSUE_TEMPLATE/decision-request.yml`
- `scripts/check-contract-adr.mjs`
- `.github/workflows/contract-adr-gate.yml`

## Rule

Any PR changing `packages/contracts/openapi.yaml` must include ADR updates.
