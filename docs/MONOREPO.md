# Monorepo Guide

## Structure

- `apps/api-java` backend
- `apps/web-react` frontend
- `packages/contracts` shared API contracts
- `infra` local runtime and deployment assets
- `docs` strategy and architecture

## Working model for agents

1. Pick one issue with `agent-ready`.
2. Implement backend first if domain rule changes.
3. Update contract if endpoint shape changes.
4. Implement frontend view/state.
5. Update docs and example payloads.
