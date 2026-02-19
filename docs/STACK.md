# Stack Decision

## Chosen stack

- Backend: Java 21 + Spring Boot
- Frontend: React + TypeScript + Vite

## Why this stack

- Strong backend reliability for civic workflows and governance logic.
- Fast UI iteration for public dashboards and operator panels.
- Clear separation of concerns for agent-based implementation.

## Rules

- Business rules must be in backend services.
- Frontend must only orchestrate and visualize validated data.
- API contracts versioned in `packages/contracts`.
