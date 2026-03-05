# Open Civic Signal OS

## What This Is

Open Civic Signal OS is a civic-tech monorepo that turns community signals into a transparent, auditable, and executable backlog for public action. It combines a Java/Spring backend for deterministic prioritization and a React frontend for reporting, explainability, and community workflows. The product serves citizens, moderators, and public servants with explicit trust and governance constraints.

## Core Value

Community priorities must be visible, explainable, and reproducible end-to-end.

## Requirements

### Validated

- ✓ Deterministic score breakdown exists in backend and is exposed in signal responses.
- ✓ Community workflows (memberships, threads, blog, feed) are implemented and integrated with auth.
- ✓ Governance/quality scaffolding exists (agent checks, CI workflows, backlog docs, release gates).

### Active

- [ ] Resolve API contract drift so OpenAPI fully reflects live endpoints and pagination/error shapes.
- [ ] Harden trust-critical runtime and UX gates (public endpoint auth handling, Playwright evidence reliability).
- [ ] Complete reproducibility and quality bars with enforceable coverage and deterministic ingest/replay.

### Out of Scope

- Participatory budgeting simulation in current cycle — listed as P2 strategic bet.
- City federation compatibility layer in current cycle — deferred to later roadmap phase.

## Context

- Fixed stack for v1: Java backend + React frontend + OpenAPI contracts.
- Backlog source of truth: `docs/community/issue-backlog.md` and `docs/community/current-backlog.md`.
- Product principles emphasize trust, determinism, explainability, and human-in-the-loop governance.
- Current repo already contains expanded community features and multiple trust-critical audits in backlog.

## Constraints

- **Architecture**: Ranking logic stays backend-owned — frontend renders only.
- **Contract**: API behavior changes require OpenAPI updates in same change set.
- **Quality**: CI checks cannot be weakened; evidence required for frontend UX changes.
- **Trust**: Auditability metadata and deterministic behavior are non-negotiable.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Keep Java as scoring source of truth | Determinism and auditability for civic ranking | ✓ Good |
| Keep explicit backlog governance docs in-repo | Prevent ad-hoc scope and improve traceability | ✓ Good |
| Use spec-driven `.planning` artifacts for project mapping | Reduce context loss and improve execution consistency | ✓ Good |

---
*Last updated: 2026-03-04 after system-level GSD setup + codebase mapping*
