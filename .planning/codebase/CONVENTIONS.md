# Coding Conventions

**Analysis Date:** 2026-03-04

## Naming Patterns

**Files:**
- Frontend components/views use `PascalCase.tsx` (e.g., `CommunityThreads.tsx`).
- Store/hooks use `useXStore.ts` pattern.
- Backend classes follow standard Java `PascalCase` naming by role (`*Controller`, `*Service`, `*Repository`).

**Functions:**
- TypeScript uses camelCase handlers (`handleLogout`, `loadThreads`).
- Java service/controller methods use camelCase with explicit domain verbs.

**Variables and Constants:**
- TS variables camelCase, constants uppercase snake or semantic const names.
- Java fields camelCase, constants upper snake case where present.

**Types:**
- Frontend interfaces/types in `types.ts` with PascalCase.
- Backend DTO records/classes in `web/dto`.

## Code Style

**Formatting/Linting:**
- Frontend includes ESLint + Prettier scripts (`apps/web-react/package.json`).
- TypeScript strictness enforced through `tsc -b` in build.
- Backend style inferred from Spring conventions; tests and services are explicit and deterministic-focused.

## Import Organization

- Frontend generally orders external imports first, then local modules.
- Backend follows Java package imports grouped by JDK, third-party, then project classes.
- Path aliasing is minimal; relative imports dominate in frontend.

## Error Handling

**Backend:**
- Global exception normalization via `GlobalExceptionHandler`.
- Domain-specific exceptions (`ConflictException`, `ResourceNotFoundException`, etc.).
- Fail-fast behavior used in validation-heavy paths.

**Frontend:**
- Axios interceptor attaches `friendlyMessage` based on backend payload/status.
- Views surface failures through toast messaging and explicit UI states.

## Logging

- Backend uses SLF4J logger for service-level events and moderation/merge operations.
- Frontend uses `console.warn` sparsely around recoverable API failures.
- Operational metrics tracked through Micrometer in prioritized endpoints.

## Comments

- Inline comments used for policy markers and hardening notes (`P0`, `UX-*` style tags).
- Repo rules prefer concise comments that explain intent, not obvious mechanics.

## Function and Module Design

- Frontend favors composable UI primitives (`Civic*`) to reduce duplication.
- Backend keeps domain calculations in services and maps entities -> DTOs in controllers.
- Stores encapsulate persistence/hydration and expose explicit mutation functions.

## Repository-Level Engineering Conventions

- One objective per PR, contract alignment required for API shape changes.
- No generated artifacts in commits (`dist`, `test-results`, screenshots unless requested).
- Frontend-impacting changes require Playwright UX evidence in PR.
- Backend and trust-critical changes require deterministic tests and reproducibility evidence.

---
*Convention analysis: 2026-03-04*
*Update when lint/style/policy patterns change*
