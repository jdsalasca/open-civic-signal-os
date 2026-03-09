# ADR-20260308: Privacy Center And Sensitive Access Contract

- Status: Accepted
- Date: 2026-03-08
- Story: `OCS-P1-050`

## Context

Open Civic Signal OS already exposes civic identity, community permissions, moderation, decision ledgers, and trust metrics. Those features increase trust only if users can control what parts of their identity and participation remain visible, and if privileged reads leave an auditable trail. Before this story, profile visibility stopped at role/bio and affiliations, community publication policy was implicit, and privileged profile reads or signal exports did not surface a user-facing audit log.

## Decision

Introduce a backend-owned privacy center contract with:

- one new profile audience field:
  - `activityVisibility`
- one personal audit endpoint:
  - `GET /api/auth/privacy/access-logs`
- one community privacy policy surface:
  - `GET /api/communities/{communityId}/privacy`
  - `PUT /api/communities/{communityId}/privacy`
- one explicit community publication policy enum:
  - `DISABLED`
  - `AGGREGATED_PUBLIC`
  - `AGGREGATED_AND_DECISIONS`
- one new community permission scope:
  - `MANAGE_PRIVACY_SETTINGS`
- one sensitive access audit model for first release:
  - `PROFILE_ADMIN_VIEW`
  - `SIGNAL_EXPORT`

Privacy and publication decisions stay backend-owned. The frontend only renders the returned profile audiences, access log entries, and publication policy options. Privileged profile reads and signal exports must write audit log records through backend services.

## Consequences

Positive:

- users gain one visible place to review who accessed protected data
- communities can publish aggregated civic outputs without exposing raw personal records
- privacy-sensitive behavior becomes explainable and auditable instead of implicit
- publication policy joins the same permission framework already used for moderation, governance, and membership rules

Trade-offs:

- first release audits only protected profile reads and signal exports, not every possible sensitive lookup
- community privacy policy is visible only to roles allowed by backend policy, so some members will see a locked state instead of configuration controls
- aggregated publication policy is declarative in this release; downstream public dashboards still need to consume it explicitly

## Validation

- targeted backend integration coverage for profile privacy updates, audit-log retrieval, and community privacy policy access control
- OpenAPI updated in the same change set
- frontend settings flow updated with privacy center UI and Playwright coverage
- GSD planning/state/backlog/changelog updated with the shipped story
