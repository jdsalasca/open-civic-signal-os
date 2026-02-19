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

## Monorepo Implementation Contract

- Stack is fixed for v1: Java backend + React frontend.
- Backend APIs own all domain calculations.
- Frontend must not implement ranking logic independently.
- Every feature issue should map to one of:
  - API endpoint change
  - Contract update
  - UI workflow update

## Agent Handoff Template

When closing an issue, agents must include:

- what changed in backend
- what changed in frontend
- which contract file changed
- sample input/output
- known limitations
## Agent Operations

- Gemini execution guide: `GEMINI_INSTRUCTIONS.md`
- Playbook: `docs/IMPLEMENTATION_PLAYBOOK.md`
- Definition of Done: `docs/DEFINITION_OF_DONE.md`
- PR checklist: `docs/PR_REVIEW_CHECKLIST.md`
- System prompt: `docs/agent/SYSTEM_PROMPT.md`
## Excellence Pack

- Engineering standards: `docs/ENGINEERING_STANDARDS.md`
- Backend standards: `docs/BACKEND_EXCELLENCE.md`
- UX standards: `docs/UX_EXCELLENCE.md`
- Test strategy: `docs/TEST_STRATEGY.md`
- Security baseline: `docs/SECURITY_BASELINE.md`
- Observability guide: `docs/OBSERVABILITY.md`
- ADR template: `docs/architecture/ADR_TEMPLATE.md`
- Agent gate script: `scripts/agent-gate.mjs`
- Agent gate CI: `.github/workflows/agent-gate.yml`
- Execution brief template: `docs/agent/EXECUTION_BRIEF_TEMPLATE.md`
## Release Excellence

- Release policy: `docs/RELEASE_POLICY.md`
- Changelog: `docs/CHANGELOG.md`
- Quality gate script: `scripts/release-quality-gate.mjs`
- CI workflow: `.github/workflows/release-quality-gate.yml`

## Required Agent Context (Read First)

Before implementing any issue, read:

- `AGENTS.md`
- `docs/ideas.md`
- `docs/community/issue-backlog.md`
- `docs/agent/README.md`
- `docs/agent/user-stories.md`
- `docs/agent/process-flows.md`
- `docs/agent/system-design.md`
- `docs/agent/execution-checklists.md`

## Mandatory Delivery Rules

- One primary objective per PR.
- Include backend + contract + frontend impact notes.
- Include explicit acceptance criteria and test evidence.
- Never merge with failing CI.
- Keep ranking logic backend-owned; frontend rendering only.
