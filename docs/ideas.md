# ideas.md

## Vision

Build the most trusted open civic execution stack: from community signals to measurable public action.

## Strategic Outcome

- Communities feel heard because priorities are visible and explained.
- Institutions execute faster because backlog is structured and evidence-backed.
- Public can audit decisions, not just announcements.

## Priority Matrix (Now / Next / Later)

## Now (0-30 days)

- Multi-channel signal intake adapter layer:
  - Web form import
  - CSV batch import
  - WhatsApp/Telegram export parser
- Deterministic prioritization v1:
  - urgency
  - impact
  - affected people
  - community votes
- Public backlog output:
  - sortable JSON
  - static dashboard with filters
- Explainability panel:
  - "score breakdown" per item
- Basic governance workflow:
  - submit signal
  - validate
  - rank
  - publish

## Next (30-90 days)

- Messaging and community sync:
  - scheduled digest to Telegram/WhatsApp channels
  - weekly "top issues" bulletin
  - auto-summary for community assemblies
- Dashboards v2:
  - trends by neighborhood
  - issue aging
  - execution status heatmap
- Top problem lists:
  - weekly top 10 unresolved
  - monthly top by category (safety, mobility, education)
- Institutional execution bridge:
  - convert backlog items to municipal ticket format
  - owner assignment and SLA tracking

## Later (90-180 days)

- Participatory budgeting mode:
  - budget envelope simulation
  - trade-off visualization
- Bias and fairness controls:
  - detect over-representation by channel
  - weighting calibration assistant
- Open API and federation:
  - city-to-city data compatibility
  - open benchmark datasets

## High-Impact Product Modules

- Signal Ingest Hub
- Prioritization Engine
- Public Trust Dashboard
- Action Tracker (from backlog to execution)
- Community Messaging Relay
- Audit and Compliance Ledger

## Messaging Features (Community Reach)

- "Need registered" confirmation message.
- "Status changed" notification for tracked issues.
- Broadcast: top priorities this week.
- Broadcast: actions completed this week.
- Crisis mode alerts for urgent categories.

## Dashboard Ideas

- Civic Pulse:
  - new signals in last 7/30 days
  - most affected sectors
- Transparency Board:
  - score formula
  - last algorithm update
  - data freshness indicators
- Execution Board:
  - planned / in progress / resolved
  - average time to first action

## Ranking Extensions

- Severity multiplier for safety and health incidents.
- Vulnerability index weighting (children, elders, high-risk zones).
- Confidence score by source reliability.
- Duplicate detection to merge repeated reports.

## Anti-Gaming and Integrity

- vote abuse detection
- duplicate account suspicion scoring
- channel diversity requirement before top-rank elevation
- moderator review for outlier signals

## Pilot Program Ideas

- Pilot A: one neighborhood, weekly assembly sync.
- Pilot B: one university campus for facilities issues.
- Pilot C: NGO coalition with thematic categories.

## Metrics for Real Impact

- median days from report to visible prioritization
- median days from prioritization to first institutional response
- community repeat participation rate
- percentage of issues resolved within SLA

## Open Source Growth Ideas

- "starter kit for municipalities" repository template
- contributor map by region
- public roadmap sessions (monthly)
- issue bounties sponsored by local partners

## Prioritized Backlog (Execution Order)

1. Multi-channel ingest adapters
2. Explainable scoring breakdown in dashboard
3. Weekly top-problems digest automation
4. Execution status synchronization workflow
5. Fairness and anti-gaming module
6. Federation/open API package

## Big Bet Experiments

- "Civic Copilot": helps facilitators draft actionable backlog from raw reports.
- "Assembly Mode": real-time display for townhall prioritization.
- "Trust Proof": cryptographic snapshot of each published backlog.

## Risks and Mitigations

- Risk: low data quality.
  Mitigation: strict validation + confidence score.
- Risk: politicization of ranking.
  Mitigation: transparent formula + public changelog.
- Risk: low institutional adoption.
  Mitigation: export formats aligned to existing workflows.

## Implementation Blueprint (Java + React Monorepo)

### Backend (Java)

- IngestController + parser services
- PrioritizationService + score breakdown DTO
- BacklogController + status transitions
- ExportService for institutional bridges

### Frontend (React)

- Public Dashboard (read-only)
- Operator Console (workflow actions)
- Explainability Drawer (per signal)
- Weekly Digest Preview

### Data and Contracts

- OpenAPI as source of truth
- JSON examples for all core flows
- Versioned formula metadata

## Additional High-Impact Ideas

- AI-assisted signal clustering (opt-in, never hidden)
- Moderator copilot for conflict/duplicate resolution
- Community leader mobile view with offline snapshots
- SLA breach early-warning panel
- Participatory budgeting simulator

## Detailed Execution Assets

- Story catalog: `docs/agent/user-stories.md`
- Process diagrams: `docs/agent/process-flows.md`
- System design: `docs/agent/system-design.md`
- Delivery checklists: `docs/agent/execution-checklists.md`

## Additional High-Impact Initiatives

### P0 Additions

- Incident triage board for trust-critical civic events.
- Data freshness monitor per source channel.
- Explainability snapshot export for community assemblies.
- Moderation review queue with reason taxonomy.

### P1 Additions

- SLA breach predictor for institutional backlog owners.
- Category-level trend anomaly detector.
- Community trust pulse survey ingestion.
- Operator playbooks for conflict resolution.

### P2 Additions

- Policy simulator for score-weight adjustments.
- Federation bridge for inter-city backlog comparability.
- Verifiable public archive of ranking history.
