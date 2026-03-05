# Roadmap: Open Civic Signal OS

## Overview

This roadmap stabilizes trust-critical behavior first, then aligns contracts and quality gates, and finally hardens community workflows for scalable civic operations. The sequence prioritizes reproducibility, explainability, and CI-enforceable reliability before adding new feature breadth.

## Phases

- [ ] **Phase 1: Trust and Contract Stabilization** - Remove trust-critical runtime mismatches and align OpenAPI with live API behavior.
- [ ] **Phase 2: Quality Gate Hardening** - Make verification and evidence gates deterministic across environments.
- [ ] **Phase 3: Community Workflow Scalability** - Improve paging/performance/state integrity for high-activity communities.

## Phase Details

### Phase 1: Trust and Contract Stabilization
**Goal**: Public/API trust surface is correct, documented, and deterministic.
**Depends on**: Nothing (first phase)
**Requirements**: PRIO-01, PRIO-02, API-01, API-02, API-03, API-04, TRST-01, TRST-04, QUAL-01, QUAL-04
**Success Criteria**:
1. Public signal endpoints return valid responses without anonymous-principal failures.
2. OpenAPI includes active signal/community/auth endpoints with accurate request/response models.
3. Contract and implementation parity is validated in CI with no skipped trust-critical checks.
**Plans**: 3 plans

Plans:
- [ ] 01-01: Fix public endpoint auth null-safety and add regression tests.
- [ ] 01-02: Update OpenAPI to match active routes, pagination, and error schemas.
- [ ] 01-03: Align CI/gates with non-skipped backend verification policy.

### Phase 2: Quality Gate Hardening
**Goal**: Quality evidence is reliable and repeatable for contributors and CI.
**Depends on**: Phase 1
**Requirements**: PRIO-03, PRIO-04, TRST-02, TRST-03, QUAL-02, QUAL-03
**Success Criteria**:
1. Playwright evidence command works reliably on Windows/Linux contributor paths.
2. Coverage gate enforces non-zero thresholds with meaningful coverage summaries.
3. Onboarding/verification degraded-mode UX is validated with deterministic test evidence.
**Plans**: 3 plans

Plans:
- [ ] 02-01: Fix Playwright wrapper fallback behavior and add Windows path normalization checks.
- [ ] 02-02: Raise and enforce release coverage minimum with updated reporting.
- [ ] 02-03: Expand auth/verify degraded-mode tests and UX evidence artifacts.

### Phase 3: Community Workflow Scalability
**Goal**: Community collaboration surfaces remain performant and predictable at scale.
**Depends on**: Phase 2
**Requirements**: COMM-01, COMM-02, COMM-03, COMM-04
**Success Criteria**:
1. Thread/blog/community surfaces use server-driven paging/filter semantics consistently.
2. Global community context and role selection persist correctly across route transitions.
3. Initial load and engagement APIs avoid avoidable N+1 request patterns.
**Plans**: 3 plans

Plans:
- [ ] 03-01: Optimize thread/message retrieval and validate paging contracts end-to-end.
- [ ] 03-02: Harden community state persistence and route transition behavior with tests.
- [ ] 03-03: Reduce engagement call fan-out in community blog/feed paths.

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Trust and Contract Stabilization | 0/3 | Not started | - |
| 2. Quality Gate Hardening | 0/3 | Not started | - |
| 3. Community Workflow Scalability | 0/3 | Not started | - |
