# Requirements: Open Civic Signal OS

**Defined:** 2026-03-04
**Core Value:** Community priorities must be visible, explainable, and reproducible end-to-end.

## v1 Requirements

### Core Prioritization

- [ ] **PRIO-01**: Prioritized signals endpoint returns deterministic ordering for the same dataset and scope.
- [ ] **PRIO-02**: Every prioritized signal includes full score breakdown fields and total score.
- [ ] **PRIO-03**: Signal detail includes status history and trust/audit context.
- [ ] **PRIO-04**: Frontend renders ranking rationale without re-implementing scoring formula.

### Contracts and API Reliability

- [ ] **API-01**: OpenAPI documents all active public endpoints used by backend and frontend.
- [ ] **API-02**: OpenAPI reflects pagination/filter/query semantics for community threads and signals.
- [ ] **API-03**: Error schemas/status codes are consistent between implementation and contract.
- [ ] **API-04**: Contract-impacting changes include same-PR alignment notes and ADR linkage when required.

### Community Workflows

- [ ] **COMM-01**: User can join/switch communities and preserve selected community context across routes.
- [ ] **COMM-02**: Community threads support deterministic server-side paging and status filters.
- [ ] **COMM-03**: Community blog supports role-based posting and engagement counters.
- [ ] **COMM-04**: Community feed aggregates signals/blog/threads with freshness metadata.

### Security and Trust

- [ ] **TRST-01**: Publicly permitted endpoints do not fail when no authenticated principal exists.
- [ ] **TRST-02**: Runtime secrets are externalized for compose/prod-like deployments.
- [ ] **TRST-03**: Verification/onboarding failure modes expose clear user recovery paths.
- [ ] **TRST-04**: Trust-critical gates do not skip backend verification tests.

### Quality and Delivery

- [ ] **QUAL-01**: Preflight/CI checks are consistent with documented quality policy.
- [ ] **QUAL-02**: Frontend UX evidence workflow is executable on supported contributor platforms.
- [ ] **QUAL-03**: Coverage gate has non-zero enforceable threshold with meaningful summaries.
- [ ] **QUAL-04**: Documentation artifacts are well-formed and free of malformed markdown/json.

## v2 Requirements

### Advanced Governance

- **GOV-01**: Policy simulation for score weights with controlled, auditable diffs.
- **GOV-02**: Explainability export snapshots tailored for community assemblies.

### Platform Expansion

- **PLAT-01**: Federation-ready API compatibility for multi-city interoperability.
- **PLAT-02**: Low-bandwidth operator mode with performance budget guarantees.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Full monthly/yearly automation orchestration revamp | Outside current stabilization and contract-alignment objective |
| New runtime dependencies for convenience | Explicitly restricted unless story requires and justifies |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PRIO-01 | Phase 1 | Pending |
| PRIO-02 | Phase 1 | Pending |
| PRIO-03 | Phase 2 | Pending |
| PRIO-04 | Phase 2 | Pending |
| API-01 | Phase 1 | Pending |
| API-02 | Phase 1 | Pending |
| API-03 | Phase 1 | Pending |
| API-04 | Phase 1 | Pending |
| COMM-01 | Phase 3 | Pending |
| COMM-02 | Phase 3 | Pending |
| COMM-03 | Phase 3 | Pending |
| COMM-04 | Phase 3 | Pending |
| TRST-01 | Phase 1 | Pending |
| TRST-02 | Phase 2 | Pending |
| TRST-03 | Phase 2 | Pending |
| TRST-04 | Phase 1 | Pending |
| QUAL-01 | Phase 1 | Pending |
| QUAL-02 | Phase 2 | Pending |
| QUAL-03 | Phase 2 | Pending |
| QUAL-04 | Phase 1 | Pending |

**Coverage:**
- v1 requirements: 20 total
- Mapped to phases: 20
- Unmapped: 0

---
*Requirements defined: 2026-03-04*
*Last updated: 2026-03-04 after spec-driven codebase baseline*
