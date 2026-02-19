# Community Issue Backlog

Persistent, executable backlog for Open Civic Signal OS.

Priority legend:

- `P0`: immediate civic impact and trust.
- `P1`: growth, automation, and institutional adoption.
- `P2`: strategic platform bets.

## Current Sprint Focus (2026-02-19)

1. `api: stabilize GET /signals/{id} with explicit not-found behavior`
2. `web: complete dashboard -> signal detail navigation and explainability UI`
3. `tests: add backend endpoint coverage for detail and prioritization stability`
4. `ci: ensure Java backend checks run in pull requests`
5. `docs: publish trust journey from report ingestion to ranked publish`

## P0 Issues

1. `ingest: implement WhatsApp/Telegram export parser with schema validation`
2. `ingest: add CSV import validator with row-level error report`
3. `scoring: publish explainable scoring breakdown API endpoint`
4. `scoring: add deterministic score regression dataset and tests`
5. `dashboard: add why-ranked-here panel per civic signal`
6. `dashboard: add top unresolved problems by category and zone`
7. `workflow: implement submit -> validate -> rank -> publish lifecycle`
8. `audit: attach source metadata and transformation version to ranked outputs`
9. `fairness: add duplicate and vote-abuse detection rules`
10. `alerts: weekly top-issues digest for community channels`
11. `api: publish OpenAPI examples for core signal lifecycle`
12. `tests: add end-to-end civic signal prioritization scenario`
13. `docs: add trust and explainability user journey`
14. `docs: add municipal onboarding quickstart`
15. `ci: add backlog reproducibility quality gate`

## P1 Issues

1. `messaging: add scheduled weekly bulletin generation`
2. `dashboard: add issue aging and SLA risk panels`
3. `execution: add institutional assignment and owner workflow`
4. `execution: add status transitions with audit trail`
5. `analytics: add neighborhood trend and surge detection`
6. `fairness: add channel diversity weighting policy`
7. `contracts: version prioritization formula metadata`
8. `community: add participatory assembly mode screen`
9. `docs: add anti-gaming moderation playbook`
10. `scripts: generate monthly transparency report`
11. `exports: municipal ticket export format adapter`
12. `security: add public data anonymization checklist enforcement`
13. `ci: add changelog and contract consistency checks`
14. `growth: create civic pilot success story template`
15. `ops: create triage protocol for trust-critical incidents`

## P2 Issues

1. `budgeting: participatory budgeting simulation module`
2. `federation: city-to-city open API compatibility layer`
3. `governance: public formula change proposal workflow`
4. `trust-proof: cryptographic snapshot for published backlog`
5. `ai: optional civic signal clustering with human approval`
6. `mobile: low-bandwidth operator mode`
7. `benchmarks: scoring performance stress suite`
8. `policy: fairness calibration assistant`
9. `platform: plugin architecture for custom scoring factors`
10. `evidence: cross-source confidence index model`

## Deep Execution Stories

1. `story:OCS-P0-001 build ingest adapters for web/csv/chat exports`
2. `story:OCS-P0-002 implement deterministic prioritization service with score breakdown`
3. `story:OCS-P0-003 expose prioritized backlog API with explainability fields`
4. `story:OCS-P0-004 ship public dashboard top problems and filters`
5. `story:OCS-P0-005 add full audit metadata from ingest to publish`
6. `story:OCS-P0-006 add abuse detection pipeline and moderator queue`
7. `story:OCS-P0-007 implement weekly civic digest generation`
8. `story:OCS-P0-008 add reproducibility script for ranking outputs`
9. `story:OCS-P1-001 add issue aging, trends, and SLA risk views`
10. `story:OCS-P1-002 implement municipal execution bridge and ownership`
11. `story:OCS-P1-003 version and expose formula metadata`
12. `story:OCS-P1-004 build transparency monthly report pipeline`
13. `story:OCS-P1-005 add anti-gaming policy and moderation playbook`
14. `story:OCS-P2-001 prototype assembly mode for townhall facilitation`
15. `story:OCS-P2-002 implement trust-proof snapshot for backlog publish`
16. `story:OCS-P2-003 design federation-ready contracts for multi-city use`

Use `.github/workflows/seed-community-issues.yml` to persist these as GitHub issues.

## Additional Deep Stories

17. `story:OCS-P1-006 add data freshness monitoring and stale-source alerts`
18. `story:OCS-P1-007 add explainability export snapshots for assemblies`
19. `story:OCS-P1-008 ingest community trust pulse inputs and aggregates`
20. `story:OCS-P2-004 build policy simulation sandbox for scoring weights`
21. `story:OCS-P2-005 deliver low-bandwidth field dashboard mode`
