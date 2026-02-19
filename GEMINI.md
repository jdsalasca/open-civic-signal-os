# GEMINI.md - Open Civic Signal OS Context

## Project Overview
**Open Civic Signal OS** is a monorepo civic-tech platform designed to transform community signals into transparent, auditable, and actionable public priorities.

- **Backend:** Java 21 + Spring Boot (located in `apps/api-java`).
- **Frontend:** React + TypeScript + Vite (located in `apps/web-react`).
- **API Contracts:** OpenAPI 3.0 (located in `packages/contracts/openapi.yaml`).
- **Core Logic:** Deterministic prioritization based on urgency, impact, affected population, and community votes.

## Building and Running

### Root Environment
- `npm install`: Install workspace dependencies.
- `npm run prioritize`: Runs the MVP prioritization script using `examples/feedback.json`.
- `npm run agent:gate`: Runs the agent quality gate script.

### Backend (apps/api-java)
- `./mvnw clean install`: Build the Java application.
- `./mvnw spring-boot:run`: Start the API server locally.
- **Key Files:** 
  - `SignalController.java`: API endpoints.
  - `PrioritizationService.java`: Core ranking logic.

### Frontend (apps/web-react)
- `npm run dev:web`: Start the React dev server from the root.
- `npm run build:web`: Build the production bundle.
- **Key Files:**
  - `src/App.tsx`: Main dashboard entry point.

## Development Conventions

### 1. Prioritization Logic
- **Constraint:** Prioritization logic **must** reside in the backend. The frontend should only consume and display ranked data.
- **Formula:** Score = (Urgency * 30) + (Impact * 25) + min(People/10, 30) + min(Votes/5, 15).

### 2. Contract-First Development
- All API changes **must** start with an update to `packages/contracts/openapi.yaml`.
- Ensure backend and frontend stay synchronized with the contract.

### 3. Engineering Standards (from AGENTS.md)
- **Determinism:** Scoring must be reproducible and explainable.
- **Privacy:** Minimize PII capture; prefer anonymized data for public dashboards.
- **Immutability:** Never mutate source records during scoring.

### 4. Contribution Workflow
- Follow the protocols in `GEMINI_INSTRUCTIONS.md`.
- Use the branch naming convention: `feat/<issue-number>-<short-slug>`.
- Every PR requires a structured summary covering Scope, API changes, Contract changes, and Verification.

## Quality Gates
- Backend tests must run (no test skipping): `mvn -q test`.
- Frontend must build: `npm run build:web`.
- MVP script must work: `npm run prioritize`.
- Documentation must be updated for any behavior change.

## Anti-Regression Rules for Gemini

- Never commit generated artifacts (`dist`, `test-results`, screenshots, temp logs).
- Never add dependencies for convenience; only add when issue scope requires and justify in PR.
- Never change unrelated layers in same PR (for example, no large CSS redesign in backend issue).
- Never ship malformed markdown or JSON (check for escaped newline fragments and JSON parse errors).
- Never weaken checks (`skipTests`, bypass CI, incomplete verification evidence).
