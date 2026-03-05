# ADR-20260305-community-hierarchy-contract

- Status: accepted
- Date: 2026-03-05

## Context

Phase 5 foundation work requires communities to model nested spaces such as city -> district -> building or university -> faculty -> program. The app already exposed flat communities and memberships, which made active context ambiguous and prevented the frontend from explaining where a user was operating inside a larger civic structure.

Because this change adds a database migration and changes the live API contract consumed by the frontend, it requires an ADR under repository governance rules.

## Decision

Adopt backend-owned community hierarchy primitives in the contract:

- `Community.parentCommunityId` becomes part of the persisted data model and API response.
- `CreateCommunityRequest` accepts optional `parentCommunityId`.
- `CommunityMembership` responses include `parentCommunityId` and an ordered `breadcrumb`.
- Add `GET /api/communities/tree` for hierarchical navigation.
- Add `GET /api/communities/{communityId}/breadcrumb` for direct path retrieval.

The frontend may render hierarchy and breadcrumbs, but hierarchy ownership stays in backend data and contract semantics.

## Consequences

Positive:

- Users can understand and switch community context inside a real hierarchy instead of a flat list.
- Future permissions, proposals, and transparency features can inherit clear parent-child space semantics.
- The contract now supports breadcrumbs and tree navigation without frontend inference.

Tradeoffs:

- Community creation now carries a parent reference that must be validated server-side.
- Seed data and future imports must consider hierarchy consistency.

## Validation

- Flyway migration `V13__Community_Hierarchy.sql`
- Backend integration test `CommunityHierarchyIT`
- Frontend Playwright spec `community-hierarchy.spec.ts`
