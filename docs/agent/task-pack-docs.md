# Task Pack: Docs and Operations

## Inputs

- Story/issue reference
- Target audience (operator, contributor, maintainer)
- Change intent (guide, policy, runbook, backlog)

## Implement

1. Update only relevant docs and keep scope tight.
2. Keep examples concrete and actionable.
3. Sync docs index when adding/removing docs.

## Verify

1. `npm run backlog:current:check` (if backlog touched)
2. `npm run agent:context:check`
3. Confirm links/paths are valid

## Output

- Updated docs
- Rationale note
- Follow-up items (if any)
