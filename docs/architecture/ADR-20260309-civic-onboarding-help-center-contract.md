# ADR-20260309: Civic Onboarding And Help Center Contract

- Status: Accepted
- Date: 2026-03-09
- Story: `OCS-P1-051`

## Context

Open Civic Signal OS now exposes community home, trust metrics, decision ledger, proposal voting, moderation, and privacy controls. Those surfaces are high-value, but they also increase product complexity for first-time participants and role-specific operators. Before this story, onboarding guidance was fragmented across route copy and did not give citizens, moderators, or representatives one backend-owned place to learn the next step, search bilingual help content, or dismiss guidance without losing it permanently.

## Decision

Introduce a backend-owned civic onboarding and help center contract with:

- one audience model for help targeting:
  - `CITIZEN`
  - `MODERATOR`
  - `REPRESENTATIVE`
- one surface model for contextual guide routing:
  - `GENERAL`
  - `DASHBOARD`
  - `REPORT`
  - `COMMUNITIES`
  - `PROPOSALS`
  - `GOVERNANCE`
  - `PROJECTS`
- one guide-kind model:
  - `ARTICLE`
  - `CONTEXTUAL`
- two endpoints:
  - `GET /api/help-center`
  - `PUT /api/help-center/state`
- one persisted per-user state model:
  - completed onboarding step keys
  - dismissed guide keys

Audience resolution stays backend-owned and is derived from the authenticated user plus optional active community context. Search, bilingual content selection, and contextual guide matching also stay backend-owned so the frontend only renders deterministic help payloads and updates persisted state.

## Consequences

Positive:

- first-run users get one consistent help surface instead of route-by-route guesswork
- moderators and representatives receive role-specific guidance without duplicating policy logic in the frontend
- onboarding completion and dismissed guides follow the account across sessions
- contextual help can be embedded in high-risk workflows while remaining contract-driven and auditable

Trade-offs:

- first release ships curated static bilingual content inside the backend service instead of a separate CMS workflow
- audience resolution depends on current community context, so users may see different help payloads when they switch communities
- persisted onboarding state is lightweight key storage, not a full event history of every guide interaction

## Validation

- targeted backend integration coverage for help-center retrieval and persisted onboarding state
- OpenAPI updated in the same change set
- frontend contextual help panels and full help-center workflow added with Docker build validation
- GSD planning/state/backlog/changelog updated with the shipped story
