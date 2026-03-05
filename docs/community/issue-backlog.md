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
16. `community-membership: support user membership in one or many communities`
17. `community-rbac: add community-scoped roles and permission policies`
18. `community-chat: enable structured inter-community communication channels`
19. `community-blog: add public-servant progress blog by community`
20. `community-feed: show cross-community updates and accountability timeline`

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

## Community Features Expansion (Agent-Ready)

1. `story:OCS-P1-009 model community membership with user-community join table, API, and UI selector`
2. `story:OCS-P1-010 add community-scoped RBAC (member, moderator, coordinator, liaison) with audit trail`
3. `story:OCS-P1-011 implement inter-community conversation threads linked to civic signals`
4. `story:OCS-P1-012 ship public-servant community blog (create/list/update) with public timeline view`
5. `story:OCS-P1-013 add community context switcher and scoped dashboards/reporting`
6. `story:OCS-P1-014 add moderation workflow for community conversations and blog comments`

## Frontend Audience and Usability Expansion (GSD 2026-03-04)

1. `story:OCS-P1-015 harden register->verify->first-action funnel with explicit progress and recovery states`
2. `story:OCS-P1-016 add low-bandwidth mobile UX mode with payload/performance budget enforcement`
3. `story:OCS-P1-017 ship public shareability surfaces and social card metadata for top civic issues`
4. `story:OCS-P1-018 complete plain-language EN/ES copy parity for trust-critical and contribution flows`
5. `story:OCS-P1-019 uplift accessibility baseline (keyboard-first paths, landmarks, focus states, contrast)`
6. `story:OCS-P1-020 expose list-level explainability snippets to improve ranking trust for new users`
7. `story:OCS-P1-021 add notification preference UX for re-engagement and community follow-ups`
8. `story:OCS-P1-022 instrument frontend conversion funnel analytics with auditable event contracts`
9. `story:OCS-P1-023 simplify dashboard action hierarchy to reduce first-view choice overload`
10. `story:OCS-P1-024 add role-aware guided home with one recommended next action`
11. `story:OCS-P1-025 simplify navigation labels and progressively disclose advanced options`
12. `story:OCS-P1-026 explain the report-to-outcome lifecycle in plain language after submission`
13. `story:OCS-P1-027 make empty and restricted states action-oriented with recovery paths`
14. `story:OCS-P1-028 rewrite public-facing README and landing narrative around user value`

Reference pack: `docs/community/frontend-audience-usability-backlog.md`

## Community Operating System Expansion (GSD 2026-03-05)

1. `story:OCS-P1-029 add enriched civic identity profiles with privacy visibility controls`
2. `story:OCS-P1-030 model hierarchical communities (organization -> subgroup -> channel) with breadcrumbs`
3. `story:OCS-P1-031 implement community-scoped RBAC policies with configurable scopes`
4. `story:OCS-P1-032 add role-aware simple vs advanced experience modes`
5. `story:OCS-P1-033 build official announcements channel with pinned archive and trust labeling`
6. `story:OCS-P1-034 evolve community discussions into nested forum threads with relevance ordering`
7. `story:OCS-P1-035 add real-time community rooms, mentions, and thread mute controls`
8. `story:OCS-P1-036 unify official updates, discussions, and chat presence in community home`
9. `story:OCS-P1-037 turn report flow into issue tracker with wizard, evidence, and impact capture`
10. `story:OCS-P1-038 add case timeline, assignment, and lifecycle audit trail to every issue`
11. `story:OCS-P1-039 add geospatial issue map with clustering and community filters`
12. `story:OCS-P1-040 launch proposal templates with problem, solution, cost, and beneficiaries structure`
13. `story:OCS-P1-041 add structured deliberation with pro/con arguments and evidence markers`
14. `story:OCS-P1-042 implement verifiable community voting and prioritization rules`
15. `story:OCS-P2-006 establish verified identity tiers for sensitive actions`
16. `story:OCS-P2-007 design secure election workflow with auditable vote handling`
17. `story:OCS-P2-008 prototype participatory budgeting allocation flow`
18. `story:OCS-P1-043 add community project boards with micro-kanban and task accountability`
19. `story:OCS-P1-044 add volunteering signup flows with slots, quotas, and reminders`
20. `story:OCS-P1-045 add shared resource booking calendars with approval rules`
21. `story:OCS-P1-046 build governance library for rules, minutes, budgets, and agreements`
22. `story:OCS-P1-047 add community decision ledger linked to proposals, votes, and execution`
23. `story:OCS-P1-048 publish public trust metrics dashboards with freshness and drill-downs`
24. `story:OCS-P1-049 implement moderation queues, sanctions, and policy-based enforcement`
25. `story:OCS-P1-050 ship privacy center with visibility controls and sensitive-data access logs`
26. `story:OCS-P1-051 add civic onboarding, guided education, and contextual help center`
27. `story:OCS-P1-052 expose open civic data exports and scoped external API access`
28. `story:OCS-P1-053 add outbound integrations layer for calendar, messaging, email, and maps`
29. `story:OCS-P2-009 design bounded civic reputation model with non-toxic incentives`

Reference pack: `docs/community/community-operating-system-backlog.md`
