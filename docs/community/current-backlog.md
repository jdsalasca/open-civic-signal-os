# Current Backlog (2026-02-19)

Operational backlog snapshot for immediate execution in Open Civic Signal OS.

## Current State

- Backend Java API is active with prioritization and score breakdown.
- React dashboard supports list and detail navigation for signals.
- Agent governance and quality workflows are configured.

## Now (Next 7 Days)

1. Stabilize signal detail flow end-to-end.
   - Validate `GET /api/signals/{id}` behavior for missing/non-existing IDs.
   - Add API tests for detail endpoint status and payload.
   - Add web test for dashboard-to-detail navigation.
2. Harden prioritization reliability.
   - Add deterministic dataset regression checks for score order.
   - Validate duplicate-detection behavior with realistic near-duplicate cases.
3. Publish explainability trust surface.
   - Add "why ranked here" section in detail page with factor descriptions.
   - Add docs journey: report -> score -> publish -> audit trail.
4. Protect delivery quality.
   - Restore/verify `npm run quality:quick` as required local gate.
   - Add Java compile/test check in CI for `apps/api-java`.

## Next (7-21 Days)

1. Ingest channel expansion.
   - CSV validator with row-level error report.
   - WhatsApp/Telegram export parser adapter.
2. Execution bridge.
   - Backlog item ownership workflow and status transitions.
   - Municipal export format adapter for top prioritized issues.
3. Civic messaging automation.
   - Weekly top issues digest generation.
   - "You asked, we shipped" operational digest template.

## Community Features Track (New)

1. Multi-community memberships.
   - Users can belong to one or more communities with explicit membership records.
   - Dashboard and reporting filters are scoped by selected community context.
2. Inter-community communication.
   - Community spaces can open structured cross-community conversations on shared issues.
   - Add moderation and audit logs for community-to-community threads.
3. Community internal roles.
   - Define community-scoped roles (member, moderator, coordinator, public-servant liaison).
   - Enforce role-based permissions for posting, moderation, and official announcements.
4. Community progress blog for public servants.
   - Public servants can publish progress updates per community.
   - Citizens can follow updates and see timeline visibility of what is being worked on.

## P0 Execution Queue (Agent Order)

1. `story:OCS-P0-003` expose prioritized backlog API with explainability fields.
2. `story:OCS-P0-004` ship public dashboard top problems and filters.
3. `story:OCS-P0-005` add full audit metadata from ingest to publish.
4. `story:OCS-P0-006` add abuse detection pipeline and moderator queue.
5. `story:OCS-P0-008` add reproducibility script for ranking outputs.

## Definition of Ready for Any New Story

- Story ID exists in `docs/community/issue-backlog.md`.
- Acceptance criteria and validation commands are explicit.
- Contract and rollback notes are included.
- Owner lane assigned (backend, frontend, docs, ops).

## Critical Fix Backlog (Audit 2026-02-23)

1. `P0 security: remove dev profile from production compose runtime`
   - Replace `SPRING_PROFILES_ACTIVE: dev` in `infra/docker-compose.yml` with a hardened runtime profile.
   - Acceptance criteria:
     - verification code backdoor (`123456`) is not accepted in compose runtime
     - SQL debug logging is disabled in compose runtime
     - auth and signal flows still pass smoke tests
2. `P0 security: externalize JWT secret and forbid hardcoded fallback in compose`
   - Remove inline secret from `infra/docker-compose.yml` and load via environment/secret manager.
   - Acceptance criteria:
     - compose starts only when explicit secret is provided
     - no static secret literal remains in repo runtime files
3. `P1 trust-critical: align OpenAPI with active signal endpoints`
   - Add contract entries for currently used endpoints (`/api/signals/{id}`, `/api/signals/meta`, `/api/signals/mine`, `/api/signals/{id}/vote`).
   - Acceptance criteria:
     - OpenAPI includes request/response + error schemas for these routes
     - API integration tests validate at least detail + meta contract examples
4. `P1 performance: remove N+1 engagement calls in community blog timeline`
   - Replace per-post `GET /api/community/blog/{id}/comments` fetch pattern with batched comment summary/list endpoint.
   - 2026-03-04 remediation evidence:
     - Blog initial load now uses batched `GET /api/community/blog/comments/count` and avoids per-post comments fetch.
     - Playwright validation: `apps/web-react/src/tests/community-threads-paging.spec.ts` (`community blog initial load uses batched comment count endpoint without per-post comment fan-out`).
   - Acceptance criteria:
     - blog timeline load requires O(1) or O(log n) API calls for engagement data
     - Playwright trace shows at least 70% fewer blog comment requests on initial load
5. `P1 performance: cache and de-duplicate membership loading in layout`
   - Avoid repeated `GET /api/communities/my` calls across route transitions.
   - Acceptance criteria:
     - a single navigation session does not re-fetch memberships on every page unless explicit refresh
     - global state persistence tests remain green

## Critical Fix Backlog (Audit 2026-02-25)

1. `P0 trust-critical: harden registration email delivery in compose/prod-like runtime`
   - Evidence: API logs show repeated `EmailService ... Authentication failed` during `POST /api/auth/register` and welcome flow.
   - Civic risk: users cannot complete verification without out-of-band DB access, breaking onboarding trust.
   - Acceptance criteria:
     - registration + resend + welcome emails succeed in compose runtime with configured SMTP credentials
     - if SMTP is unavailable, API returns explicit degraded-mode message and stores auditable delivery failure reason
2. `P0 ux-critical: expose verification fallback UX when email delivery fails`
   - Evidence: current verify flow assumes inbox delivery and provides no in-app recovery route.
   - Civic risk: real users get blocked at account activation with no guided resolution path.
   - Acceptance criteria:
     - verify screen includes clear retry/help path (`resend`, cooldown, support route, and state feedback)
     - Playwright desktop/mobile coverage confirms complete recovery path from failed delivery
3. `P1 scalability: add pagination/filter contract to community threads endpoint`
   - Evidence: smoke call to `GET /api/community/threads?communityId=...` returned 50 items with no paging controls.
   - Civic risk: thread timelines will degrade quickly for active communities and increase payload costs.
   - Acceptance criteria:
     - API supports deterministic paging (`page`, `size`, `sort`) and optional status filter
     - frontend uses server paging and preserves current filter/sort between route transitions
4. `P1 trust-critical: show backend permission reason in blog/thread creation UI`
   - Evidence: `POST /api/community/blog` returns `403 Security clearance insufficient for this sector` for non-staff roles.
   - Civic risk: users perceive broken actions if role constraints are not explained contextually.
   - Acceptance criteria:
     - UI hides disabled actions for unauthorized roles and shows inline rationale where relevant
     - role requirements are documented in EN/ES copy for blog + threads entry points
5. `P1 developer-experience: fix Windows Playwright wrapper path normalization`
   - Evidence: `npm run agent:ux:pw -- open ...` failed with `/bin/bash: C:Users...playwright_cli.sh: No such file or directory`.
   - Civic risk: required UX evidence gate becomes unreliable for contributors on Windows.
   - Acceptance criteria:
     - wrapper resolves Windows paths correctly or cleanly falls back to `npx @playwright/cli`
     - `agent:ux:pw` open/snapshot workflow works from Windows and Linux without manual env overrides
