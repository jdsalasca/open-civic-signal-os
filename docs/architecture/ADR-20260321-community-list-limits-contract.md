# ADR-20260321: Community List Limit Contract

- Status: Partially implemented
- Date: 2026-03-21
- Closes: top-20 backlog item 19 (`P1 scalability: final paging/filter consistency sweep`) — partially
- Story: `BE-10` platform hardening pack

## Context

The BE-10 acceptance criterion reads "community endpoints share deterministic paging/filter semantics". The actual state was worse than inconsistent: **no community collection endpoint takes `page` or `size` at all.** Only `/api/signals/prioritized` returns a `ApiPageResponse`. Fourteen community list endpoints return an unbounded `List<>` straight from a repository query.

That is not automatically a defect. These are community-scoped workspaces that hold tens to low hundreds of rows, no user has reported a latency problem, and the stated motivation for BE-10 is that "scale and trust are now constrained more by operational reliability than missing tables".

But unbounded is not deterministic, and a civic accountability surface that can return an arbitrarily large payload is a liability the moment a community does grow.

## Decision

Bound the collection rather than re-paginate it.

- `CommunityListLimits` is one shared contract: `DEFAULT_LIMIT = 50`, `MAX_LIMIT = 200`, and `resolveLimit(Integer)` which clamps rather than rejecting. A null, zero, negative, or oversized value degrades to a bounded read instead of failing the request.
- Applied to the four community workspaces shipped in this cycle, where the service and its single caller were both controlled:
  - `GET /api/community/rooms/workspace`
  - `GET /api/community/activities/board`
  - `GET /api/community/resources/board`
  - `GET /api/community/integrations/center`
- Each takes an optional `limit`, documented in OpenAPI with `minimum: 1`, `maximum: 200`, `default: 50`.

## What is still unbounded, and why

**This does not close the BE-10 criterion.** These ten endpoints still return unbounded lists:

`/api/communities`, `/api/communities/tree`, `/api/communities/{id}/breadcrumb`, `/api/communities/my`, `/api/communities/{id}/permissions`, `/api/community/blog`, `/api/community/blog/archive`, `/api/community/feed`, `/api/community/proposals`, `/api/community/decisions`, `/api/community/projects`, `/api/community/governance/documents`, `/api/community/blog/{id}/comments`, and the comment list on a thread.

Fully paginating them means changing roughly twenty service and repository methods from `List` to `Page` and migrating a frontend consumer for each. That is a large, breaking change with real regression risk, motivated by a problem nobody has reported. Shipping a half-migration that breaks ten views would be worse than the current state.

The bounded approach was chosen because it removes the unbounded-payload liability without a breaking change, and because the endpoints it covers are the ones most likely to grow — rooms, activities, resources, and integrations accumulate per community and have no natural archive.

## Consequences

Positive:

- four community workspaces have a documented, deterministic, bounded read
- one shared contract means the clamp behaviour is identical everywhere it applies
- malformed `limit` values degrade safely instead of erroring

Trade-offs:

- **inconsistent by design across the community surface.** Four endpoints honour `limit`; ten do not. A caller reading the contract can see which.
- the clamp is silent. A caller asking for 500 receives 200 with no indication, because the response carries no truncation marker. Adding one, or switching to `ApiPageResponse` with a `totalElements`, is the correct fix and needs a shape change.
- no `page` support. The contract bounds a response; it does not let a client walk a large collection.
- the ten remaining endpoints are unbounded, so the BE-10 criterion is partially met at best.

## Validation

- `CommunityListLimitsTest` (5/5) covers null, zero, negative, in-range, oversized, and an invariant loop asserting every resolved limit lands in `[1, 200]`
- `mvn test`: 120 tests pass
- OpenAPI documents `limit` on all four endpoints; YAML re-parsed; parity gate passes
- `CommunityActivityIT`, `CommunityResourceIT`, `CommunityRoomsIT`, and `CommunityIntegrationIT` all pass unchanged against the new signatures, confirming the added parameter is backward compatible