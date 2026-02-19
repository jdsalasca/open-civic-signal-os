# Executable User Stories

## P0 Stories

### OCS-P0-001 Multi-Channel Ingest Foundation
- Persona: civic coordinator collecting needs from multiple channels.
- Problem: signals arrive fragmented; manual consolidation delays action.
- Scope: web + csv + chat-export parsers; normalized signal schema.
- Acceptance criteria:
  - all channels map to canonical signal DTO,
  - invalid rows return structured rejection reason,
  - ingest source metadata is preserved.
- Test plan: parser unit tests, normalization integration tests, malformed input tests.

### OCS-P0-002 Explainable Deterministic Scoring
- Persona: public auditor and community leader.
- Problem: priorities are distrusted when ranking is opaque.
- Scope: scoring service returns total + factor breakdown.
- Acceptance criteria:
  - same input dataset yields same ordering,
  - each ranked item includes per-factor contributions,
  - formula version is included in output.
- Test plan: deterministic snapshots, edge-factor tests, ordering tests.

### OCS-P0-003 Public Backlog API v1
- Persona: frontend/dashboard integrator.
- Problem: no stable endpoint for prioritized civic backlog.
- Scope: backend endpoint returning prioritized list with explainability.
- Acceptance criteria:
  - endpoint supports category and zone filters,
  - response includes score breakdown and timestamps,
  - OpenAPI docs include examples.
- Test plan: contract tests + endpoint integration tests.

### OCS-P0-004 Top Problems Dashboard
- Persona: community members tracking unresolved issues.
- Problem: hard to identify top unresolved problems quickly.
- Scope: react page with ranking, filters, and "why ranked" drawer.
- Acceptance criteria:
  - filters by category, neighborhood, status,
  - list shows priority and last-updated,
  - drawer shows score breakdown fields.
- Test plan: component tests + API mock flow tests.

### OCS-P0-005 Audit Trail End-to-End
- Persona: governance and transparency team.
- Problem: evidence chain is incomplete across transformations.
- Scope: add metadata through ingest, scoring, publishing.
- Acceptance criteria:
  - source, ingestion time, transform version are persisted,
  - audit fields available in API and export,
  - immutable event IDs for processing steps.
- Test plan: repository tests and end-to-end lineage checks.

### OCS-P0-006 Abuse and Integrity Controls
- Persona: moderation team.
- Problem: ranking manipulation via duplicate and vote abuse.
- Scope: duplicate heuristics and suspicious voting flags.
- Acceptance criteria:
  - suspicious records are flagged not silently discarded,
  - moderation queue API returns reasons,
  - false-positive safeguards documented.
- Test plan: abuse fixtures and precision/recall sanity tests.

### OCS-P0-007 Weekly Civic Digest Automation
- Persona: community communications lead.
- Problem: manual updates reduce engagement.
- Scope: generate weekly top-issues summary payload.
- Acceptance criteria:
  - includes top unresolved + newly actioned items,
  - includes score/evidence references,
  - supports export to messaging channels.
- Test plan: summary generation tests + scheduling job tests.

### OCS-P0-008 Reproducibility Gate
- Persona: maintainers.
- Problem: scoring changes may drift without trace.
- Scope: CI gate ensuring ranking reproducibility using golden dataset.
- Acceptance criteria:
  - failed reproducibility blocks merge,
  - output diff highlights changed signals and factors.
- Test plan: intentional drift fixture + stable baseline fixture.

## P1 Stories

### OCS-P1-001 Execution SLA and Aging Panels
### OCS-P1-002 Institutional Assignment Workflow
### OCS-P1-003 Formula Metadata Versioning
### OCS-P1-004 Transparency Report Pipeline
### OCS-P1-005 Anti-Gaming Moderation Playbook

## P2 Stories

### OCS-P2-001 Assembly Mode
### OCS-P2-002 Trust-Proof Snapshot
### OCS-P2-003 Federation Contract Starter
