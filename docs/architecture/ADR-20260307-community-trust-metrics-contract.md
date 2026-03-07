# ADR-20260307: Community Trust Metrics Contract

- Status: Accepted
- Date: 2026-03-07
- Story: `OCS-P1-048`

## Context

Open Civic Signal OS already supports issue reporting, deliberation, decisions, project boards, and governance records. The remaining trust gap is visible proof that those workflows produce measurable outcomes. Communities need one explainable metrics surface that shows freshness, participation, execution progress, and case throughput without pushing aggregation logic into the frontend.

## Decision

Introduce a backend-owned trust metrics dashboard contract under `/api/community/trust-metrics` with:

- required `communityId` and optional `period` filter
- deterministic cards for resolution rate, participation coverage, execution completion, and median resolution time
- backend-owned freshness and `lastUpdatedAt` metadata
- explicit low-data signaling for sparse communities
- typed drill-down breakdowns for issue outcomes, decisions, tasks, and issue categories

Frontend adds `/communities/trust` as the browse surface, but formulas, aggregation windows, and freshness semantics remain backend-owned.

## Consequences

Positive:

- communities can verify whether the civic loop produces visible outcomes
- low-data periods are explained instead of silently rendering empty charts
- period handling and metric formulas stay deterministic and auditable
- the dashboard creates a trust bridge from reporting and decision-making into public proof

Trade-offs:

- the first release is community-member scoped, not anonymous-public, because existing community surfaces are membership-gated
- aggregation currently uses service-layer composition on existing repositories rather than a dedicated metrics read model
- chart rendering stays intentionally simple until Docker-backed visual validation is available again

## Validation

- targeted backend integration coverage for deterministic metric payloads
- OpenAPI updated in the same change set
- GSD state, backlog, and changelog updated with the shipped story
- frontend route and low-data behavior covered by seeded Playwright spec, with Docker evidence deferred while Docker Engine remains unavailable
