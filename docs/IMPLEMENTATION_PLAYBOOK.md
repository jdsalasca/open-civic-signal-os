# Implementation Playbook

## 1. Intake

- Choose one `agent-ready` issue in current milestone.
- Confirm acceptance criteria are testable.
- If criteria are ambiguous, clarify in issue comments first.

## 2. Design

- Define endpoint contract and DTOs.
- Decide where domain rule belongs in backend service.
- Plan frontend states: loading, success, empty, error.

## 3. Build

- Implement backend feature behind clear service boundaries.
- Update `packages/contracts/openapi.yaml`.
- Connect frontend to real endpoint.
- Update examples and docs.

## 4. Verify

Run:

```bash
npm run prioritize
npm run build:web
```

Run Java build when local JDK is available:

```bash
cd apps/api-java
mvn -q -DskipTests package
```

## 5. Ship

- Keep PR focused on one issue objective.
- Add screenshots/GIFs for dashboard changes.
- Link issue and milestone.

## 6. Post-merge

- Move status label in issue.
- Confirm `docs/kanban.md` auto-refresh workflow completed.
