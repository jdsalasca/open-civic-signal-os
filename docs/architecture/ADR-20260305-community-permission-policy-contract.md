# ADR-20260305 Community Permission Policy Contract

- Status: accepted
- Date: 2026-03-05

## Context

Phase 5 expands the product from fixed community roles into community-owned governance. Hardcoded role checks for threads, official updates, and membership management were no longer enough because different communities need different operating rules.

The contract also needed to explain denied actions in a way the frontend can render directly, without inventing permission logic client-side.

## Decision

We introduced a backend-owned permission policy contract per community.

- Add `CommunityPermissionScope` as the stable list of governable actions.
- Add `/api/communities/{communityId}/permissions` for reading the effective policy matrix.
- Add `/api/communities/{communityId}/permissions` `PUT` for coordinator-managed overrides.
- Keep defaults in backend code and only persist overrides in the database.
- Return structured `403` payloads with `COMMUNITY_PERMISSION_DENIED`, `permissionScope`, `currentRole`, and `allowedRoles`.
- Reuse the policy engine for:
  - thread creation
  - thread replies
  - thread moderation
  - official update creation/editing
  - membership role management

## Consequences

Positive:

- Communities can evolve role policies without frontend logic drift.
- The UI can explain permission failures with backend truth.
- Future sensitive actions can attach to the same scope model.

Tradeoffs:

- Scope names become a versioned contract surface and must stay stable.
- Policy changes now require migration-aware backend validation and contract upkeep.

## Notes

- Frontend must render friendly labels for scopes and roles; raw enums remain contract identifiers only.
- Ranking and other domain calculations remain backend-owned and are not affected by this contract.
