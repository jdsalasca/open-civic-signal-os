# Prompt Library

Purpose: high-signal prompts that force agents to implement production-grade changes with clear acceptance criteria.

## 1) Implement Story Prompt

Use when starting a story:

```text
Implement Story ID: <story-id> in Open Civic Signal OS.
Constraints:
- Keep scope to one objective.
- Preserve backward compatibility.
- Add tests and docs in same PR.
- Include rollback plan.
Validation:
- Run npm run prioritize
- Run npm run build:web
- Run backend build when backend changes.
Output:
- Files changed
- Acceptance criteria checklist
- Risks and mitigations
```

## 2) Refactor Safety Prompt

```text
Refactor only internal structure for <module>.
Do not change API behavior.
Add regression tests proving behavior parity.
Show before/after risk analysis.
```

## 3) Bugfix Prompt

```text
Fix bug: <summary>.
First reproduce with failing test.
Then implement minimal fix.
Add regression test.
Document root cause and rollback option.
```

## 4) Contract Change Prompt

```text
Update contract for endpoint <endpoint>.
Keep old clients safe (or document migration).
Update OpenAPI + examples + frontend usage.
Add contract-level tests.
```

## 5) Release Readiness Prompt

```text
Prepare release for Open Civic Signal OS.
Validate changelog, quality gates, and known limitations.
Summarize user-facing impact in plain language.
```
