# ADR-20260305: Community Home Composite Contract

- Date: 2026-03-05
- Status: Accepted

## Context

Community surfaces were split across feed, official updates, threads, and dashboard. That fragmentation made the product feel generic and forced users to infer what mattered in a community.

`story:OCS-P1-036` requires one coherent landing surface for a selected community.

## Decision

Add `GET /api/community/home?communityId=...` as a composite endpoint owned by backend.

The response includes:

- `generatedAt`
- `freshness`
- `activeRoomsCount`
- `officialUpdates`
- `hotThreads`
- `topSignals`

`activeRoomsCount` is `0` for now because real-time room infrastructure is not yet implemented; keeping it in the contract preserves the future shape of the community home without requiring frontend-owned placeholders.

## Consequences

Positive:

- frontend renders one community-first home without stitching several endpoints together
- section priority stays backend-informed
- freshness is visible and explicit

Tradeoffs:

- composite endpoint duplicates some data already available in narrower endpoints
- room presence is currently a zero-state placeholder until `OCS-P1-035`

## Validation

- backend composite endpoint integration test
- frontend Docker build against the new response shape
- contract + ADR gate in same change set
