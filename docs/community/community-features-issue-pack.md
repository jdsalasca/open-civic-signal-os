# Community Features Issue Pack

Ready-to-create issues for Open Civic Signal OS community expansion.

Use with:
- `.github/workflows/seed-community-issues.yml` (automated seeding), or
- manual creation in GitHub Issues using the titles and bodies below.

## Labels to use

- Priority: `priority:P1`
- Optional trust/impact tags: `impact-high`, `pilot-ready`, `trust-critical`
- Story items: add `type:story`

## Issue 1

Title: `community-membership: support user membership in one or many communities`

Body:
- Problem: users need to collaborate across multiple neighborhoods/collectives without duplicating accounts.
- Proposed change: implement many-to-many user-community membership with active context selection.
- Acceptance criteria:
  - [ ] A user can belong to multiple communities.
  - [ ] User can switch active community context in UI.
  - [ ] API responses are scoped by selected community.
  - [ ] Membership audit trail includes `createdAt`, `createdBy`, `role`.
- Validation:
  - [ ] Backend membership tests.
  - [ ] Frontend context-switch flow with Playwright evidence.

## Issue 2

Title: `community-rbac: add community-scoped roles and permission policies`

Body:
- Problem: global roles are insufficient for local governance workflows.
- Proposed change: add community-level roles (`MEMBER`, `MODERATOR`, `COORDINATOR`, `PUBLIC_SERVANT_LIAISON`) with policy enforcement.
- Acceptance criteria:
  - [ ] Role checks enforced on community actions.
  - [ ] Moderation and announcements require appropriate role.
  - [ ] Role changes are auditable.
  - [ ] Forbidden actions return deterministic `403`.
- Validation:
  - [ ] RBAC integration tests.
  - [ ] Contract updates documented.

## Issue 3

Title: `community-chat: enable structured inter-community communication channels`

Body:
- Problem: communities cannot coordinate shared incidents across boundaries.
- Proposed change: implement thread-based communication between communities linked to signals.
- Acceptance criteria:
  - [ ] Communities can open cross-community threads.
  - [ ] Messages support author, timestamp, source community, related signal.
  - [ ] Moderators can flag/hide abusive messages.
  - [ ] Thread history is queryable and auditable.
- Validation:
  - [ ] Backend message/thread tests.
  - [ ] Frontend thread UX evidence (desktop/mobile).

## Issue 4

Title: `community-blog: add public-servant progress blog by community`

Body:
- Problem: communities lack visibility into ongoing institutional work.
- Proposed change: enable public servants to publish community progress updates in a blog timeline.
- Acceptance criteria:
  - [ ] Authorized public servants can create/edit/update posts.
  - [ ] Posts belong to a specific community and include status tags.
  - [ ] Citizens can view ordered timeline by community.
  - [ ] Every post shows author role and publish timestamp.
- Validation:
  - [ ] Backend authorization tests for create/edit.
  - [ ] Frontend timeline accessibility and responsive tests.

## Issue 5

Title: `community-feed: show cross-community updates and accountability timeline`

Body:
- Problem: users cannot quickly understand what is progressing across linked communities.
- Proposed change: add unified feed that combines signals, blog updates, and inter-community thread milestones.
- Acceptance criteria:
  - [ ] Feed items are typed and filterable (`signal`, `blog`, `thread-update`).
  - [ ] Feed supports community and timeframe filters.
  - [ ] Data freshness timestamp visible.
  - [ ] Empty/error states are explicit and actionable.
- Validation:
  - [ ] API contract for feed endpoint.
  - [ ] Playwright evidence for filtering flow.

## Story Mapping (for agents)

- `story:OCS-P1-009` Membership model + APIs + UI selector.
- `story:OCS-P1-010` Community-scoped RBAC + audit trail.
- `story:OCS-P1-011` Inter-community conversation threads.
- `story:OCS-P1-012` Public-servant blog by community.
- `story:OCS-P1-013` Community context switcher + scoped dashboard.
- `story:OCS-P1-014` Moderation workflow for threads/blog comments.
