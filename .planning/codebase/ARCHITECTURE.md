# Architecture

**Analysis Date:** 2026-03-04

## Pattern Overview

**Overall:** Full-stack monorepo with backend-owned domain logic and contract-mediated frontend integration.

**Key Characteristics:**
- Backend computes civic prioritization and explainability.
- Frontend focuses on workflows, rendering, and user feedback.
- Governance/docs/quality automation are first-class repository concerns.

## Layers

**Presentation Layer (Frontend React):**
- Purpose: user-facing civic workflows (dashboard, report, detail, communities, threads, blog, feed).
- Contains: views, UI primitives, stores, i18n, API client.
- Depends on: API contracts and backend response shape.
- Used by: citizens, moderators, public servants through browser UI.

**API Layer (Spring Controllers):**
- Purpose: expose authenticated/role-scoped HTTP endpoints.
- Contains: controllers in `apps/api-java/src/main/java/.../web`.
- Depends on: services and security context.
- Used by: frontend and potential external consumers.

**Domain Service Layer (Backend):**
- Purpose: deterministic scoring, moderation, collaboration, auth, notifications.
- Contains: services under `.../service`.
- Depends on: repositories and domain entities.
- Used by: controller layer.

**Persistence Layer (JPA + Flyway):**
- Purpose: store entities/audit history and evolve schema safely.
- Contains: domain entities + repositories + migration scripts.
- Depends on: PostgreSQL runtime.
- Used by: service layer.

## Data Flow

**Signal Prioritization Flow:**
1. User submits/updates/votes on civic signal via frontend.
2. API validates auth/community scope and delegates to service.
3. Prioritization service computes score + breakdown in backend.
4. Response DTO is returned to UI for rendering explainability.
5. Audit/status history can be queried from dedicated endpoints.

**Community Collaboration Flow:**
1. UI requests threads/blog/feed scoped by active community.
2. API enforces membership and role constraints.
3. Service layer retrieves paged data and engagement state.
4. UI preserves filter/paging state in community store.

**State Management:**
- Backend: persistent relational state + computed DTOs.
- Frontend: local persisted auth/community/settings via Zustand.

## Key Abstractions

**Signal + ScoreBreakdown:**
- Purpose: represent civic issue priority with explainability factors.
- Examples: `Signal`, `ScoreBreakdown`, `SignalResponse`.
- Pattern: backend domain object + DTO mapping.

**Community Context:**
- Purpose: scope reads/writes by active membership.
- Examples: `X-Community-Id`, `CommunityAccessService`, frontend `useCommunityStore`.
- Pattern: explicit context propagation from UI -> API.

**Trust/Quality Gates:**
- Purpose: keep changes auditable and reproducible.
- Examples: `agent-preflight`, `agent-context-check`, release/ADR checks, UX evidence gate.

## Entry Points

**Frontend:**
- `apps/web-react/src/main.tsx` and routed app in `apps/web-react/src/App.tsx`.

**Backend:**
- `apps/api-java/src/main/java/org/opencivic/signalos/OpenCivicSignalOsApplication.java`.
- Primary controller entrypoints include `SignalController`, `AuthController`, `CommunityCollaborationController`.

**Automation/CI:**
- Root `package.json` scripts and `.github/workflows/*.yml`.

## Error Handling

**Strategy:** Centralized API exception handling + explicit frontend friendly messages.

**Patterns:**
- Backend `GlobalExceptionHandler` standardizes API errors.
- Frontend axios interceptor maps backend/network failures to user-facing messages.
- Some trust-critical paths still require hardening for anonymous/public route behavior.

## Cross-Cutting Concerns

**Security:** Spring Security RBAC + JWT + refresh cookie strategy.  
**Observability:** Actuator + metrics counters/timers for key endpoints.  
**Accessibility/UX Standards:** codified in AGENTS and agent docs; validated via Playwright evidence gates.  
**Governance:** backlog, decisions, and mandatory context docs define operational source of truth.

---
*Architecture analysis: 2026-03-04*
*Update when major patterns/layer boundaries change*
