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
