# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-04)

**Core value:** Community priorities must be visible, explainable, and reproducible end-to-end.  
**Current focus:** Phase 5 - Community Operating System Expansion

## Current Position

Phase: 5 of 5 (Community Operating System Expansion)  
Current execution plan: 05-30 (Privacy Center and Sensitive-Data Access Logs)
Tracked phase subplans: 31
Tracked project plans: 44
Status: In progress  
Last activity: 2026-03-09 - Completed `05-29` for `OCS-P1-049` with backend moderation reports/sanctions, moderator queue UX, contract/ADR updates, Docker validation, and backlog promotion to `OCS-P1-050`

Progress: 4 of 5 roadmap phases complete; Phase 5 foundations shipped and next target is `OCS-P1-050`

## Performance Metrics

**Execution Snapshot:**
- Total tracked plans across project: 44
- Tracked Phase 5 subplans: 31
- Shipped Phase 5 stories: 18 (`OCS-P1-029`, `030`, `031`, `032`, `033`, `034`, `036`, `037`, `038`, `039`, `040`, `041`, `042`, `043`, `046`, `047`, `048`, `049`)
- Immediate queue: `OCS-P1-050`, `OCS-P1-051`, `OCS-P1-052`, `OCS-P1-035`, `OCS-P1-044`

## Accumulated Context

### Decisions

- 2026-03-04: Adopted GSD `.planning` baseline to anchor future phase planning and execution.
- 2026-03-04: Prioritized trust/contract stabilization before new scope expansion.
- 2026-03-04: Planned all 9 roadmap plans with explicit wave/dependency structure.
- 2026-03-04: Added deterministic ingest path for reproducible replay and deduplicated merges.
- 2026-03-04: Added backend + frontend degraded auth resend coverage to protect onboarding trust path.
- 2026-03-04: Refactored thread retrieval to batch message/reaction loading and reduce avoidable fan-out queries.
- 2026-03-04: Hardened community store validation so membership refresh preserves valid active community context.
- 2026-03-04: Reduced community blog engagement fan-out and added request-level Playwright regression evidence.
- 2026-03-04: Expanded frontend audience/usability backlog with story-level API/contract/UI mapping and measurable outcomes.
- 2026-03-04: Added explicit onboarding step progress cues on register/verify with EN/ES copy and Playwright coverage.
- 2026-03-04: Reworked dev Docker runtime with retry entrypoints, persistent caches, and reliable hot reload defaults.
- 2026-03-05: Added list-level explainability summaries so users understand ranking before opening detail pages.
- 2026-03-05: Surfaced backend permission reasons inline in blog/thread creation instead of opaque 403 failures.
- 2026-03-05: Fixed Windows Playwright screenshot output path behavior to match documented agent workflow.
- 2026-03-05: Strengthened skip-link keyboard behavior so focus moves into main content instead of trapping at page chrome.
- 2026-03-05: Reframed phase 4 priorities toward product clarity, reduced choice overload, and clearer user-facing value communication.
- 2026-03-05: Improved empty/restricted states with action-oriented recovery paths and pushed the change to develop.
- 2026-03-05: Opened Phase 5 for identity, communication, deliberation, execution, governance, and transparency expansion.
- 2026-03-05: Implemented OCS-P1-029 with persisted civic profiles, audience-based visibility filtering, Settings UX, contract, and ADR updates.
- 2026-03-05: Implemented OCS-P1-030 with nested community hierarchy, breadcrumb contract support, seeded child spaces, and hierarchy-aware Communities UX.
- 2026-03-05: Implemented OCS-P1-031 with backend-owned community permission scopes, structured 403 permission responses, policy overrides, and coordinator-facing policy matrix UX.
- 2026-03-05: Implemented OCS-P1-033 with pinned official announcements, archive search/mutation support, and a more legible official updates channel.
- 2026-03-05: Established the visual core refresh foundation for OCS-P1-032 with a new community-first shell, shared design primitives, typographic identity, and a redesigned dashboard hero validated in Docker.
- 2026-03-05: Extended OCS-P1-032 with persisted `interfaceMode` profile preference, simple/advanced settings controls, backend contract storage, and shell/dashboard behavior changes.
- 2026-03-05: Refocused identity UX around communities by exposing membership count, active community path, visible role/rank, and direct community actions inside Settings and shell chrome.
- 2026-03-05: Extended OCS-P1-032 into a community-first home by surfacing active-community context, community actions, and calmer simple-mode guidance directly in the dashboard and shell.
- 2026-03-05: Implemented OCS-P1-034 with backend-owned thread relevance ordering, reply-depth enforcement, relevance metadata in thread payloads, and improved thread sorting UX.
- 2026-03-05: Implemented OCS-P1-036 with a backend-owned community-home composite response and a unified feed route that now foregrounds official updates, hot discussions, and top open issues.
- 2026-03-05: Implemented OCS-P1-037 with an issue tracker-grade reporting wizard, structured location/evidence fields, signal detail evidence rendering, migration, and contract/ADR updates.
- 2026-03-05: Implemented OCS-P1-038 with backend-owned signal assignment, typed timeline events, audited lifecycle actor/reason fields, and a stronger case detail responsibility surface.
- 2026-03-05: Implemented OCS-P1-039 with a dedicated community map, cross-community heat view, deterministic hotspot payloads, and a geospatial route that keeps filter semantics backend-owned.
- 2026-03-05: Hardened shared UI consistency by localizing page-header chrome, improving empty-state resilience, wrapping chip text safely, and converting the map from click-only bubbles into explicit selected-detail panels.
- 2026-03-05: Standardized `CivicCard` header/actions and adopted the new reusable card-surface utilities across community blog, community threads, and signal detail to reduce layout drift and long-copy overlap.
- 2026-03-05: Extended civic identity profiles with persisted avatar presets, backend-owned achievement milestones, tag-based affiliation editing, and a stronger recognition-oriented Settings experience.
- 2026-03-05: Introduced reusable `CivicStatCard` and migrated the highest-risk summary grids in Settings, Dashboard, and CommunityFeed to shared responsive tokens instead of local metric blocks.
- 2026-03-05: Finished the cross-app stat-grid hardening pass by moving Communities and SignalDetail onto shared summary primitives, localizing remaining signal-detail chrome, and improving global badge/button wrapping for ES-heavy layouts.
- 2026-03-05: Reduced Settings card density by separating identity, community belonging, and achievement surfaces, shortened community summary labels, and hardened stat-card/meta wrapping so Spanish copy stays stable under compact layouts.
- 2026-03-05: Reduced dashboard and community-home control density by grouping secondary actions, introducing calmer action-bar layouts, and standardizing reusable list-card surfaces for official updates, threads, and top issues.
- 2026-03-05: Added a global overflow-hardening pass for grid columns, card copy, badges, pills, PrimeReact button labels, dropdown labels, and datatable cells so longer strings wrap instead of overlapping surfaces across the app.
- 2026-03-05: Hardened light-theme contrast and responsive copy handling across shared selects, stat cards, mobile nav, topbar community context, Settings identity density, report summaries, and signal detail lifecycle controls to remove remaining cross-app breakpoints.
- 2026-03-05: Standardized shared typography and note/list surfaces across page headers, meta rows, dashboard signal tables, digest/alert sidebars, community blog, and threads so repeated content types now use one visual language instead of view-specific markup.
- 2026-03-05: Extended the responsive hardening pass into MetricsGrid, Communities, CommunityMap, and Moderation so the remaining dense community hubs now share the same card, heading, and metadata standards with better wrapping under narrow layouts.
- 2026-03-05: Implemented OCS-P1-040 with backend-owned structured proposals, the `CREATE_PROPOSAL` permission scope, linked-issue validation, a community proposal workspace route, OpenAPI schema coverage, and ADR-backed contract documentation.
- 2026-03-06: Implemented OCS-P1-041 with backend-owned typed deliberation entries, evidence-link validation, moderation that preserves proposal context, OpenAPI contract coverage, and proposal-detail UI sections for pro/con/questions/evidence.
- 2026-03-06: Implemented OCS-P1-046 with a backend-owned governance document library, version history, explicit visibility, `MANAGE_GOVERNANCE_LIBRARY` scope, `/communities/governance` workflow, OpenAPI coverage, and ADR-backed contract notes.
- 2026-03-07: Published `docs/community/frontend-backend-top20-priority-backlog.md` to split the next 20 highest-value tasks into frontend and backend execution lanes with wave sequencing, acceptance criteria, and validation expectations.
- 2026-03-07: Added `05-25-PLAN.md` and normalized GSD documentation so `ROADMAP.md`, `STATE.md`, and `current-backlog.md` all point to the same Phase 5 rolling execution model and next-story order.
- 2026-03-07: Implemented `OCS-P1-047` with a backend-owned community decision ledger, explicit approval-basis metadata, execution-owner linkage, `MANAGE_DECISION_LEDGER` scope, `/communities/decisions` workflow, OpenAPI coverage, ADR update, and targeted backend integration tests.
- 2026-03-07: Implemented `OCS-P1-048` with a backend-owned community trust metrics service, deterministic period filters, freshness metadata, low-data semantics, `/communities/trust` workflow, OpenAPI coverage, ADR update, and targeted backend integration tests.
- 2026-03-07: Implemented `OCS-P1-042` with backend-owned proposal voting rules, eligibility and tally visibility configuration, anti-abuse audit counters, proposal vote UX, OpenAPI coverage, ADR update, and targeted backend integration tests.
- 2026-03-09: Implemented `OCS-P1-049` with backend-owned community moderation reports and sanctions, `MANAGE_MODERATION_QUEUE` enforcement, sanction-aware participation guardrails, `/community/moderation/*` APIs, moderator queue UX, OpenAPI coverage, ADR update, Docker-backed frontend evidence, and targeted backend integration tests.

### Pending Todos

See:

- `docs/community/current-backlog.md`
- `docs/community/frontend-audience-usability-backlog.md`
- `docs/community/frontend-product-clarity-backlog.md`
- `docs/community/community-operating-system-backlog.md`

### Blockers/Concerns

- Host `agent:preflight` still fails on this workstation because `build:web` expects host `tsc`; use the Docker runtime for integrated frontend validation until the host toolchain is normalized.
- The Docker dev container injects PostgreSQL datasource environment into `@ActiveProfiles("test")` Spring tests, so broad backend test runs require explicit H2 JVM overrides until the shared test harness is normalized.
- Existing duplicate-email Playwright case is flaky/not deterministic in current environment and should be stabilized.
- Local frontend E2E execution still depends on workspace dependencies living in Docker volumes; use Docker runtime or wrapper commands, not raw host `npx playwright`.

## Session Continuity

Last session: 2026-03-09
Stopped at: `OCS-P1-049` shipped; next recommended execution target is `OCS-P1-050` privacy center and sensitive-data access logs
Resume file: None
