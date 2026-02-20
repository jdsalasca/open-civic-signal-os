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
- Keep notifications and logs from covering interactive fields:
  - no toast/overlay in viewport center during form workflows
  - reserve safe top/bottom offsets for sticky nav + mobile keyboards
  - validate registration/login/verify screens with Playwright in desktop + mobile

## Collaboration Workflow

- Strategy proposals -> `docs/ideas.md`
- Product decisions -> `docs/DECISIONS.md`
- Concrete execution -> GitHub issues + milestone labels
- Keep PRs small and linked to one objective

## Actionable Backlog Source (Mandatory)

- Primary executable backlog: `docs/community/issue-backlog.md`
- Operational sequencing source: `docs/community/current-backlog.md`
- Community features expansion source: `docs/community/community-features-issue-pack.md`
- GitHub issue seeding workflow: `.github/workflows/seed-community-issues.yml`
- Agents must start from these backlog sources before proposing new scope.

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

- Agent hard rules: `.agentic-rules`
- Gemini project context: `GEMINI.md`
- Gemini execution guide: `GEMINI_INSTRUCTIONS.md`
- Codex execution contract: `CODEX.md`
- Playwright visual protocol: `docs/agent/playwright-visual-protocol.md`
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
- `.agentic-rules`
- `GEMINI.md`
- `GEMINI_INSTRUCTIONS.md`
- `CODEX.md`
- `docs/agent/playwright-visual-protocol.md`
- `docs/agent/agent-capability-matrix.md`
- `docs/agent/ux-quality-bar.md`
- `docs/agent/must-should-rules.md`
- `docs/ideas.md`
- `docs/community/issue-backlog.md`
- `docs/community/current-backlog.md`
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

## AI Failure Patterns and Guardrails (Mandatory)

These rules were added from recurring AI-generated mistakes in this repository.

- Do not commit generated artifacts:
  - `apps/web-react/dist/*`
  - `apps/web-react/test-results/*`
  - `*.png` and `*.jpg` screenshots generated during tests/audits
  - temporary logs and local cache files
- Do not introduce hidden dependency churn:
  - never add test or UI tooling dependencies unless the issue explicitly requires it
  - if a dependency is added, justify it in PR summary
- Do not mutate files outside scope:
  - if the task is docs/backlog, do not modify runtime code
  - if the task is backend-only, do not redesign frontend styles
- Do not weaken quality gates:
  - no `-DskipTests` for backend validation in CI
  - no bypass of required checks
- Do not ship malformed markdown/json:
  - never commit escaped newline literals like `` `n `` as documentation bullets
  - ensure `package.json` remains valid JSON with no trailing literal escape text
- Do not mix unrelated work:
  - avoid bundling UI redesign + backend + docs automation in one PR unless requested
  - keep one objective and one rollback path
- Do not stack notification systems without intent:
  - avoid rendering multiple global toast layers (`react-hot-toast` + PrimeReact `Toast`) in the same layout path
  - if both are required, document ownership/scope and test z-index/position conflicts
- Protect global client state integrity:
  - buttons and navigation actions must not reset persisted global state (`auth`, `settings`, `community`) unless explicitly a logout/reset action
  - when reloading memberships/preferences, preserve existing selected context if still valid; fallback only when invalid
  - add Playwright coverage for state persistence across route/button flows when touching shared layout or stores

## Hard Validation Before Push

Run these before push:

- `npm run agent:preflight`

If backend Java files changed, also run:

- `mvn -q test` in `apps/api-java`

If frontend files changed, also run:

- `npm run agent:ux:pw -- open http://localhost:5173 --headed`
- `npm run agent:ux:pw -- snapshot`
- `npm run agent:ux:pw -- screenshot output/playwright/<name>.png`

## Execution Governance Rules (Expanded)

- Every issue must define measurable civic outcome.
- Every scoring change must include formula diff and reproducibility evidence.
- Every dashboard feature must expose data freshness timestamp.
- Every moderation rule must include false-positive review strategy.
- Every release must publish "what changed for communities" summary.

## Docker Runtime Baseline

- Local and deployment runtime must be defined in `infra/docker-compose.yml`.
- Development overrides must be defined in `infra/docker-compose.dev.yml`.
- Frontend API target must be configurable via `VITE_API_BASE_URL`.
- Health checks are required before considering deployment successful.

## Agent PR Hygiene Workflow

- Workflow: `.github/workflows/agent-pr-hygiene.yml`
- Script: `scripts/agent-pr-hygiene.mjs`
- PRs must include Story ID, verification evidence, and rollback plan.

## Frontend UX Evidence Gate

- Workflow: `.github/workflows/frontend-ux-evidence-gate.yml`
- Script: `scripts/check-frontend-ux-evidence.mjs`
- Rule: if PR touches `apps/web-react/**`, Playwright evidence is mandatory in PR body.


## Agent Input Pack

Use these inputs before implementation:

- `docs/agent/prompt-library.md`
- `docs/agent/quality-rubric.md`
- `docs/agent/test-scenario-pack.md`
- `docs/agent/failure-playbook.md`
- `docs/agent/change-risk-matrix.md`
- `docs/agent/agent-handshake-protocol.md`
- `docs/agent/backlog-expansion-seeds.md`

## Agent Evaluation Gate

- Workflow: `.github/workflows/agent-evaluation.yml`
- Script: `scripts/agent-evaluate-pr.mjs`
- Minimum score default: `8/12`
- PRs below threshold must be revised before merge.

## Agent Quality Review Workflow

- Workflow: `.github/workflows/agent-quality-review.yml`
- Trigger: weekly + manual
- Source: `docs/metrics-history.md`
- Output: one issue per week with trends and action checklist.

## Technical and Functional Input Pack

Required references before implementation:

- `docs/agent/functional-requirements.md`
- `docs/agent/technical-requirements.md`
- `docs/agent/integration-test-matrix.md`
- `docs/agent/security-abuse-checklist.md`
## Agent Context Check

- Workflow: `.github/workflows/agent-context-check.yml`
- Script: `scripts/agent-context-check.mjs`
- Purpose: fail PRs missing mandatory technical/functional agent context.

## Domain Decision Governance

- Pack: docs/agent/domain-decision-pack.md
- ADR index: docs/architecture/decision-index.md
- Gate: .github/workflows/contract-adr-gate.yml
- Script: scripts/check-contract-adr.mjs
- Rule: contract changes require ADR updates in the same PR.
