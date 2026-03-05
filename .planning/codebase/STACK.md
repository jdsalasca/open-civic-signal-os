# Technology Stack

**Analysis Date:** 2026-03-04

## Languages

**Primary:**
- Java 21 - Backend API, domain logic, persistence (`apps/api-java/src/main/java`).
- TypeScript - Frontend app (`apps/web-react/src`).

**Secondary:**
- JavaScript (ESM) - Repo scripts and quality gates (`scripts/*.mjs`, `src/*.mjs`).
- YAML/TOML/Markdown - CI workflows, contracts, governance and planning docs.

## Runtime

**Environment:**
- Node.js v25.5.0 (detected locally) for workspace scripts and frontend tooling.
- Java runtime (JDK 21 expected by workflows) for backend build/test.
- PostgreSQL 15 (containerized for local/compose runtime).

**Package Manager:**
- npm (workspace root) with `package-lock.json`.
- Maven for Java backend (`apps/api-java/pom.xml` + `mvnw` usage).

## Frameworks

**Core:**
- Spring Boot (Java backend API).
- React 18 + Vite 5 (frontend SPA).

**Testing:**
- JUnit/Spring Boot integration tests (`apps/api-java/src/test`).
- Playwright specs (`apps/web-react/src/tests/*.spec.ts`).

**Build/Dev:**
- TypeScript compiler + Vite bundling in web package.
- Node scripts for preflight/agent gates/release checks.
- Docker Compose for prod-like/dev local orchestration (`infra/`).

## Key Dependencies

**Critical (frontend):**
- `react`, `react-router-dom`, `zustand` - app state and routing.
- `axios` - API client with auth refresh flow.
- `primereact`/`primeflex` - UI components/layout system.
- `react-hook-form` - form state handling.
- `react-hot-toast` - user feedback notifications.

**Critical (backend):**
- Spring Security + JWT components for auth/session model.
- Spring Data JPA + Flyway for DB persistence and migrations.
- Micrometer/Actuator for metrics and health endpoints.

## Configuration

**Environment:**
- Backend envs in `apps/api-java/src/main/resources/application.yml` and compose env overrides.
- Frontend API target through `VITE_API_BASE_URL`.
- JWT secret required in compose (`infra/docker-compose.yml`).

**Build/Quality:**
- Root scripts in `package.json`.
- CI workflows in `.github/workflows/`.
- Contract file at `packages/contracts/openapi.yaml`.

## Platform Requirements

**Development:**
- Node + npm, Java/Maven, optional Docker.
- Windows/Linux/macOS contributors supported by repo docs and workflows.

**Production/Deployment:**
- Docker-based deployment baseline (`infra/docker-compose.yml`).
- API/Web image pipelines via `.github/workflows/docker-images.yml`.

---
*Stack analysis: 2026-03-04*
*Update after major dependency/runtime changes*
