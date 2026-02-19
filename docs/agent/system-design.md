# System Design

## Architecture

```mermaid
flowchart TD
  A[React Dashboard] --> B[Java API]
  B --> C[Ingest Service]
  B --> D[Scoring Service]
  B --> E[Moderation Service]
  B --> F[Backlog Publish Service]
  F --> G[Public JSON/Exports]
```

## Contract Boundaries

- Backend owns canonical ranking logic.
- Frontend only renders ranking and explanations.
- OpenAPI is source of truth for all external consumers.
- Audit metadata must persist end-to-end.

## Non-Functional Targets

- Deterministic ranking across reruns.
- Explainable output for every ranked item.
- Low-bandwidth friendly dashboard rendering.
- PII minimization at ingest.
