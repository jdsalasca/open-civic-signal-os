# ADR-20260305-community-geospatial-map-contract

- Status: accepted
- Date: 2026-03-05

## Context

`story:OCS-P1-039` requires geospatial visibility that is useful for communities, not just decorative mapping. The product already stores `latitude`, `longitude`, and `locationLabel` on signals, but there was no backend-owned contract for:

- one active community map with local issue points and cluster summaries
- one federated heat surface across the communities visible to the current user

Without a shared contract, frontend filters and heat semantics would drift into client-owned logic and break auditability.

## Decision

Add two backend-owned endpoints under the signal domain:

- `GET /api/signals/map`
  - requires `communityId`
  - returns mapped points, cluster summaries, coverage counters, filter echo, and available filter dimensions for one community
- `GET /api/signals/map/heat`
  - returns cross-community heat cells for the communities visible to the current user
  - falls back to global community visibility only for staff roles without memberships

Heat semantics are deterministic and explicit:

- point `heatWeight` = `priorityScore + max(impact, urgency) * 10`
- community and cluster heat = sum of child `heatWeight` values

Frontend responsibilities:

- render filters and send them to backend
- render points, clusters, and cross-community heat cells
- never compute geospatial prioritization or community heat independently

## Consequences

Positive:

- every community gets a usable local map
- cross-community pressure is visible without mixing business rules into React
- filters remain testable against explicit API query params

Tradeoffs:

- heat is a product-level pressure metric, not a GIS-grade kernel-density algorithm
- communities with no coordinates still require `locationLabel` follow-up and show up as unmapped coverage debt

## Contract Impact

- `packages/contracts/openapi.yaml`
  - add `CommunitySignalMap`
  - add `CommunitySignalsHeatMap`
  - add supporting point/cluster/filter/heat-cell schemas

## Validation

- backend integration tests for community map and cross-community heat visibility
- frontend route with real filter-to-query assertions
- Docker web build and GSD contract/ADR checks
