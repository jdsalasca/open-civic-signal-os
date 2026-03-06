# ADR-20260306 Community Project Boards Contract

- Status: accepted
- Date: 2026-03-06

## Context

`story:OCS-P1-043` requires communities to turn approved proposals into visible execution work. The system already supports proposals and deliberation, but it still lacks an execution surface where tasks, owners, due dates, and task notes remain auditable in the same community context.

## Decision

Add backend-owned community project boards with:

- project boards linked to one community and optionally one structured proposal
- tasks with deterministic statuses: `TODO`, `IN_PROGRESS`, `DONE`
- optional task assignee and due date
- task comments for execution notes and blockers
- permission scope `MANAGE_PROJECT_BOARDS` for board/task creation and status movement
- membership-gated read access so only community members can inspect boards

Expose this through:

- `GET /api/community/projects?communityId=...`
- `GET /api/community/projects/{projectId}`
- `POST /api/community/projects`
- `POST /api/community/projects/{projectId}/tasks`
- `PATCH /api/community/projects/{projectId}/tasks/{taskId}`
- `POST /api/community/projects/{projectId}/tasks/{taskId}/comments`

## Consequences

- proposals can now move into execution without inventing frontend-only workflow state
- project delivery remains tied to the same community and optional proposal origin
- execution progress becomes visible and auditable through one backend contract
- future decision-ledger work can link formal decisions to actual board/task delivery

## Validation

- `mvn -q -Dtest=CommunityProjectBoardIT test`
- `docker exec infra-civic-web-dev-1 sh -lc "cd /workspace && npm --workspace apps/web-react run build"`
- `npm run agent:context:check`
- `npm run backlog:current:check`
- `npm run agent:adr:check`
