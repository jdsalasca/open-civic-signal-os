# AGENTS.md

## Mission

Open Civic Signal OS turns continuous community feedback into transparent, auditable, and executable public backlogs.

## Product Principles

- Public trust first: every priority decision must be explainable.
- Evidence over intuition: scoring and ranking require explicit inputs.
- Human-in-the-loop: algorithm suggests, humans validate.
- Inclusion by default: low-friction channels for communities with different digital access levels.
- No black-box governance: formulas and assumptions must stay visible in docs.

## Engineering Standards

- Keep core prioritization deterministic.
- Store raw input and derived output separately.
- Never mutate source records during scoring.
- Ensure idempotent runs for all processing scripts.
- Prefer JSON/CSV interoperable formats before custom schemas.

## Data and Safety

- Minimize PII capture at ingest.
- Add anonymization step before publishing public dashboards.
- Keep audit trail metadata: source, timestamp, transformation version.
- Distinguish citizen reports from institutional updates using explicit fields.

## Quality Gates

- Every new feature includes one executable example.
- Every transformation script should fail fast on invalid input.
- Maintain a simple golden dataset for regression checks.
- Add reproducibility note in docs for each scoring change.

## UX Guidelines

- Prioritize clarity over visual complexity.
- Expose "why this item is ranked here" in every UI list.
- Support mobile-first layouts for community leaders in the field.
- Make dashboards readable in low-bandwidth scenarios.

## Collaboration Workflow

- Strategy proposals -> `docs/ideas.md`
- Product decisions -> `docs/DECISIONS.md`
- Concrete execution -> GitHub issues + milestone labels
- Keep PRs small and linked to one objective

## Labels Convention

- `impact-high`: measurable community value in < 30 days.
- `trust-critical`: affects transparency/auditability.
- `data-quality`: schema validation and source reliability.
- `pilot-ready`: can be tested with one real community.

## Release Readiness Checklist

- Backlog output reproducible from sample dataset.
- Dashboard clearly shows formula and last update time.
- At least one civic pilot workflow documented end-to-end.
- Known limitations documented in README.

## North-Star Metrics

- Time from signal capture to visible backlog update.
- Percentage of backlog items with traceable evidence.
- Percentage of high-priority items actioned by institutions.
- Community trust score (survey-based).
