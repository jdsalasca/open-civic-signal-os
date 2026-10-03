# Decision Index

Track product and technical decisions that affect behavior, contracts, or policy.

## Required ADR Triggers

Create/update an ADR when any of the following changes:

- API contract (`packages/contracts/openapi.yaml`)
- Domain invariants or policy logic
- Security/auth model
- Data model with migration implications
- Any behavior change requiring migration notes

## ADR List

- Add ADR files here using `ADR-YYYYMMDD-<slug>.md`.
- Keep status updated (`proposed`, `accepted`, `superseded`).
- `ADR-20260220-community-collaboration-model.md` (accepted)
- `ADR-20260222-prioritized-status-filter-contract.md` (accepted)
- `ADR-20260304-prioritized-explainability-summary-contract.md` (accepted)
- `ADR-20260305-civic-profile-visibility-contract.md` (accepted)
- `ADR-20260305-community-hierarchy-contract.md` (accepted)
- `ADR-20260305-community-home-composite-contract.md` (accepted)
- `ADR-20260305-community-permission-policy-contract.md` (accepted)
- `ADR-20260305-signal-intake-wizard-contract.md` (accepted)
- `ADR-20260305-signal-timeline-assignment-contract.md` (accepted)
- `ADR-20260305-community-geospatial-map-contract.md` (accepted)
- `ADR-20260305-community-thread-relevance-contract.md` (accepted)
- `ADR-20260305-official-announcements-channel-contract.md` (accepted)
- `ADR-20260305-community-proposal-templates-contract.md` (accepted)
- `ADR-20260306-community-proposal-deliberation-contract.md` (accepted)
- `ADR-20260306-community-project-boards-contract.md` (accepted)
- `ADR-20260306-governance-library-contract.md` (accepted)
- `ADR-20260307-community-decision-ledger-contract.md` (accepted)
- `ADR-20260307-community-trust-metrics-contract.md` (accepted)
- `ADR-20260307-community-proposal-voting-contract.md` (accepted)
- `ADR-20260308-community-moderation-contract.md` (accepted)
- `ADR-20260308-privacy-center-contract.md` (accepted)
- `ADR-20260309-civic-onboarding-help-center-contract.md` (accepted)
- `ADR-20260319-community-open-data-contract.md` (accepted)
- `ADR-20260321-community-rooms-contract.md` (accepted)
- `ADR-20260321-community-activities-contract.md` (accepted)
- `ADR-20260321-community-resources-contract.md` (accepted)
- `ADR-20260321-community-integrations-contract.md` (accepted)
- `ADR-20260321-signal-audit-metadata-contract.md` (accepted)
- `ADR-20260321-prioritization-formula-metadata-contract.md` (accepted)
- `ADR-20260321-case-aging-sla-contract.md` (accepted)
- `ADR-20260321-runtime-secret-policy.md` (accepted)
- `ADR-20260321-contract-parity-gate.md` (accepted)
- `ADR-20260321-community-list-limits-contract.md` (partial)
- `ADR-20260321-bulk-ingest-contract.md` (accepted)
- `ADR-20260321-public-data-anonymization-gate.md` (accepted)
- `ADR-20260321-transparency-report-pipeline.md` (accepted)
- `ADR-20260321-municipal-ticket-export.md` (accepted)
- `ADR-20260321-surge-detection-formula.md` (accepted)
- `ADR-20260321-freshness-monitoring.md` (accepted)
- `ADR-20260321-explainability-snapshots.md` (accepted)
- `ADR-20260321-ranking-reproducibility.md` (accepted)
- `ADR-20260321-dashboard-filter-queries.md` (accepted)
- `ADR-20260321-duplicate-clustering-human-approval.md` (accepted)
- `ADR-20260321-bias-diagnostics.md` (accepted)
- `ADR-20260321-weekly-digest-snapshot.md` (accepted)
- `ADR-20260321-public-backlog-view.md` (accepted)

## Review Rule

If contract changes are present in a PR, at least one ADR file must be touched.
